package me.jaival.telewalls.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jaival.telewalls.core.updater.SemVer
import me.jaival.telewalls.core.updater.UpdateState
import me.jaival.telewalls.ui.components.M3eWavyProgressBar
import kotlin.math.roundToInt

@Composable
fun UpdateAvailableDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onInstallClick: () -> Unit,
    onInstallApkPrompt: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    if (updateState is UpdateState.Idle || updateState is UpdateState.Checking) {
        return
    }

    val currentVer = when (updateState) {
        is UpdateState.UpdateAvailable -> SemVer.formatVersionName(updateState.currentVersionName)
        is UpdateState.Downloading -> SemVer.formatVersionName(updateState.currentVersionName)
        is UpdateState.DownloadCompleted -> "v" + SemVer.parseVersion(updateState.releaseInfo.tagName).joinToString(".")
        else -> "v1.0.0"
    }

    val latestVer = when (updateState) {
        is UpdateState.UpdateAvailable -> SemVer.formatVersionName(updateState.releaseInfo.versionName)
        is UpdateState.Downloading -> SemVer.formatVersionName(updateState.releaseInfo.versionName)
        is UpdateState.DownloadCompleted -> SemVer.formatVersionName(updateState.releaseInfo.versionName)
        else -> "v1.1.0"
    }

    val htmlUrl = when (updateState) {
        is UpdateState.UpdateAvailable -> updateState.releaseInfo.htmlUrl
        is UpdateState.Downloading -> updateState.releaseInfo.htmlUrl
        is UpdateState.DownloadCompleted -> updateState.releaseInfo.htmlUrl
        else -> "https://github.com/jaival-11/TeleWalls/releases"
    }

    val releaseNotes = when (updateState) {
        is UpdateState.UpdateAvailable -> updateState.releaseInfo.releaseNotes
        is UpdateState.Downloading -> updateState.releaseInfo.releaseNotes
        is UpdateState.DownloadCompleted -> updateState.releaseInfo.releaseNotes
        else -> ""
    }

    AlertDialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Update available",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                // Exact requested body text format: "Current app version is v1.0.0, an update to v1.1.0 is available."
                val bodyText = "Current app version is $currentVer, an update to $latestVer is available."
                
                Text(
                    text = bodyText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // If changelog notes exist and we're not downloading, show a clean release notes card
                if (releaseNotes.isNotBlank() && updateState is UpdateState.UpdateAvailable) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "What's New:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = releaseNotes,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Show M3 Expressive Wavy Progress Bar during downloading state
                if (updateState is UpdateState.Downloading) {
                    val progressPercent = (updateState.progress * 100).roundToInt()
                    val downloadedMb = String.format("%.1f", updateState.bytesDownloaded / (1024f * 1024f))
                    val totalMb = if (updateState.totalBytes > 0) String.format("%.1f", updateState.totalBytes / (1024f * 1024f)) else "??"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading update...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$progressPercent% ($downloadedMb / $totalMb MB)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = primaryColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // M3 Expressive Wavy Progress Bar
                        M3eWavyProgressBar(
                            progress = updateState.progress,
                            color = primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                if (updateState is UpdateState.DownloadCompleted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Download complete! Ready to install $latestVer.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        M3eWavyProgressBar(
                            progress = 1.0f,
                            color = primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                if (updateState is UpdateState.Error) {
                    Text(
                        text = "Error: ${updateState.message}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dismiss button
                TextButton(
                    onClick = onDismiss,
                    enabled = updateState !is UpdateState.Downloading
                ) {
                    Text(
                        text = "Dismiss",
                        color = if (updateState is UpdateState.Downloading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Changelog button (Opens release in browser)
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Changelog",
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Install button
                when (updateState) {
                    is UpdateState.DownloadCompleted -> {
                        Button(
                            onClick = onInstallApkPrompt,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Install Now", fontWeight = FontWeight.Bold)
                        }
                    }
                    is UpdateState.Downloading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Downloading...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onInstallClick,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Install", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    )
}
