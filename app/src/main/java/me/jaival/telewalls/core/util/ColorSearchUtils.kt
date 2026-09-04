package me.jaival.telewalls.core.util

import android.graphics.Color
import me.jaival.telewalls.data.repository.Wallpaper
import kotlin.math.sqrt

object ColorSearchUtils {

    data class RgbColor(val r: Int, val g: Int, val b: Int)

    private val COLOR_NAME_MAP = mapOf(
        "red" to RgbColor(255, 0, 0),
        "darkred" to RgbColor(139, 0, 0),
        "maroon" to RgbColor(128, 0, 0),
        "crimson" to RgbColor(220, 20, 60),
        "pink" to RgbColor(255, 192, 203),
        "hotpink" to RgbColor(255, 105, 180),
        "magenta" to RgbColor(255, 0, 255),
        "fuchsia" to RgbColor(255, 0, 255),
        "rose" to RgbColor(255, 0, 127),
        "purple" to RgbColor(128, 0, 128),
        "violet" to RgbColor(238, 130, 238),
        "indigo" to RgbColor(75, 0, 130),
        "blue" to RgbColor(0, 0, 255),
        "navy" to RgbColor(0, 0, 128),
        "darkblue" to RgbColor(0, 0, 139),
        "lightblue" to RgbColor(173, 216, 230),
        "skyblue" to RgbColor(135, 206, 235),
        "cyan" to RgbColor(0, 255, 255),
        "aqua" to RgbColor(0, 255, 255),
        "teal" to RgbColor(0, 128, 128),
        "turquoise" to RgbColor(64, 224, 208),
        "green" to RgbColor(0, 255, 0),
        "darkgreen" to RgbColor(0, 100, 0),
        "forestgreen" to RgbColor(34, 139, 34),
        "lime" to RgbColor(0, 255, 0),
        "mint" to RgbColor(152, 255, 152),
        "yellow" to RgbColor(255, 255, 0),
        "gold" to RgbColor(255, 215, 0),
        "orange" to RgbColor(255, 165, 0),
        "coral" to RgbColor(255, 127, 80),
        "brown" to RgbColor(165, 42, 42),
        "black" to RgbColor(0, 0, 0),
        "amoled" to RgbColor(0, 0, 0),
        "dark" to RgbColor(20, 20, 30),
        "white" to RgbColor(255, 255, 255),
        "light" to RgbColor(240, 240, 240),
        "gray" to RgbColor(128, 128, 128),
        "grey" to RgbColor(128, 128, 128),
        "silver" to RgbColor(192, 192, 192)
    )

    fun parseColorFromQuery(query: String): RgbColor? {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return null

        val colorByName = COLOR_NAME_MAP[clean.replace(" ", "")]
        if (colorByName != null) return colorByName

        val rgbMatch = Regex("""(?:rgb\s*\(\s*)?(\d{1,3})[\s,]+(\d{1,3})[\s,]+(\d{1,3})\s*\)?""").find(clean)
        if (rgbMatch != null) {
            val (r, g, b) = rgbMatch.destructured
            val rInt = r.toIntOrNull() ?: -1
            val gInt = g.toIntOrNull() ?: -1
            val bInt = b.toIntOrNull() ?: -1
            if (rInt in 0..255 && gInt in 0..255 && bInt in 0..255) {
                return RgbColor(rInt, gInt, bInt)
            }
        }

        var hex = clean.removePrefix("#")
        if (hex.all { it in '0'..'9' || it in 'a'..'f' }) {
            if (hex.length == 3) {
                hex = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
            }
            if (hex.length == 6) {
                val r = hex.substring(0, 2).toIntOrNull(16)
                val g = hex.substring(2, 4).toIntOrNull(16)
                val b = hex.substring(4, 6).toIntOrNull(16)
                if (r != null && g != null && b != null) {
                    return RgbColor(r, g, b)
                }
            } else if (hex.length == 8) {
                val r = hex.substring(2, 4).toIntOrNull(16)
                val g = hex.substring(4, 6).toIntOrNull(16)
                val b = hex.substring(6, 8).toIntOrNull(16)
                if (r != null && g != null && b != null) {
                    return RgbColor(r, g, b)
                }
            }
        }

        try {
            val hexString = if (clean.startsWith("#")) clean else "#$clean"
            val parsedInt = Color.parseColor(hexString)
            val r = (parsedInt shr 16) and 0xFF
            val g = (parsedInt shr 8) and 0xFF
            val b = parsedInt and 0xFF
            return RgbColor(r, g, b)
        } catch (_: Exception) {
            // Not a valid color
        }

        return null
    }

