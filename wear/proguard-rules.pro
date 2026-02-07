# Metrolist Wear OS ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }

# Ignore missing classes (not needed at runtime)
-dontwarn com.google.re2j.**
-dontwarn java.beans.**
-dontwarn java.lang.management.**
-dontwarn javax.script.**
-dontwarn org.mozilla.javascript.**
