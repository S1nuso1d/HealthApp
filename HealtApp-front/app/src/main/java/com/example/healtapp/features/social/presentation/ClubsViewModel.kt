package com.example.healtapp.features.social.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healtapp.core.common.AppRefreshBus
import com.example.healtapp.data.network.ApiServerConfig
import com.example.healtapp.data.network.dto.social.ClubCreateDto
import com.example.healtapp.data.network.dto.social.ClubMemberResponseDto
import com.example.healtapp.data.network.dto.social.ClubMemberRoleUpdateDto
import com.example.healtapp.data.network.dto.social.ClubPostCreateDto
import com.example.healtapp.data.network.dto.social.ClubPollVoteDto
import com.example.healtapp.data.network.dto.social.ClubResponseDto
import com.example.healtapp.data.network.dto.social.ClubPostResponseDto
import com.example.healtapp.data.network.dto.social.ClubUpdateDto
import com.example.healtapp.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class ClubsUiState(
    val isLoading: Boolean = false,
    val clubs: List<ClubResponseDto> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

data class ClubDetailUiState(
    val isLoading: Boolean = false,
    val club: ClubResponseDto? = null,
    val members: List<ClubMemberResponseDto> = emptyList(),
    val posts: List<ClubPostResponseDto> = emptyList(),
    val selectedTab: Int = 0,
    val myRole: String? = null,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ClubsViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val apiServerConfig: ApiServerConfig,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _clubsState = MutableStateFlow(ClubsUiState())
    val clubsState: StateFlow<ClubsUiState> = _clubsState.asStateFlow()

    private val _detailState = MutableStateFlow(ClubDetailUiState())
    val detailState: StateFlow<ClubDetailUiState> = _detailState.asStateFlow()

    init {
        refreshClubs()
        viewModelScope.launch {
            AppRefreshBus.events.collect { refreshClubs() }
        }
    }

    fun refreshClubs() {
        viewModelScope.launch {
            _clubsState.update { it.copy(isLoading = true, error = null) }
            repository.getClubs()
                .onSuccess { clubs -> _clubsState.update { it.copy(isLoading = false, clubs = clubs) } }
                .onFailure { e -> _clubsState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun createClub(name: String, description: String?, rules: String?, avatarUri: Uri? = null) {
        val trimmed = name.trim()
        if (trimmed.length < 3) {
            _clubsState.update { it.copy(error = "Название клуба — минимум 3 символа") }
            return
        }
        viewModelScope.launch {
            _clubsState.update { it.copy(isLoading = true, error = null) }

            val avatarUrl = avatarUri?.let { uri ->
                uploadClubImage(uri).getOrElse { error ->
                    _clubsState.update { it.copy(isLoading = false, error = error.message) }
                    return@launch
                }
            }

            repository.createClub(
                ClubCreateDto(
                    name = trimmed,
                    description = description?.trim()?.takeIf { it.isNotBlank() },
                    rules = rules?.trim()?.takeIf { it.isNotBlank() },
                    avatar_url = avatarUrl,
                ),
            ).onSuccess {
                _clubsState.update { state -> state.copy(isLoading = false, message = "Клуб «$trimmed» создан") }
                refreshClubs()
            }.onFailure { e ->
                _clubsState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun uploadClubImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Не удалось прочитать изображение")
            val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = bytes.toRequestBody(mime.toMediaType())
            val part = MultipartBody.Part.createFormData("file", "club_avatar.jpg", requestBody)
            val response = repository.uploadPostMedia(part).getOrThrow()
            apiServerConfig.baseUrl().removeSuffix("/") + response.url
        }
    }

    fun joinClub(clubId: Int, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.joinClub(clubId)
                .onSuccess {
                    refreshClubs()
                    loadClubDetail(clubId)
                    AppRefreshBus.notifyDataChanged()
                    onComplete?.invoke()
                }
                .onFailure { e -> _clubsState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveClub(clubId: Int, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.leaveClub(clubId)
                .onSuccess {
                    refreshClubs()
                    loadClubDetail(clubId)
                    AppRefreshBus.notifyDataChanged()
                    onComplete?.invoke()
                }
                .onFailure { e ->
                    _detailState.update { it.copy(error = e.message) }
                    _clubsState.update { it.copy(error = e.message) }
                }
        }
    }

    fun loadClubDetail(clubId: Int) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            val club = repository.getClub(clubId).getOrNull()
            val members = repository.getClubMembers(clubId).getOrElse { emptyList() }
            val posts = repository.getClubPosts(clubId).getOrElse { emptyList() }
            _detailState.update {
                it.copy(
                    isLoading = false,
                    club = club,
                    members = members,
                    posts = posts,
                    myRole = members.find { m -> m.user.is_self }?.role,
                )
            }
        }
    }

    fun selectDetailTab(index: Int) {
        _detailState.update { it.copy(selectedTab = index) }
    }

    fun updateClub(
        clubId: Int,
        name: String,
        description: String?,
        rules: String?,
        avatarUri: Uri? = null,
        keepAvatarUrl: String? = null,
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            val avatarUrl = when {
                avatarUri != null -> uploadClubImage(avatarUri).getOrElse { error ->
                    _detailState.update { it.copy(isLoading = false, error = error.message) }
                    return@launch
                }
                else -> keepAvatarUrl?.trim()?.takeIf { it.isNotBlank() }
            }
            repository.updateClub(
                clubId,
                ClubUpdateDto(
                    name = name.trim().takeIf { it.isNotBlank() },
                    description = description?.trim()?.takeIf { it.isNotBlank() },
                    rules = rules?.trim()?.takeIf { it.isNotBlank() },
                    avatar_url = avatarUrl,
                ),
            ).onSuccess {
                loadClubDetail(clubId)
                refreshClubs()
                AppRefreshBus.notifyDataChanged()
                _detailState.update { it.copy(message = "Настройки клуба сохранены") }
            }.onFailure { e -> _detailState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun removeMember(clubId: Int, userId: Int) {
        viewModelScope.launch {
            repository.removeClubMember(clubId, userId)
                .onSuccess {
                    loadClubDetail(clubId)
                    refreshClubs()
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e -> _detailState.update { it.copy(error = e.message) } }
        }
    }

    fun setMemberRole(clubId: Int, userId: Int, role: String) {
        viewModelScope.launch {
            repository.updateClubMemberRole(clubId, userId, ClubMemberRoleUpdateDto(role))
                .onSuccess {
                    loadClubDetail(clubId)
                    AppRefreshBus.notifyDataChanged()
                }
                .onFailure { e -> _detailState.update { it.copy(error = e.message) } }
        }
    }

    fun createPost(clubId: Int, type: String, body: String, pollOptions: List<String>? = null) {
        viewModelScope.launch {
            repository.createClubPost(
                clubId,
                ClubPostCreateDto(post_type = type, body = body.takeIf { it.isNotBlank() }, poll_options = pollOptions),
            ).onSuccess { loadClubDetail(clubId) }
                .onFailure { e -> _detailState.update { it.copy(error = e.message) } }
        }
    }

    fun votePoll(clubId: Int, postId: Int, option: String) {
        viewModelScope.launch {
            repository.voteClubPoll(clubId, postId, ClubPollVoteDto(option))
                .onSuccess { loadClubDetail(clubId) }
                .onFailure { e -> _detailState.update { it.copy(error = e.message) } }
        }
    }
}
