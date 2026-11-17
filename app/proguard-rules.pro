# Proguard / R8 Optimization & Retention Rules for Downloader

# Keep general reflection attributes
-keepattributes *Annotation*,EnclosingMethod,InnerClasses,Signature,SourceFile,LineNumberTable

# 1. Keep native method declarations and JNI bindings
-keepclasseswithmembers class * {
    native <methods>;
}

# 2. youtubedl-android, FFmpeg, Aria2c, and Python runtime
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
-keep class io.github.junkfood02.** { *; }
-dontwarn io.github.junkfood02.**

# youtubedl dependencies: Apache Commons Compress, Commons IO, Jackson
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.io.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Keep yt-dlp JSON mapper models
-keepclassmembers class com.yausername.youtubedl_android.mapper.** { *; }
-keepclassmembers class io.github.junkfood02.youtubedl_android.mapper.** { *; }

# 3. Media3 (ExoPlayer & MediaSession)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# 4. Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# 5. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 6. Downloader App Data Models & Engine
-keep class com.leanwell.downloader.model.** { *; }
-keep class com.leanwell.downloader.engine.VideoMetadata** { *; }
-keep class com.leanwell.downloader.engine.FormatOption** { *; }
-keep class com.leanwell.downloader.engine.PlaylistItemInfo** { *; }
-keep class com.leanwell.downloader.engine.ExtractedPlaylistInfo** { *; }
-keep class com.leanwell.downloader.engine.ExtractedVideoInfo** { *; }
-keep class com.leanwell.downloader.engine.VideoDefinition** { *; }
-keep class com.leanwell.downloader.engine.PlaylistQualityPreset** { *; }

# 7. Strip verbose/debug logs in release builds for performance and payload size
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
