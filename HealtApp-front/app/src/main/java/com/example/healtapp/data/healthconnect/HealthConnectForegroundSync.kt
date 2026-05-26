package com.example.healtapp.data.healthconnect

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.dto.dataimport.ImportBatchResponseDto
import com.example.healtapp.domain.repository.ActivityRepository
import com.example.healtapp.domain.repository.ImportRepository
import com.example.healtapp.features.activity.presentation.ActivityStepsHelper
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Тихий импорт из Health Connect, пока процесс приложения в состоянии «на экране»:
 * — короткая задержка после входа в foreground, затем импорт;
 * — затем повтор примерно каждые [PERIODIC_INTERVAL_MS], пока пользователь в приложении.
 *
 * [importOnUserRequest] — то же чтение + импорт по кнопке (например экран «Сон»).
 */
@Singleton
class HealthConnectForegroundSync @Inject constructor(
    private val healthConnectReader: HealthConnectReader,
    private val importRepository: ImportRepository,
    private val activityRepository: ActivityRepository,
) {

    private val started = AtomicBoolean(false)
    private val importMutex = Mutex()

    fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return

        val owner = ProcessLifecycleOwner.get()
        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(INITIAL_DELAY_MS)
                runQuietImport("foreground")
                while (isActive) {
                    delay(PERIODIC_INTERVAL_MS)
                    runQuietImport("periodic")
                }
            }
        }
    }

    suspend fun importOnUserRequest(): Result<ImportBatchResponseDto> {
        return importMutex.withLock {
            executeImport().also { result ->
                result.onSuccess { res ->
                    val n = res.sleeps_created + res.activities_created + res.health_samples_created +
                        res.meals_created
                    if (n > 0) {
                        Log.i(TAG, "User import: +$n (sleep=${res.sleeps_created})")
                        AppRefreshBus.notifyDataChanged()
                    }
                }
            }
        }
    }

    private suspend fun runQuietImport(reason: String) {
        importMutex.withLock {
            executeImport()
                .onSuccess { res ->
                    val n = res.sleeps_created + res.activities_created + res.health_samples_created +
                        res.meals_created
                    if (n > 0) {
                        Log.i(
                            TAG,
                            "Auto-import ($reason): +$n (sleep=${res.sleeps_created}, act=${res.activities_created}, samples=${res.health_samples_created})",
                        )
                        AppRefreshBus.notifyDataChanged()
                    }
                }
                .onFailure { e ->
                    Log.w(TAG, "importBatch failed ($reason)", e)
                }
        }
    }

    private suspend fun executeImport(): Result<ImportBatchResponseDto> {
        if (!healthConnectReader.isHealthConnectUsable()) {
            return Result.failure(IllegalStateException("Health Connect недоступен на этом устройстве"))
        }
        if (!healthConnectReader.canRequestPermissions()) {
            return Result.failure(IllegalStateException("Нельзя запросить доступ к Health Connect"))
        }
        val permitted = runCatching { healthConnectReader.areReadPermissionsGranted() }.getOrDefault(false)
        if (!permitted) {
            return Result.failure(
                IllegalStateException("Нет разрешений на чтение Health Connect. Откройте «Интеграции» и выдайте доступ."),
            )
        }
        removeDuplicateWalkRecordsBeforeImport()

        val batch = healthConnectReader.buildImportPayload(DEFAULT_IMPORT_DAYS).getOrElse {
            return Result.failure(it)
        }
        val result = importRepository.importBatch(batch)
        result.onSuccess { removeDuplicateWalkRecordsBeforeImport() }
        return result
    }

    /** Импорт каждые 10 мин создавал новую «ходьбу» на тот же день — удаляем дубли. */
    private suspend fun removeDuplicateWalkRecordsBeforeImport() {
        val history = activityRepository.getActivityHistory().getOrNull() ?: return
        val toRemove = ActivityStepsHelper.walkDuplicateIdsToRemove(history)
        toRemove.forEach { id ->
            activityRepository.deleteActivity(id)
        }
    }

    private companion object {
        private const val TAG = "HealthConnectFgSync"
        private const val DEFAULT_IMPORT_DAYS = 14
        /** Не конкурировать с первой загрузкой дашборда после входа. */
        private const val INITIAL_DELAY_MS = 12_000L
        private const val PERIODIC_INTERVAL_MS = 5L * 60L * 1000L
    }
}
