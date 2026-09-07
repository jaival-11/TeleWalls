package me.jaival.telewalls.core.updater

object SemVer {
    /**
     * Compare two semantic version strings (e.g. "v1.0.0" vs "v1.1.0" or "1.0.0" vs "1.1.0").
     * Returns true if [latest] is strictly greater than [current].
     */
    fun isUpdateAvailable(current: String, latest: String): Boolean {
        val currentParts = parseVersion(current)
        val latestParts = parseVersion(latest)

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val curr = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > curr) return true
            if (lat < curr) return false
        }
        return false
    }

    fun parseVersion(version: String): List<Int> {
        val cleaned = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }

        return cleaned.split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    fun formatVersionName(version: String): String {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        return "v$cleaned"
    }
}
