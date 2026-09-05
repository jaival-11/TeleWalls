package me.jaival.telewalls.ui.navigation

object ScreenRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val COLLECTIONS = "collections"
    const val FAVORITES = "favorites"
    const val UPLOAD = "upload"
    const val AUTH = "auth"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{wallpaperId}"
    const val CATEGORY_DETAIL = "category_detail/{categoryName}"

    fun detailRoute(wallpaperId: String): String = "detail/$wallpaperId"
    fun categoryDetailRoute(categoryName: String): String = "category_detail/${android.net.Uri.encode(categoryName)}"
}
