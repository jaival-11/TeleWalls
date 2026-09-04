package me.jaival.telewalls.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import me.jaival.telewalls.core.telegram.TelegramAuthState
import me.jaival.telewalls.ui.components.glassmorphism
import me.jaival.telewalls.viewmodel.AuthViewModel

@Composable
fun OnboardingScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val activeChannelId by viewModel.activeChannelId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    var currentStep by remember { mutableIntStateOf(1) }

    var apiIdText by remember { mutableStateOf("") }
    var apiHashText by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var newChannelTitle by remember { mutableStateOf("TeleWalls Vault") }
    var stepErrorState by remember { mutableStateOf<String?>(null) }

    // Auto load channels when authenticated
    LaunchedEffect(authState) {
        if (authState is TelegramAuthState.Ready) {
            viewModel.loadStorageChannels()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // App & Setup Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Wallpaper, contentDescription = null, tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "TeleWalls",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Compulsory Setup Process",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Text(
                    text = "Step $currentStep of 3",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Unified Error Banner (UI & TDLib Error Handling)
            val displayError = errorMessage ?: stepErrorState ?: (authState as? TelegramAuthState.Failed)?.message
            if (displayError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Error",
                            tint = errorColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = displayError,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                stepErrorState = null
                                viewModel.clearErrorMessage()
                                viewModel.resetAuthError()
                            }
                        ) {
                            Text("Dismiss", color = errorColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> {
                        // Step 1: Telegram API Credentials
                        Column {
                            Text(
                                text = "1. Telegram API Details",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TeleWalls connects directly to your Telegram account via TDLib. Please enter your API ID and API Hash from my.telegram.org.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://my.telegram.org"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = null, tint = primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Get Keys at my.telegram.org", color = primaryColor)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = apiIdText,
                                onValueChange = {
                                    apiIdText = it
                                    stepErrorState = null
                                },
                                label = { Text("API ID (numeric)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = apiHashText,
                                onValueChange = {
                                    apiHashText = it
                                    stepErrorState = null
                                },
                                label = { Text("API Hash") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val apiId = apiIdText.toIntOrNull() ?: 0
                                    if (apiId <= 0 || apiHashText.isBlank()) {
                                        stepErrorState = "Please enter a valid numeric API ID and non-empty API Hash."
                                        return@Button
                                    }
                                    stepErrorState = null
                                    viewModel.submitCredentials(apiId, apiHashText) { success, error ->
                                        if (success) {
                                            currentStep = 2
                                        } else {
                                            stepErrorState = error ?: "Failed to save API credentials."
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text("Next: Phone Authentication", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 2: Phone Number & Telegram Authentication
                        Column {
                            Text(
                                text = "2. Telegram Sign-In",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter your phone number to receive a verification code and authorize TeleWalls.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            when (val state = authState) {
                                is TelegramAuthState.WaitingForPhoneNumber, is TelegramAuthState.Initializing, is TelegramAuthState.Uninitialized -> {
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = {
                                            phoneNumber = it
                                            stepErrorState = null
                                        },
                                        label = { Text("Phone Number (+1234567890)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = fieldColors
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (phoneNumber.isBlank() || !phoneNumber.startsWith("+")) {
                                                stepErrorState = "Please include country code starting with '+' (e.g. +1234567890)."
                                                return@Button
                                            }
                                            stepErrorState = null
                                            viewModel.submitPhoneNumber(phoneNumber)
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Send Verification Code", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                is TelegramAuthState.WaitingForCode -> {
                                    Text(
                                        text = "Verification code sent to ${state.phoneNumber}",
                                        color = primaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = smsCode,
                                        onValueChange = {
                                            smsCode = it
                                            stepErrorState = null
                                        },
                                        label = { Text("Telegram Verification Code") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = fieldColors
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (smsCode.isBlank()) {
                                                stepErrorState = "Please enter the verification code received."
                                                return@Button
                                            }
                                            stepErrorState = null
                                            viewModel.submitCode(smsCode)
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Text("Verify Code", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.resetAuthError() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Change Phone Number", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                is TelegramAuthState.WaitingForPassword -> {
                                    Text(
                                        text = "Two-Step Verification is enabled on your account.",
                                        color = primaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            stepErrorState = null
                                        },
                                        label = { Text("2FA Password") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = fieldColors
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            if (password.isBlank()) {
                                                stepErrorState = "Please enter your 2FA password."
                                                return@Button
                                            }
                                            stepErrorState = null
                                            viewModel.submitPassword(password)
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(26.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Text("Verify 2FA Password", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                is TelegramAuthState.Ready -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(primaryColor.copy(alpha = 0.15f))
                                            .padding(20.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("Telegram Account Authenticated!", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("TDLib session authorized and connected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.loadStorageChannels()
                                            currentStep = 3
                                        },
                                        modifier = Modifier.fillMaxWidth().height(54.dp),
                                        shape = RoundedCornerShape(27.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        Text("Next: Storage Channel Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = null)
                                    }
                                }

                                is TelegramAuthState.Failed -> {
                                    Button(
                                        onClick = { viewModel.resetAuthError() },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(25.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry Authentication Step", fontWeight = FontWeight.Bold)
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Vault Storage Channel Selection / Creation
                        Column {
                            Text(
                                text = "3. Telegram Vault Channel",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select an existing Telegram channel or create a new private channel to store your wallpapers.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // List of existing channels if available
                            if (channels.isNotEmpty()) {
                                Text(
                                    text = "Your Storage Channels:",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                channels.forEach { channel ->
                                    val isSelected = activeChannelId == channel.chatId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable {
                                                viewModel.selectStorageChannel(channel.chatId)
                                                stepErrorState = null
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = channel.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            Text(text = "Chat ID: ${channel.chatId}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Selected", tint = primaryColor, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Create New Channel section
                            Text(
                                text = "Or Create New Channel Vault:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = newChannelTitle,
                                onValueChange = {
                                    newChannelTitle = it
                                    stepErrorState = null
                                },
                                label = { Text("New Channel Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    if (newChannelTitle.isBlank()) {
                                        stepErrorState = "Please specify a name for the new Telegram channel."
                                        return@OutlinedButton
                                    }
                                    stepErrorState = null
                                    viewModel.createStorageChannel(newChannelTitle) { success, error ->
                                        if (!success) {
                                            stepErrorState = error ?: "Failed to create channel."
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create & Select Channel", color = primaryColor, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Finish Compulsory Setup Button
                            Button(
                                onClick = {
                                    stepErrorState = null
                                    viewModel.completeSetup(activeChannelId) { success, error ->
                                        if (success) {
                                            onComplete()
                                        } else {
                                            stepErrorState = error ?: "Please select or create a storage channel to continue."
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
                                } else {
                                    Icon(imageVector = Icons.Filled.CloudDone, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Complete Setup & Enter TeleWalls", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
