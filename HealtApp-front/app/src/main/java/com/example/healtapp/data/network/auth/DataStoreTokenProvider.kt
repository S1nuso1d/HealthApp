package com.example.healtapp.data.network.auth

import com.example.healtapp.data.preferences.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreTokenProvider @Inject constructor(
    private val tokenStorage: TokenStorage,
    @ApplicationScope appScope: CoroutineScope,
) : TokenProvider {

    @Volatile
    private var token: String? = null

    init {
        appScope.launch {
            tokenStorage.tokenFlow().collectLatest { value ->
                token = value
            }
        }
    }

    /**
     * После login/register in-memory ещё может быть пустым, пока не пришёл первый emit из DataStore.
     * Читаем с диска, чтобы первый же запрос к API не ушёл без заголовка или со старым кэшем.
     */
    override fun getToken(): String? {
        val cached = token
        if (!cached.isNullOrBlank()) return cached
        return runBlocking(Dispatchers.IO) { tokenStorage.getToken() }
    }
}

