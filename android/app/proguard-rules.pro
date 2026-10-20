# Smart Campus Silencer - ProGuard / R8 Rules

# Preserve JNI / C++ Native methods and bridge
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.campussilencer.app.nativebridge.** { *; }

# Preserve App Models and Data Classes
-keep class com.campussilencer.app.data.** { *; }

# Preserve Android Components (Activity, Service, Receiver, App)
-keep public class com.campussilencer.app.CampusSilencerApp
-keep public class com.campussilencer.app.MainActivity
-keep public class com.campussilencer.app.receiver.** { *; }
-keep public class com.campussilencer.app.service.** { *; }
-keep public class com.campussilencer.app.audio.** { *; }
-keep public class com.campussilencer.app.ui.** { *; }

# WorkManager Worker
-keep class androidx.work.** { *; }
-keep class com.campussilencer.app.service.SmartSilencerWorker { *; }

# Coroutines & Kotlin reflection
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Material components & AndroidX
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
