# Metrolist Wear OS ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
