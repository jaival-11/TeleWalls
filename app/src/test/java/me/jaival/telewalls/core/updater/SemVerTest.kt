package me.jaival.telewalls.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun testUpdateAvailableWhenLatestIsHigher() {
        assertTrue(SemVer.isUpdateAvailable("v1.0.0", "v1.1.0"))
        assertTrue(SemVer.isUpdateAvailable("1.0.0", "1.1.0"))
        assertTrue(SemVer.isUpdateAvailable("v1.0.0", "v1.0.1"))
        assertTrue(SemVer.isUpdateAvailable("v1.0.0", "v2.0.0"))
    }

    @Test
    fun testNoUpdateWhenVersionsAreEqual() {
        assertFalse(SemVer.isUpdateAvailable("v1.0.0", "v1.0.0"))
        assertFalse(SemVer.isUpdateAvailable("1.1.0", "v1.1.0"))
        assertFalse(SemVer.isUpdateAvailable("v2.5.1", "2.5.1"))
    }

    @Test
    fun testNoUpdateWhenCurrentIsHigher() {
        assertFalse(SemVer.isUpdateAvailable("v1.1.0", "v1.0.0"))
        assertFalse(SemVer.isUpdateAvailable("v2.0.0", "v1.9.9"))
    }

    @Test
    fun testFormatVersionName() {
        assertEquals("v1.1.0", SemVer.formatVersionName("1.1.0"))
        assertEquals("v1.1.0", SemVer.formatVersionName("v1.1.0"))
    }
}
