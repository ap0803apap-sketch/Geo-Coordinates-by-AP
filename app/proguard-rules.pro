# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Sign-In specific
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class retrofit2.** { *; }
-keepnames class okhttp3.** { *; }

# Room
-keep class androidx.room.paging.** { *; }

# Glide
-keep public class * extends com.github.bumptech.glide.module.AppGlideModule
-keep public class * extends com.github.bumptech.glide.module.LibraryGlideModule
-keep class com.github.bumptech.glide.GeneratedAppGlideModuleImpl

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$companion {
    public kotlinx.serialization.KSerializer serializer();
}

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
