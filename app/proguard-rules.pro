# OkHttp / ML Kit / Room
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# Keep Kotlin metadata used by Compose/Room
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
