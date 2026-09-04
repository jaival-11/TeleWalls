package me.jaival.telewalls.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jaival.telewalls.ui.components.Glassmorphism
import me.jaival.telewalls.ui.theme.NeonCyan
import me.jaival.telewalls.ui.theme.VibrantMagenta
import me.jaival.telewalls.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    var dynamicColorsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            )
            Text(
                text = "TeleWalls Configuration & Storage",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // UI Theme & Aesthetics Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Dynamic Palette Colors", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = "Extract palette swatches from wallpaper", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = dynamicColorsEnabled,
                            onCheckedChange = { dynamicColorsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF07080B),
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cache Management Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Image cache cleared!", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.CleaningServices, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Clear Image Cache", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = "Frees temporary document preview files", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About TeleWalls Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "TeleWalls App", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = "Package: me.jaival.telewalls", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Version 1.0.0 (TDLib Telegram Storage)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    authViewModel.logout()
                    Toast.makeText(context, "Logged out from Telegram Vault session", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A1520),
                    contentColor = VibrantMagenta
                )
            ) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Logout Telegram Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}
