package me.jaival.telewalls.core.util

import me.jaival.telewalls.data.repository.Wallpaper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSearchUtilsTest {

    private fun createWallpaper(category: String, title: String = "Test Wallpaper") = Wallpaper(
        id = "1",
        messageId = 1L,
        chatId = 100L,
        fileId = "f1",
        fileName = "test.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 100L,
        localPath = null,
        thumbnailPath = null,
        title = title,
        author = "Author",
        category = category,
        tags = emptyList(),
        resolution = "1080x1920",
        aspectRatio = "9:16",
        colors = emptyList(),
        description = "",
        timestamp = 1000L,
        isFavorite = false,
        wallpaperType = "Phone"
    )

    @Test
    fun testAllSelectedMatchesAnyCategory() {
        val wpAmoled = createWallpaper("AMOLED")
        val wpNature = createWallpaper("Nature")

        val (match1, _) = ColorSearchUtils.evaluateWallpaper(wpAmoled, "", setOf("All"))
        val (match2, _) = ColorSearchUtils.evaluateWallpaper(wpNature, "", setOf("All"))

        assertTrue(match1)
        assertTrue(match2)
    }

    @Test
    fun testMultiCategorySelectionMatchesSelectedCategories() {
        val wpAmoled = createWallpaper("AMOLED")
        val wpNature = createWallpaper("Nature")
        val wpCars = createWallpaper("Cars")

        val selected = setOf("AMOLED", "Nature")

        val (match1, _) = ColorSearchUtils.evaluateWallpaper(wpAmoled, "", selected)
        val (match2, _) = ColorSearchUtils.evaluateWallpaper(wpNature, "", selected)
        val (match3, _) = ColorSearchUtils.evaluateWallpaper(wpCars, "", selected)

        assertTrue(match1)
        assertTrue(match2)
        assertFalse(match3)
    }

    @Test
    fun testSingleCategorySelectionMatchesOnlyThatCategory() {
        val wpAmoled = createWallpaper("AMOLED")
        val wpNature = createWallpaper("Nature")

        val selected = setOf("AMOLED")

        val (match1, _) = ColorSearchUtils.evaluateWallpaper(wpAmoled, "", selected)
        val (match2, _) = ColorSearchUtils.evaluateWallpaper(wpNature, "", selected)

        assertTrue(match1)
        assertFalse(match2)
    }
}
