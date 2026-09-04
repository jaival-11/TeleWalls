package me.jaival.telewalls.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val defaultCategories = listOf(
    "All", "AMOLED", "Nature", "Minimal", "Sci-Fi", "Architecture", "Abstract", "Cars"
)

@Composable
fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categories: List<String> = defaultCategories,
    modifier: Modifier = Modifier
) {
    val selectedBg = MaterialTheme.colorScheme.primaryContainer
    val selectedText = MaterialTheme.colorScheme.onPrimaryContainer
    val unselectedBg = MaterialTheme.colorScheme.surfaceContainer
    val unselectedText = MaterialTheme.colorScheme.onSurfaceVariant

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            val chipBg by animateColorAsState(
                targetValue = if (isSelected) selectedBg else unselectedBg,
                animationSpec = tween(250),
                label = "chip_bg"
            )
            val chipText by animateColorAsState(
                targetValue = if (isSelected) selectedText else unselectedText,
                animationSpec = tween(250),
                label = "chip_text"
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(chipBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onCategorySelected(category)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = chipText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

