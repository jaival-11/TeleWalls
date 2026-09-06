package me.jaival.telewalls.ui.screens.upload

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.jaival.telewalls.ui.components.CategoryChips
import me.jaival.telewalls.viewmodel.UploadState
import me.jaival.telewalls.viewmodel.UploadViewModel

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onUploadSuccess: () -> Unit,
    isMultiMode: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedUri by viewModel.selectedImageUri.collectAsState()
    val selectedUris by viewModel.selectedImageUris.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val detectedResolution by viewModel.detectedResolution.collectAsState()
    val detectedColors by viewModel.detectedColors.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("AMOLED") }
    var tags by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(context, uri)
        }
    }

    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.selectMultipleImages(context, uris)
        }
    }

    LaunchedEffect(selectedFileName) {
        val name = selectedFileName
        if (!name.isNullOrBlank() && title.isBlank()) {
            title = name
        }
    }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            Toast.makeText(context, "Photo uploaded as high-res document to Telegram Vault!", Toast.LENGTH_LONG).show()
            viewModel.resetState()
            title = ""
            author = ""
            tags = ""
            description = ""
            onUploadSuccess()
        } else if (uploadState is UploadState.Error) {
            Toast.makeText(context, (uploadState as UploadState.Error).message, Toast.LENGTH_SHORT).show()
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = if (isMultiMode) "Multi Upload" else "Single Upload",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp
                        )
                    )
                    Text(
                        text = if (isMultiMode) "Batch upload high-res wallpapers to Telegram Vault" else "Sends as Document to preserve original resolution",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Photo picker Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(2.dp, if (selectedUri != null) primaryColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    .clickable {
                        if (isMultiMode) {
                            multiPickerLauncher.launch("image/*")
                        } else {
                            singlePickerLauncher.launch("image/*")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedUris.size > 1) "${selectedUris.size} Selected" else detectedResolution,
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Pick Photo",
                            tint = primaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isMultiMode) "Tap to select multiple photos" else "Tap to select photo from gallery",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Detected Colors Swatches
            if (detectedColors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Extracted Colors: ",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    detectedColors.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { primaryColor }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Wallpaper Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Author
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Wallpaper Type Dropdown
            var wallpaperTypeExpanded by remember { mutableStateOf(false) }
            val wallpaperTypeOptions = listOf("Auto-detect (Based on aspect ratio)", "Phone", "Desktop/Tablet")
            val selectedWallpaperType by viewModel.selectedWallpaperType.collectAsState()

            val displayWallpaperType = when (selectedWallpaperType) {
                "Phone" -> "Phone"
                "Desktop/Tablet" -> "Desktop/Tablet"
                else -> "Auto-detect (Based on aspect ratio)"
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayWallpaperType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wallpaper Type") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Type"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { wallpaperTypeExpanded = true }
                )
                DropdownMenu(
                    expanded = wallpaperTypeExpanded,
                    onDismissRequest = { wallpaperTypeExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    wallpaperTypeOptions.forEach { option ->
                        val valueToStore = when {
                            option.startsWith("Phone") -> "Phone"
                            option.startsWith("Desktop") -> "Desktop/Tablet"
                            else -> ""
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    color = if (selectedWallpaperType == valueToStore) primaryColor else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selectedWallpaperType == valueToStore) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.selectWallpaperType(valueToStore)
                                wallpaperTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selection Header & Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                TextButton(
                    onClick = { showAddCategoryDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Category",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create New",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            CategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                categories = categories
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma-separated: neon, dark, city)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Upload State Progress UI
            if (uploadState is UploadState.Uploading) {
                val state = uploadState as UploadState.Uploading
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Uploading Document to TDLib...",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.progressPercent.toInt()}%",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit Button
            Button(
                onClick = {
                    viewModel.startUpload(
                        context = context,
                        title = title,
                        category = selectedCategory,
                        tags = tags,
                        description = description,
                        author = author
                    )
                },
                enabled = selectedUri != null && uploadState !is UploadState.Uploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload to Telegram Vault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Add New Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryInput = ""
            },
            title = {
                Text(
                    text = "Create New Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new category to add it to TeleWalls & sync with Telegram:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.createCategory(trimmed) { created ->
                                selectedCategory = created
                            }
                            showAddCategoryDialog = false
                            newCategoryInput = ""
                        }
                    },
                    enabled = newCategoryInput.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        newCategoryInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
