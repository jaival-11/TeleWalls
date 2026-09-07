package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.jaival.telewalls.core.telegram.StorageChannel
import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.core.telegram.TelegramCredentials
import me.jaival.telewalls.data.repository.AuthRepository
import me.jaival.telewalls.data.repository.SettingsRepository
import me.jaival.telewalls.data.repository.WallpaperRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val telegramClient: TelegramClient,
    private val authRepository: AuthRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val authState: StateFlow<TelegramAuthState> = telegramClient.authState

    val isSetupCompleted: StateFlow<Boolean?> = authRepository.isSetupCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val phoneNumber: StateFlow<String?> = authRepository.phoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userName: StateFlow<String?> = authRepository.userNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profilePhotoPath: StateFlow<String?> = authRepository.profilePhotoPathFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _channels = MutableStateFlow<List<StorageChannel>>(emptyList())
    val channels: StateFlow<List<StorageChannel>> = _channels.asStateFlow()

    private val _activeChannelId = MutableStateFlow<Long?>(null)
    val activeChannelId: StateFlow<Long?> = _activeChannelId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isReindexing = MutableStateFlow<Boolean>(false)
    val isReindexing: StateFlow<Boolean> = _isReindexing.asStateFlow()

    private val _reindexStatus = MutableStateFlow<String?>(null)
    val reindexStatus: StateFlow<String?> = _reindexStatus.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.credentialsFlow.collect { creds ->
                if (creds != null) {
                    telegramClient.start(creds)
                }
            }
        }
        viewModelScope.launch {
            authRepository.activeChannelIdFlow.collect { id ->
                _activeChannelId.value = id
            }
        }
        viewModelScope.launch {
            authState.collect { state ->
                if (state is TelegramAuthState.WaitingForCode) {
                    if (state.phoneNumber.isNotBlank()) {
                        authRepository.savePhoneNumber(state.phoneNumber)
                    }
                } else if (state is TelegramAuthState.Ready) {
                    try {
                        val user = telegramClient.getMe()
                        if (user != null) {
                            if (user.phoneNumber.isNotBlank()) {
                                authRepository.savePhoneNumber(user.phoneNumber)
                            }
                            val name = listOf(user.firstName, user.lastName)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                            if (name.isNotBlank()) {
                                authRepository.saveUserName(name)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore error
                    }
                }
            }
        }
    }

    fun submitCredentials(apiId: Int, apiHash: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (apiId <= 0 || apiHash.isBlank()) {
            val err = "Please provide a valid numeric API ID and API Hash."
            _errorMessage.value = err
            onResult(false, err)
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                authRepository.saveCredentials(apiId, apiHash)
                telegramClient.start(TelegramCredentials(apiId, apiHash))
                onResult(true, null)
            } catch (e: Exception) {
                val err = e.message ?: "Failed to save API credentials"
                _errorMessage.value = err
                onResult(false, err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (phoneNumber.isNotBlank()) {
                authRepository.savePhoneNumber(phoneNumber)
            }
            telegramClient.submitPhoneNumber(phoneNumber)
            _isLoading.value = false
        }
    }

    fun submitCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            telegramClient.submitCode(code)
            _isLoading.value = false
        }
    }

    fun submitPassword(password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            telegramClient.submitPassword(password)
            _isLoading.value = false
        }
    }

    fun resetAuthError() {
        viewModelScope.launch {
            telegramClient.resetAuthState()
            _errorMessage.value = null
        }
    }

    fun loadStorageChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetched = telegramClient.listStorageChannels()
                _channels.value = fetched.filter {
                    it.title.startsWith("TeleWalls") && it.description.contains("#telewalls-storage")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load storage channels"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectStorageChannel(channelId: Long) {
        viewModelScope.launch {
            authRepository.saveActiveChannelId(channelId)
            _activeChannelId.value = channelId
        }
    }

    fun switchChannel(channelId: Long) {
        if (_activeChannelId.value == channelId && !_isReindexing.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _isReindexing.value = true
            _reindexStatus.value = "Switching active channel..."
            _errorMessage.value = null
            try {
                authRepository.saveActiveChannelId(channelId)
                _activeChannelId.value = channelId
                _reindexStatus.value = "Re-indexing channel wallpapers..."
                val result = wallpaperRepository.reindexFromChannel(channelId)
                result.onSuccess { (wallpapersCount, categoriesCount) ->
                    _reindexStatus.value = "Channel indexed ($wallpapersCount wallpapers, $categoriesCount categories)"
                }.onFailure { error ->
                    _errorMessage.value = "Re-indexing failed: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to switch channel"
            } finally {
                _isLoading.value = false
                _isReindexing.value = false
            }
        }
    }

    fun createStorageChannel(title: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (title.isBlank()) {
            val err = "Channel title cannot be empty"
            _errorMessage.value = err
            onResult(false, err)
            return
        }
        val cleanTitle = title.trim()
        val formattedTitle = if (cleanTitle.startsWith("TeleWalls")) {
            cleanTitle
        } else {
            "TeleWalls $cleanTitle"
        }
        viewModelScope.launch {
            _isLoading.value = true
            _isReindexing.value = true
            _reindexStatus.value = "Creating new Telegram channel..."
            _errorMessage.value = null
            try {
                val channel = telegramClient.createStorageChannel(formattedTitle)
                authRepository.saveActiveChannelId(channel.chatId)
                _activeChannelId.value = channel.chatId
                loadStorageChannels()
                _reindexStatus.value = "Re-indexing new channel..."
                val result = wallpaperRepository.reindexFromChannel(channel.chatId)
                result.onSuccess { (wallpapersCount, categoriesCount) ->
                    _reindexStatus.value = "Channel created & indexed ($wallpapersCount wallpapers)"
                }
                onResult(true, null)
            } catch (e: Exception) {
                val err = e.message ?: "Failed to create storage channel"
                _errorMessage.value = err
                onResult(false, err)
            } finally {
                _isLoading.value = false
                _isReindexing.value = false
            }
        }
    }

    fun completeSetup(channelId: Long? = null, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedId = channelId ?: _activeChannelId.value
                if (selectedId == null || selectedId == 0L) {
                    val err = "Please select or create a Telegram storage channel first."
                    _errorMessage.value = err
                    onResult(false, err)
                    return@launch
                }
                authRepository.saveActiveChannelId(selectedId)
                authRepository.setSetupCompleted(true)
                onResult(true, null)
            } catch (e: Exception) {
                val err = e.message ?: "Failed to complete setup"
                _errorMessage.value = err
                onResult(false, err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            telegramClient.logout()
            authRepository.clearSession()
            wallpaperRepository.clearAllData()
            settingsRepository.resetSettings()
            _channels.value = emptyList()
            _activeChannelId.value = null
            _errorMessage.value = null
            _reindexStatus.value = null
            _isLoading.value = false
            _isReindexing.value = false
        }
    }
}
