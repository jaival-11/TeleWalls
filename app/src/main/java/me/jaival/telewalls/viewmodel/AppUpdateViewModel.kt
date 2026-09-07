package me.jaival.telewalls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.jaival.telewalls.BuildConfig
import me.jaival.telewalls.core.updater.AppReleaseInfo
import me.jaival.telewalls.core.updater.AppUpdateRepository
import me.jaival.telewalls.core.updater.SemVer
import me.jaival.telewalls.core.updater.UpdateState
import me.jaival.telewalls.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val currentVersionName: String
        get() = try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "v1.0.0"
        }

    init {
        checkForUpdatesOnAppOpen()
    }

    /**
     * Trigger background check on app open.
     * Runs at most once per hour.
     */
    fun checkForUpdatesOnAppOpen() {
        viewModelScope.launch {
            if (_updateState.value is UpdateState.Downloading || _updateState.value is UpdateState.DownloadCompleted) {
                return@launch
            }

            val lastCheckTime = settingsRepository.getLastUpdateCheckTime()
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastCheckTime < ONE_HOUR_MS) {
                // Skip automatic check if less than 1 hour has elapsed since last check
                return@launch
            }

            _updateState.value = UpdateState.Checking
            val releaseInfo = repository.fetchLatestRelease()
            settingsRepository.setLastUpdateCheckTime(currentTime)

            if (releaseInfo != null && SemVer.isUpdateAvailable(currentVersionName, releaseInfo.tagName)) {
                _updateState.value = UpdateState.UpdateAvailable(
                    releaseInfo = releaseInfo,
                    currentVersionName = currentVersionName
                )
            } else {
                _updateState.value = UpdateState.Idle
            }
        }
    }

    /**
     * Manual check for updates (e.g. from Settings).
     * Bypasses the 1-hour rate limit and updates the last check timestamp.
     */
    fun checkManual(onResult: (String) -> Unit) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val currentTime = System.currentTimeMillis()
            val releaseInfo = repository.fetchLatestRelease()
            settingsRepository.setLastUpdateCheckTime(currentTime)

            if (releaseInfo == null) {
                _updateState.value = UpdateState.Idle
                onResult("Unable to check for updates. Check internet connection.")
            } else if (SemVer.isUpdateAvailable(currentVersionName, releaseInfo.tagName)) {
                _updateState.value = UpdateState.UpdateAvailable(
                    releaseInfo = releaseInfo,
                    currentVersionName = currentVersionName
                )
                onResult("An update to ${SemVer.formatVersionName(releaseInfo.tagName)} is available!")
            } else {
                _updateState.value = UpdateState.Idle
                onResult("You're on the latest version (${SemVer.formatVersionName(currentVersionName)}).")
            }
        }
    }

    fun dismissUpdate() {
        if (_updateState.value !is UpdateState.Downloading) {
            _updateState.value = UpdateState.Idle
        }
    }

    fun startDownload(releaseInfo: AppReleaseInfo) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(
                releaseInfo = releaseInfo,
                currentVersionName = currentVersionName,
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = -1L
            )

            try {
                val apkFile = repository.downloadApk(releaseInfo.downloadUrl) { progress, bytesDownloaded, totalBytes ->
                    _updateState.value = UpdateState.Downloading(
                        releaseInfo = releaseInfo,
                        currentVersionName = currentVersionName,
                        progress = progress,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes
                    )
                }

                val apkUri = repository.getApkUri(apkFile)
                _updateState.value = UpdateState.DownloadCompleted(
                    releaseInfo = releaseInfo,
                    apkFile = apkFile,
                    apkUri = apkUri
                )

                // Prompt user to install app on download successful
                repository.promptInstallApk(apkFile)

            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = UpdateState.Error(
                    message = e.localizedMessage ?: "Failed to download update APK"
                )
            }
        }
    }

    fun promptInstallApk() {
        val currentState = _updateState.value
        if (currentState is UpdateState.DownloadCompleted) {
            repository.promptInstallApk(currentState.apkFile)
        }
    }
}
