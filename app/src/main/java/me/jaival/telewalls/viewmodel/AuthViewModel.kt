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
import me.jaival.telewalls.data.repository.BackupRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val telegramClient: TelegramClient,
    private val authRepository: AuthRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val authState: StateFlow<TelegramAuthState> = telegramClient.authState

    val isSetupCompleted: StateFlow<Boolean> = authRepository.isSetupCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _channels = MutableStateFlow<List<StorageChannel>>(emptyList())
    val channels: StateFlow<List<StorageChannel>> = _channels.asStateFlow()

    private val _activeChannelId = MutableStateFlow<Long?>(null)
    val activeChannelId: StateFlow<Long?> = _activeChannelId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                _channels.value = telegramClient.listStorageChannels()
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

    fun createStorageChannel(title: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (title.isBlank()) {
            val err = "Channel title cannot be empty"
            _errorMessage.value = err
            onResult(false, err)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val channel = telegramClient.createStorageChannel(title)
                authRepository.saveActiveChannelId(channel.chatId)
                _activeChannelId.value = channel.chatId
                loadStorageChannels()
                onResult(true, null)
            } catch (e: Exception) {
                val err = e.message ?: "Failed to create storage channel"
                _errorMessage.value = err
                onResult(false, err)
            } finally {
                _isLoading.value = false
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
        }
    }

    fun exportBackup(uri: android.net.Uri, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = backupRepository.exportBackupToUri(uri)
            _isLoading.value = false
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to export backup."
                _errorMessage.value = err
                onResult(false, err)
            }
        }
    }

    fun restoreBackup(uri: android.net.Uri, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = backupRepository.restoreBackupFromUri(uri)
            _isLoading.value = false
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to restore backup file."
                _errorMessage.value = err
                onResult(false, err)
            }
        }
    }
}
