package me.jaival.telewalls.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import me.jaival.telewalls.data.repository.Wallpaper
import me.jaival.telewalls.ui.components.AnimatedWallpaperCard
import me.jaival.telewalls.ui.components.BatchSelectionHeader
import me.jaival.telewalls.ui.components.CategoryChips
import me.jaival.telewalls.ui.components.ExpandableUploadFab
import me.jaival.telewalls.ui.components.ShimmerCard
import me.jaival.telewalls.ui.dialogs.BatchEditDialog
import me.jaival.telewalls.ui.dialogs.MassUploadDialog
import me.jaival.telewalls.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWallpaperClick: (String) -> Unit,
    onSingleUploadClick: () -> Unit = {},
    onMultiUploadClick: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
    onSearchQueryChange: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    var showMassUploadDialog by remember { mutableStateOf(false) }
    var selectedWallpaperIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedWallpaperIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode || searchQuery.isNotEmpty() || onBackClick != null) {
        if (isSelectionMode) {
            selectedWallpaperIds = emptySet()
            return@BackHandler
        }
        if (searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
            viewModel.updateSearchQuery("")
        }
        if (onBackClick != null) {
            onBackClick()
        }
    }

    val gridState = rememberLazyGridState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var isInitialTabOpen by remember { mutableStateOf(true) }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            gridState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex != 0 || currentOffset != 0) {
                    isInitialTabOpen = false
                }
                if (currentIndex == 0 && currentOffset == 0) {
                    isFabVisible = true
                } else {
                    val indexDelta = currentIndex - previousIndex
                    val offsetDelta = currentOffset - previousScrollOffset

                    if (indexDelta > 0 || (indexDelta == 0 && offsetDelta > 10)) {
                        // Scrolling down: FAB slides down & disappears
                        isFabVisible = false
                    } else if (indexDelta < 0 || (indexDelta == 0 && offsetDelta < -10)) {
                        // Scrolling up: FAB slides back up & appears
                        isFabVisible = true
                    }
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    var isFabClicked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isFabClicked = false
    }

    val fabIconRotation by animateFloatAsState(
        targetValue = if (isFabClicked) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_rotation"
    )

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                        totalCount = wallpapers.size,
                        onClearSelection = { selectedWallpaperIds = emptySet() },
                        onSelectAllToggle = {
                            if (selectedWallpaperIds.size == wallpapers.size) {
                                selectedWallpaperIds = emptySet()
                            } else {
                                selectedWallpaperIds = wallpapers.map { it.id }.toSet()
                            }
                        },
                        onEditClick = { showBatchEditDialog = true },
                        onDeleteClick = { showDeleteConfirmationDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    // Header bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TeleWalls",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                )
                            )
                            Text(
                                text = "Personal Wallpaper Gallery",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = primaryColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Vault Sync Status Indicator
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { viewModel.reindexChannel() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = primaryColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = "Reindex from channel",
                                        tint = primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRefreshing) "Reindexing..." else "Telegram",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            onSearchQueryChange(it)
                            viewModel.updateSearchQuery(it)
                        },
                        placeholder = {
                            Text(
                                text = "Search title, tags, color (#FF007A, red)...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    onSearchQueryChange("")
                                    viewModel.updateSearchQuery("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Category Chips Bar
                    val availableCategories = remember(categories) {
                        categories.filter { !it.equals("Uncategorized", ignoreCase = true) && !it.equals("uncategorised", ignoreCase = true) }
                    }
                    CategoryChips(
                        selectedCategories = selectedCategories,
                        onCategorySelected = { viewModel.selectCategory(it) },
                        categories = availableCategories,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wallpaper Staggered Grid
                if (isRefreshing && wallpapers.isEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) {
                            ShimmerCard()
                        }
                    }
                } else if (wallpapers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Wallpapers" else "No Wallpapers Found",
                                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Try broadening your search or use fewer keywords"
                                else
                                    "Upload some wallpapers",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
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
                        itemsIndexed(wallpapers, key = { _, item -> item.id }) { index, wallpaper ->
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

            // Expandable Floating Action Button with bounce animations and speed dial options
            if (!isSelectionMode) {
                ExpandableUploadFab(
                    isVisible = isFabVisible,
                    onSingleUploadClick = onSingleUploadClick,
                    onMultiUploadClick = {
                        onMultiUploadClick()
                        showMassUploadDialog = true
                    }
                )
            }

            if (showMassUploadDialog) {
                MassUploadDialog(
                    categories = categories,
                    onDismissRequest = { showMassUploadDialog = false }
                )
            }

            if (showBatchEditDialog) {
                BatchEditDialog(
                    selectedCount = selectedWallpaperIds.size,
                    categories = categories,
                    onDismissRequest = { showBatchEditDialog = false },
                    onSave = { author, wallpaperType, selectedCategory, tags ->
                        showBatchEditDialog = false
                        val selectedWallpapers = wallpapers.filter { it.id in selectedWallpaperIds }
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
                                val selectedWallpapers = wallpapers.filter { it.id in selectedWallpaperIds }
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
