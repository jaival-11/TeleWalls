package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.jaival.telewalls.core.telegram.StorageChannel
import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.core.telegram.TelegramClient
import me.jaival.telewalls.core.telegram.TelegramCredentials
import me.jaival.telewalls.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val telegramClient: TelegramClient,
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<TelegramAuthState> = telegramClient.authState

    private val _channels = MutableStateFlow<List<StorageChannel>>(emptyList())
    val channels: StateFlow<List<StorageChannel>> = _channels.asStateFlow()

    private val _activeChannelId = MutableStateFlow<Long?>(null)
    val activeChannelId: StateFlow<Long?> = _activeChannelId.asStateFlow()

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

    fun submitCredentials(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            authRepository.saveCredentials(apiId, apiHash)
            telegramClient.start(TelegramCredentials(apiId, apiHash))
        }
    }

    fun submitPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            telegramClient.submitPhoneNumber(phoneNumber)
        }
    }

    fun submitCode(code: String) {
        viewModelScope.launch {
            telegramClient.submitCode(code)
        }
    }

    fun submitPassword(password: String) {
        viewModelScope.launch {
            telegramClient.submitPassword(password)
        }
    }

    fun requestQrCode() {
        viewModelScope.launch {
            telegramClient.requestQrCodeAuthentication()
        }
    }

    fun loadStorageChannels() {
        viewModelScope.launch {
            _channels.value = telegramClient.listStorageChannels()
        }
    }

    fun selectStorageChannel(channelId: Long) {
        viewModelScope.launch {
            authRepository.saveActiveChannelId(channelId)
            _activeChannelId.value = channelId
        }
    }

    fun createStorageChannel(title: String) {
        viewModelScope.launch {
            val channel = telegramClient.createStorageChannel(title)
            authRepository.saveActiveChannelId(channel.chatId)
            _activeChannelId.value = channel.chatId
            loadStorageChannels()
        }
    }

    fun logout() {
        viewModelScope.launch {
            telegramClient.logout()
            authRepository.clearSession()
        }
    }
}
