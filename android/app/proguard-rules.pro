# Keep line numbers for Play Console crash reports (no source file names).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures + annotations are required by Retrofit, Moshi/Gson-style
# converters, Hilt, and kotlinx.serialization.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# --- kotlinx.serialization ---
# Keep @Serializable classes and their serializers.
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Prevent obfuscation of serial names looked up reflectively.
-keepattributes RuntimeVisibleAnnotations

# --- Retrofit / OkHttp ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes GenericSignature
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keepattributes *Annotation*

# --- Coil 3 ---
-dontwarn coil3.**
-keep class coil3.** { *; }

# --- AndroidX Navigation / Compose ---
# Navigation Safe Args / deep links use reflection on generated classes.
-keep class androidx.navigation.** { *; }

# --- App-specific ---
# Retrofit service interfaces must survive shrinking (referenced only via proxy).
-keep interface org.appdevncsu.foodfinder.data.APIClient { *; }
# Hilt entry points / Application.
-keep class org.appdevncsu.foodfinder.FoodFinderApp { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
