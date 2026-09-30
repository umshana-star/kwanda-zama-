# ==============================================================================
# Comprehensive ProGuard / R8 Configuration for Zama AI Production Release
# Tailored for:
#   1. Room Database (Entities, DAOs, TypeConverters, Migrations, KSP *_Impl)
#   2. Moshi JSON Serialization Models, Adapters, and Kotlin Reflection
#   3. Firebase Dependencies (AI Logic, App Check, reCAPTCHA, Firestore, Auth, GMS)
#   4. AndroidX Biometric, Security Crypto (Tink), Lifecycle ViewModels & Networking
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Bytecode Attributes & Stack Trace Preservation
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions, SourceFile, LineNumberTable, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-renamesourcefileattribute SourceFile

# Keep application entry points and MainActivity
-keep class com.example.MainActivity { *; }
-keep class com.example.BuildConfig { *; }

# Keep @Keep annotated classes and members across AndroidX and Google libraries
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <init>(...);
}

# ------------------------------------------------------------------------------
# 2. Room Database Rules (Entities, DAOs, Migrations, and KSP Generated Classes)
# ------------------------------------------------------------------------------
# Keep all RoomDatabase subclasses and their constructors/methods
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep @androidx.room.Database class * { *; }

# Keep KSP / annotation-processor generated Room implementations (*_Impl)
-keep class **_Impl { *; }
-keep class *_*Impl { *; }

# Keep all @Entity, @Dao, and @DatabaseView classes with all fields, getters, setters, and constructors
-keep @androidx.room.Entity class * {
    <init>(...);
    <fields>;
    <methods>;
}
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.DatabaseView class * { *; }

# Keep any class members annotated with Room annotations (@ColumnInfo, @PrimaryKey, @Embedded, @Relation, @TypeConverter, @Query, @Insert, @Update, @Delete)
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
    @androidx.room.* <init>(...);
}

# Keep Room schema migrations and SQLite support interfaces
-keep class * extends androidx.room.migration.Migration { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# Explicitly keep Zama AI Room database, DAOs, Entities, and local repositories
-keep class com.example.data.local.ZamaDatabase { *; }
-keep class com.example.data.local.ZamaDatabase$* { *; }
-keep class com.example.data.local.ChatLogEntity { *; }
-keep class com.example.data.local.ChatMessageEntity { *; }
-keep class com.example.data.local.BookingEventEntity { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.model.WhatsAppAgentEntity { *; }
-keep class com.example.model.WhatsAppAgentEventEntity { *; }

# ------------------------------------------------------------------------------
# 3. Moshi JSON Serialization Models, Adapters & Kotlin Reflection
# ------------------------------------------------------------------------------
# Preserve Kotlin Metadata so KotlinJsonAdapterFactory can inspect data classes, properties, and default parameters
-keep class kotlin.Metadata { *; }

# Keep @JsonClass annotated models and generated/reflective JsonAdapters
-keep @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
    <methods>;
}
-keep class **JsonAdapter {
    <init>(...);
    *;
}
-keep class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
    *;
}

# Keep custom Moshi adapter methods and @Json annotated fields/methods
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep synthetic constructors and DefaultConstructorMarker for Kotlin data classes deserialized via Moshi
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
}
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }

# Keep Enum values() and valueOf() for Moshi EnumJsonAdapter
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    @com.squareup.moshi.* <fields>;
}

# Keep Moshi internal & Kotlin reflection adapter classes
-keep class com.squareup.moshi.** { *; }
-keep class com.squareup.moshi.kotlin.reflect.** { *; }
-dontwarn com.squareup.moshi.**
-dontwarn kotlin.reflect.**

# Explicitly keep all Zama AI domain, remote API, triage, analytics, calendar, and export models
-keep class com.example.data.remote.** { *; }
-keep class com.example.model.** { *; }
-keep class com.example.triage.** { *; }
-keep class com.example.calendar.** { *; }
-keep class com.example.analytics.** { *; }
-keep class com.example.export.** { *; }
-keep class com.example.autoreply.** { *; }
-keep class com.example.audio.** { *; }
-keep class com.example.util.** { *; }

# ------------------------------------------------------------------------------
# 4. Firebase Dependencies (AI Logic, App Check, reCAPTCHA, Firestore, Auth, GMS)
# ------------------------------------------------------------------------------
# Keep Firebase ComponentRegistrar classes loaded dynamically via ComponentDiscoveryService
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
    *;
}
-keep class com.google.firebase.components.** { *; }

# Keep Firebase initialization provider and core classes
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.FirebaseOptions { *; }
-keep class com.google.firebase.StartupTime { *; }
-keep @com.google.firebase.annotations.** class * { *; }
-keepclassmembers class * {
    @com.google.firebase.annotations.* <fields>;
    @com.google.firebase.annotations.* <methods>;
}

# Keep Firebase AI Logic (Gemini / Vertex AI in Firebase) classes and serialization models
-keep class com.google.firebase.ai.** { *; }
-keep class com.google.firebase.vertexai.** { *; }

# Keep Firebase App Check (Play Integrity, reCAPTCHA Enterprise, Debug provider)
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.android.recaptcha.** { *; }

# Keep Firebase Firestore & Firebase Auth models and custom object mapper annotations
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keep @com.google.firebase.firestore.ThrowOnExtraProperties class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

# Keep Google Play Services base & Tasks API used by Firebase
-keep @com.google.android.gms.common.annotation.KeepName class * { *; }
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.recaptcha.**
-dontwarn com.google.protobuf.**
-dontwarn io.grpc.**

# ------------------------------------------------------------------------------
# 5. AndroidX Biometric, Security Crypto (Tink) & Lifecycle ViewModels
# ------------------------------------------------------------------------------
# Keep AndroidX Security Crypto (EncryptedSharedPreferences / MasterKey) & Google Tink primitives
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Keep AndroidX Biometric classes, callbacks, and Zama security managers
-keep class androidx.biometric.** { *; }
-keep class com.example.security.** { *; }

# Keep ViewModels and ViewModelProvider.Factory implementations
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * implements androidx.lifecycle.ViewModelProvider$Factory { *; }
-keep class com.example.ui.viewmodel.** { *; }
-keep class com.example.ui.theme.** { *; }

# ------------------------------------------------------------------------------
# 6. Retrofit, OkHttp, Kotlinx Serialization & Coroutines
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepattributes Signature, InnerClasses, EnclosingMethod

# Keep Kotlinx Serialization & Coroutines metadata (used by Firebase AI SDK & Ktor/JSON)
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keep,includedescriptorclasses class com.google.firebase.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.coroutines.**
