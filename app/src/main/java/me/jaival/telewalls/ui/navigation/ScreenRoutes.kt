package me.jaival.telewalls.ui.navigation

object ScreenRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val COLLECTIONS = "collections"
    const val FAVORITES = "favorites"
    const val UPLOAD = "upload?mode={mode}"
    const val AUTH = "auth"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val DETAIL = "detail/{wallpaperId}"
    const val CATEGORY_DETAIL = "category_detail/{categoryName}"

    fun uploadRoute(mode: String = "single"): String = "upload?mode=$mode"
    fun detailRoute(wallpaperId: String): String = "detail/$wallpaperId"
    fun categoryDetailRoute(categoryName: String): String = "category_detail/${android.net.Uri.encode(categoryName)}"
}
