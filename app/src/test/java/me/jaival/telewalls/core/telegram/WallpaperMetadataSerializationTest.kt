package me.jaival.telewalls.core.telegram

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperMetadataSerializationTest {

    private val gson = Gson()

    @Test
    fun testDeserializeCompactReleaseJson() {
        val releaseJson = """{"a":"1000069388.png","b":"Uncategorized","c":[],"d":"897x1094","e":"897:1094","f":940903,"g":["#D0D0D0","#48B848","#088018","#5880D0","#60A860"],"h":"","i":"Woody Strode","j":1788843526516,"k":"Phone"}"""

        val metadata = gson.fromJson(releaseJson, WallpaperMetadata::class.java)

        assertEquals("1000069388.png", metadata.title)
        assertEquals("Uncategorized", metadata.category)
        assertTrue(metadata.tags.isEmpty())
        assertEquals("897x1094", metadata.resolution)
        assertEquals("897:1094", metadata.aspectRatio)
        assertEquals(940903L, metadata.sizeBytes)
        assertEquals(5, metadata.colors.size)
        assertEquals("#D0D0D0", metadata.colors[0])
        assertEquals("", metadata.description)
        assertEquals("Woody Strode", metadata.author)
        assertEquals(1788843526516L, metadata.timestamp)
        assertEquals("Phone", metadata.wallpaperType)
    }

    @Test
    fun testDeserializeLegacyLongKeyJson() {
        val debugJson = """{"aspectRatio":"897:1094","author":"Pam Beesly","category":"Uncategorized","colors":["#D0D0D0","#48B848","#088018","#5880D0","#60A860"],"description":"","resolution":"897x1094","sizeBytes":940903,"tags":[],"timestamp":1788843516027,"title":"1000069388.png","wallpaperType":"Phone"}"""

        val metadata = gson.fromJson(debugJson, WallpaperMetadata::class.java)

        assertEquals("1000069388.png", metadata.title)
        assertEquals("Uncategorized", metadata.category)
        assertEquals("Pam Beesly", metadata.author)
        assertEquals(1788843516027L, metadata.timestamp)
        assertEquals("Phone", metadata.wallpaperType)
    }

    @Test
    fun testSerializeMetadataUsesCompactKeys() {
        val metadata = WallpaperMetadata(
            title = "Sample.png",
            category = "Nature",
            author = "Jaival",
            resolution = "1080x1920",
            aspectRatio = "9:16",
            sizeBytes = 123456L,
            colors = listOf("#FFFFFF"),
            description = "Test description",
            wallpaperType = "Phone"
        )

        val json = gson.toJson(metadata)

        // Verifies compact single-letter keys (a, b, d, e, i, k) are used in both debug and release builds
        assertTrue(json.contains(""""a":"Sample.png""""))
        assertTrue(json.contains(""""b":"Nature""""))
        assertTrue(json.contains(""""i":"Jaival""""))
        assertTrue(json.contains(""""d":"1080x1920""""))
        assertTrue(json.contains(""""e":"9:16""""))
        assertTrue(json.contains(""""k":"Phone""""))
    }
}
