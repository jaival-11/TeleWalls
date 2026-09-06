package me.jaival.telewalls

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
import me.jaival.telewalls.data.repository.SettingsRepository
import me.jaival.telewalls.ui.navigation.TeleWallsNavGraph
import me.jaival.telewalls.ui.theme.TeleWallsTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val reduceAnimations by settingsRepository.reduceAnimationsFlow.collectAsState(initial = false)
            TeleWallsTheme(reduceAnimations = reduceAnimations) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeleWallsNavGraph()
                }
            }
        }
    }
}
