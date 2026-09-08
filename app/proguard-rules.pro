# Keep TDLib classes and JNI methods
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# Keep models for Room and Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class me.jaival.telewalls.core.telegram.** { *; }
-keep class me.jaival.telewalls.data.local.entity.** { *; }
-keep class me.jaival.telewalls.core.updater.** { *; }
