# =============================================================================
# Barter-me R8 / ProGuard keep rules
# =============================================================================
# This file is applied on top of proguard-android-optimize.txt.
# Goal: correctness first. R8 runs in CI on the release build, so these rules
# err on the side of keeping generously rather than aggressive shrinking.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# =============================================================================

# -----------------------------------------------------------------------------
# Global attributes
# -----------------------------------------------------------------------------
# Signature        -> generic type info, needed by Retrofit/Moshi/Gson reflection.
# *Annotation*     -> keep all annotation attributes (Retrofit/Moshi/Room read them).
# RuntimeVisible*  -> annotations that must survive to runtime.
# EnclosingMethod / InnerClasses -> needed for reflection on nested/anonymous types.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Keep line numbers for readable release crash stacktraces, but hide source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# Kotlin metadata / intrinsics
# -----------------------------------------------------------------------------
# kotlin.Metadata is read reflectively by Moshi-kotlin (reflect) and other tools.
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**
-dontwarn kotlinx.**
# Keep companion/INSTANCE fields and named-args support that Kotlin relies on.
-keepclassmembers class **$Companion { *; }
# Coroutines intrinsics use reflection on these.
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# -----------------------------------------------------------------------------
# Project models (Room entities, API DTOs, anything serialized/reflected)
# -----------------------------------------------------------------------------
# Room entities + any model reflected by Moshi live here. Keep them fully so
# field names survive (Room column mapping & Moshi reflection rely on them).
-keep class com.example.data.model.** { *; }
-keep class com.example.data.api.** { *; }
# BuildConfig is referenced (DEBUG, BARTER_API_BASE_URL); harmless to keep.
-keep class com.example.BuildConfig { *; }

# -----------------------------------------------------------------------------
# Moshi (reflection adapter + moshi-kotlin-codegen generated adapters)
# -----------------------------------------------------------------------------
# Moshi reads these annotations reflectively.
-keep @com.squareup.moshi.JsonQualifier @interface *

# Keep all members of classes annotated @JsonClass(generateAdapter = true) so the
# generated adapter (or the reflection fallback) can read every property.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); <fields>; }

# Keep fields annotated with @Json (custom JSON names).
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep the generated *JsonAdapter classes produced by moshi-kotlin-codegen and
# their (Moshi)/(Moshi, Type[]) constructors that Moshi invokes reflectively.
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# Moshi-kotlin reflection adapter (KotlinJsonAdapterFactory) reaches into kotlin-reflect.
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn org.jetbrains.annotations.**
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Enum values can be (de)serialized by name; keep valueOf/values for all enums.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -----------------------------------------------------------------------------
# Retrofit
# -----------------------------------------------------------------------------
# Retrofit does reflection on method/parameter annotations and generic types.
# These mirror Retrofit's published consumer rules.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit service interfaces: keep the interfaces, their methods and annotations.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep our own Retrofit service interface signatures intact.
-keep interface com.example.data.api.** { *; }

# Do not warn about reflective access in Retrofit.
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep,allowobfuscation,allowshrinking class retrofit2.** { *; }

# With R8 full mode, keep generic signature of Retrofit's Call/Response and any
# type used as a service-method return type / parameter.
-keep,allowobfuscation,allowshrinking @retrofit2.http.* class *
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# R8 full mode strips generic signatures from return types unless told otherwise.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# -----------------------------------------------------------------------------
# OkHttp / Okio
# -----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# OkHttp platform reflection on optional TLS providers.
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# -----------------------------------------------------------------------------
# Room
# -----------------------------------------------------------------------------
# Room ships consumer rules, but keep entities/DAO/Database + generated *_Impl
# for safety (entities are also referenced by string in raw queries/migrations).
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.**

# -----------------------------------------------------------------------------
# Firebase / Google Play services
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.firebase.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
# Firebase Auth keeps unused fields referenced reflectively / via JNI.
-keepclassmembers class com.google.firebase.** { *; }

# -----------------------------------------------------------------------------
# androidx.security.crypto (Tink-backed EncryptedSharedPreferences)
# -----------------------------------------------------------------------------
# Tink uses protobuf + reflection / service loaders for key managers.
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.crypto.tink.proto.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.protobuf.**
-keep class androidx.security.crypto.** { *; }
-dontwarn org.joda.time.**

# -----------------------------------------------------------------------------
# osmdroid (does reflection + resource/asset lookups)
# -----------------------------------------------------------------------------
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# -----------------------------------------------------------------------------
# kotlinx.coroutines
# -----------------------------------------------------------------------------
# Service loader for the main dispatcher + debug probes accessed reflectively.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
# Coroutines ships META-INF/services entries; keep the implementing classes.
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class * implements kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.debug.**
-dontwarn kotlinx.coroutines.flow.**
# DebugProbes (only present when debugging) - silence missing class warnings.
-dontwarn kotlinx.coroutines.debug.internal.DebugProbesKt

# -----------------------------------------------------------------------------
# Jetpack Compose (largely self-keeping; silence known noisy warnings)
# -----------------------------------------------------------------------------
-dontwarn androidx.compose.**
# Keep @Composable-annotated members (Compose runtime relies on them).
-keep class androidx.compose.runtime.** { *; }

# -----------------------------------------------------------------------------
# Misc / parcelable / serializable safety
# -----------------------------------------------------------------------------
# Several models implement java.io.Serializable; keep the serialization hooks.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# WorkManager worker is instantiated reflectively by name.
-keep class com.example.data.MatchNotificationWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep the Application subclass (referenced by name in the manifest).
-keep class com.example.BarterApplication { *; }
