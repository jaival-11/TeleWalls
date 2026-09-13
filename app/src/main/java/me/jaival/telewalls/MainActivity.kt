package me.jaival.telewalls

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import me.jaival.telewalls.data.repository.SettingsRepository
import me.jaival.telewalls.ui.navigation.TeleWallsNavGraph
import me.jaival.telewalls.ui.theme.TeleWallsTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val sharedUrisState = MutableStateFlow<List<Uri>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            val reduceAnimations by settingsRepository.reduceAnimationsFlow.collectAsState(initial = false)
            val sharedUris by sharedUrisState.collectAsState()

            TeleWallsTheme(reduceAnimations = reduceAnimations) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeleWallsNavGraph(
                        sharedUris = sharedUris,
                        onSharedUrisHandled = {
                            sharedUrisState.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val uris = extractImageUris(intent)
        if (uris.isNotEmpty()) {
            sharedUrisState.value = uris
        }
    }

    private fun extractImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val action = intent.action ?: return emptyList()
        val type = intent.type ?: return emptyList()

        if (!type.startsWith("image/")) return emptyList()

        val uris = mutableListOf<Uri>()

        when (action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    uris.add(uri)
                } else {
                    intent.clipData?.let { clipData ->
                        if (clipData.itemCount > 0) {
                            clipData.getItemAt(0).uri?.let { uris.add(it) }
                        }
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streamUris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!streamUris.isNullOrEmpty()) {
                    uris.addAll(streamUris)
                } else {
                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i).uri?.let { uris.add(it) }
                        }
                    }
                }
            }
        }

        return uris.distinct()
    }
}