    fun hexToRgb(hexString: String): RgbColor? {
        val clean = hexString.trim().removePrefix("#")
        var hex = clean
        if (hex.length == 3 && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            hex = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
        }
        if (hex.length >= 6) {
            val r = hex.substring(0, 2).toIntOrNull(16)
            val g = hex.substring(2, 4).toIntOrNull(16)
            val b = hex.substring(4, 6).toIntOrNull(16)
            if (r != null && g != null && b != null) {
                return RgbColor(r, g, b)
            }
        }
        return null
    }

    fun colorDistance(c1: RgbColor, c2: RgbColor): Double {
        val rDiff = (c1.r - c2.r).toDouble()
        val gDiff = (c1.g - c2.g).toDouble()
        val bDiff = (c1.b - c2.b).toDouble()
        val rMean = (c1.r + c2.r) / 2.0
        val rWeight = 2.0 + rMean / 256.0
        val gWeight = 4.0
        val bWeight = 2.0 + (255.0 - rMean) / 256.0
        return sqrt((rWeight * rDiff * rDiff + gWeight * gDiff * gDiff + bWeight * bDiff * bDiff) / 3.0)
    }

    fun evaluateWallpaper(
        wallpaper: Wallpaper,
        query: String,
        selectedCategory: String
    ): Pair<Boolean, Double> {
        if (!selectedCategory.equals("All", ignoreCase = true) &&
            !wallpaper.category.equals(selectedCategory, ignoreCase = true)
        ) {
            return Pair(false, 0.0)
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return Pair(true, wallpaper.timestamp.toDouble())
        }

        val parsedTargetColor = parseColorFromQuery(trimmedQuery)
        var totalScore = 0.0
        var isMatch = false

        if (parsedTargetColor != null) {
            val paletteRgbList = wallpaper.colors.mapNotNull { hexToRgb(it) }
            if (paletteRgbList.isNotEmpty()) {
                var minDistance = Double.MAX_VALUE
                for (paletteColor in paletteRgbList) {
                    val dist = colorDistance(parsedTargetColor, paletteColor)
                    if (dist < minDistance) {
                        minDistance = dist
                    }
                }
                if (minDistance <= 95.0) {
                    isMatch = true
                    val colorScore = (1000.0 - minDistance * 5.0).coerceAtLeast(100.0)
                    totalScore += colorScore
                }
            }
        }

        val cleanQueryHex = trimmedQuery.removePrefix("#").lowercase()
        if (cleanQueryHex.length >= 2) {
            for (colorHex in wallpaper.colors) {
                val cleanHex = colorHex.removePrefix("#").lowercase()
                if (cleanHex.contains(cleanQueryHex)) {
                    isMatch = true
                    totalScore += 500.0
                    break
                }
            }
        }

        val tokens = trimmedQuery.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        var textMatchCount = 0

        val lowerTitle = wallpaper.title.lowercase()
        val lowerCategory = wallpaper.category.lowercase()
        val lowerAuthor = wallpaper.author.lowercase()
        val lowerDesc = wallpaper.description.lowercase()
        val lowerFileName = wallpaper.fileName.lowercase()
        val lowerTags = wallpaper.tags.map { it.lowercase() }

        for (token in tokens) {
            var tokenMatched = false

            if (lowerTitle.contains(token)) {
                tokenMatched = true
                totalScore += if (lowerTitle == token) 200.0 else 100.0
            }
            if (lowerCategory.contains(token)) {
                tokenMatched = true
                totalScore += 80.0
            }
            if (lowerTags.any { it.contains(token) }) {
                tokenMatched = true
                totalScore += 90.0
            }
            if (lowerAuthor.contains(token)) {
                tokenMatched = true
                totalScore += 50.0
            }
            if (lowerDesc.contains(token)) {
                tokenMatched = true
                totalScore += 40.0
            }
            if (lowerFileName.contains(token)) {
                tokenMatched = true
                totalScore += 30.0
            }

            if (tokenMatched) {
                textMatchCount++
            }
        }

        if (textMatchCount == tokens.size && tokens.isNotEmpty()) {
            isMatch = true
        }

        return Pair(isMatch, totalScore)
    }
}
