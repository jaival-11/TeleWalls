package me.jaival.telewalls.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.ui.components.Glassmorphism
import me.jaival.telewalls.ui.theme.NeonCyan
import me.jaival.telewalls.ui.theme.VibrantMagenta
import me.jaival.telewalls.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel
) {
    val authState by viewModel.authState.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val activeChannelId by viewModel.activeChannelId.collectAsState()

    var apiIdText by remember { mutableStateOf("") }
    var apiHashText by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var newChannelTitle by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is TelegramAuthState.Ready) {
            viewModel.loadStorageChannels()
        }
    }

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
                text = "Telegram Vault Setup",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            )
            Text(
                text = "Connect TDLib & configure private storage channel",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // State Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .glassmorphism(
                        backgroundColor = Color(0xFF141720),
                        borderColor = Color(0x3300F0FF),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (authState is TelegramAuthState.Ready) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (authState is TelegramAuthState.Ready) NeonCyan else VibrantMagenta,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Status: ${authState::class.simpleName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (authState is TelegramAuthState.Ready) "TDLib Client Active & Authorized" else "Action required for authentication",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 1: Credentials Setup
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Key, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "1. Telegram Credentials", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiIdText,
                        onValueChange = { apiIdText = it },
                        label = { Text("API ID (from my.telegram.org)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F1117),
                            unfocusedContainerColor = Color(0xFF0F1117),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiHashText,
                        onValueChange = { apiHashText = it },
                        label = { Text("API Hash (from my.telegram.org)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F1117),
                            unfocusedContainerColor = Color(0xFF0F1117),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val apiId = apiIdText.toIntOrNull() ?: 0
                            viewModel.submitCredentials(apiId, apiHashText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF07080B))
                    ) {
                        Text("Connect TDLib Engine", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Auth Flow depending on auth state
            when (authState) {
                is TelegramAuthState.WaitingForPhoneNumber -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Phone, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "2. Phone Sign-In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number (+123456789)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F1117),
                                    unfocusedContainerColor = Color(0xFF0F1117),
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitPhoneNumber(phoneNumber) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF07080B))
                            ) {
                                Text("Send Verification Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is TelegramAuthState.WaitingForCode -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = "Enter Telegram Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = smsCode,
                                onValueChange = { smsCode = it },
                                label = { Text("Verification Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F1117),
                                    unfocusedContainerColor = Color(0xFF0F1117),
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitCode(smsCode) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF07080B))
                            ) {
                                Text("Submit Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is TelegramAuthState.WaitingForPassword -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = "Two-Step Verification Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("2FA Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F1117),
                                    unfocusedContainerColor = Color(0xFF0F1117),
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitPassword(password) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF07080B))
                            ) {
                                Text("Submit Password", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is TelegramAuthState.Ready -> {
                    // Storage Channel Manager
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = "Storage Vault Channels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Select or create channel to store wallpapers", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            if (channels.isNotEmpty()) {
                                channels.forEach { ch ->
                                    val isSelected = activeChannelId == ch.chatId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0F1117))
                                            .clickable { viewModel.selectStorageChannel(ch.chatId) }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = ch.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = NeonCyan)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newChannelTitle,
                                onValueChange = { newChannelTitle = it },
                                label = { Text("New Channel Title (e.g. TeleWalls Vault)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F1117),
                                    unfocusedContainerColor = Color(0xFF0F1117),
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (newChannelTitle.isNotBlank()) {
                                        viewModel.createStorageChannel(newChannelTitle)
                                        newChannelTitle = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF07080B))
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Storage Channel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
