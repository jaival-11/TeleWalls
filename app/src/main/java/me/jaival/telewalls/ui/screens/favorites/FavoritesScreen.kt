package me.jaival.telewalls.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.ui.components.AnimatedWallpaperCard
import me.jaival.telewalls.ui.components.BatchSelectionHeader
import me.jaival.telewalls.ui.dialogs.BatchEditDialog
import me.jaival.telewalls.viewmodel.HomeViewModel

@Composable
fun FavoritesScreen(
    viewModel: HomeViewModel,
    onWallpaperClick: (String) -> Unit,
    scrollToTopTrigger: Int = 0
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val gridState = rememberLazyGridState()
    var isInitialTabOpen by remember { mutableStateOf(true) }

    var selectedWallpaperIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedWallpaperIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedWallpaperIds = emptySet()
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            gridState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            isInitialTabOpen = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isSelectionMode) {
                    BatchSelectionHeader(
                        selectedCount = selectedWallpaperIds.size,
                        totalCount = favorites.size,
                        onClearSelection = { selectedWallpaperIds = emptySet() },
                        onSelectAllToggle = {
                            if (selectedWallpaperIds.size == favorites.size) {
                                selectedWallpaperIds = emptySet()
                            } else {
                                selectedWallpaperIds = favorites.map { it.id }.toSet()
                            }
                        },
                        onEditClick = { showBatchEditDialog = true },
                        onDeleteClick = { showDeleteConfirmationDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            )
                        )
                        Text(
                            text = if (favorites.size == 1) "1 Wallpaper saved" else "${favorites.size} Wallpapers saved",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Favorites Yet",
                                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the heart icon on any wallpaper to add it here!",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(favorites, key = { _, item -> item.id }) { index, wallpaper ->
                            val isSelected = wallpaper.id in selectedWallpaperIds
                            AnimatedWallpaperCard(
                                wallpaper = wallpaper,
                                index = index,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedWallpaperIds = if (isSelected) selectedWallpaperIds - wallpaper.id else selectedWallpaperIds + wallpaper.id
                                    } else {
                                        onWallpaperClick(wallpaper.id)
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(wallpaper.id) },
                                onLoadThumbnail = { viewModel.loadThumbnailOnDemand(it) },
                                animateBounce = isInitialTabOpen,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onLongClick = {
                                    selectedWallpaperIds = if (isSelected) selectedWallpaperIds - wallpaper.id else selectedWallpaperIds + wallpaper.id
                                }
                            )
                        }
                    }
                }
            }

            if (showBatchEditDialog) {
                BatchEditDialog(
                    selectedCount = selectedWallpaperIds.size,
                    categories = categories,
                    onDismissRequest = { showBatchEditDialog = false },
                    onSave = { author, wallpaperType, selectedCategory, tags ->
                        showBatchEditDialog = false
                        val selectedWallpapers = favorites.filter { it.id in selectedWallpaperIds }
                        val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.batchUpdateWallpapers(
                            wallpapers = selectedWallpapers,
                            author = author,
                            category = selectedCategory,
                            tags = if (tagsList.isNotEmpty()) tagsList else null,
                            wallpaperType = wallpaperType,
                            onComplete = { count ->
                                val msg = if (count == 1) "Updated 1 wallpaper" else "Updated $count wallpapers"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                selectedWallpaperIds = emptySet()
                            }
                        )
                    }
                )
            }

            if (showDeleteConfirmationDialog) {
                val count = selectedWallpaperIds.size
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmationDialog = false },
                    title = {
                        Text(
                            text = if (count == 1) "Delete Wallpaper?" else "Delete $count Wallpapers?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text(
                            text = if (count == 1)
                                "Are you sure you want to delete the selected wallpaper? This action cannot be undone."
                            else
                                "Are you sure you want to delete the selected $count wallpapers? This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmationDialog = false
                                val selectedWallpapers = favorites.filter { it.id in selectedWallpaperIds }
                                viewModel.deleteWallpapers(
                                    wallpapers = selectedWallpapers,
                                    onComplete = { count ->
                                        val msg = if (count == 1) "Deleted 1 wallpaper" else "Deleted $count wallpapers"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        selectedWallpaperIds = emptySet()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirmationDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}



