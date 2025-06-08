# PDAM App ProGuard Rules - ERROR FREE VERSION

# Keep line numbers and source file for debugging
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Keep main app classes
-keep class com.metromultindo.pdam_app_v2.** { *; }

# Keep data models (API responses)
-keep class com.metromultindo.pdam_app_v2.data.model.** { *; }
-keep class com.metromultindo.pdam_app_v2.data.response.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Retrofit & OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil (Image loading)
-keep class coil.** { *; }
-dontwarn coil.**

# Landscapist
-keep class com.github.skydoves.landscapist.** { *; }
-dontwarn com.github.skydoves.landscapist.**

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Native code (JNI) - Generic untuk semua native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Jika ada class khusus untuk native (sesuaikan dengan nama class actual Anda)
# -keep class com.metromultindo.pdam_app_v2.YourNativeClass { *; }