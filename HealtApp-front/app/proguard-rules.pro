# Правила R8 для release-сборки HealthApp.
# Основная задача — не сломать Gson (он читает поля через рефлексию)
# и оставить читаемые стектрейсы в отчётах о падениях.

# Стектрейсы: имена файлов и номера строк
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Дженерики и аннотации нужны Gson/Retrofit для разбора типов
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ---------- Gson ----------
# DTO и доменные модели сериализуются по именам полей: переименование их ломает.
-keep class com.example.healtapp.data.network.dto.** { *; }
-keep class com.example.healtapp.domain.model.** { *; }

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ---------- Retrofit ----------
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ---------- OkHttp ----------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------- Sentry ----------
-keepattributes *Annotation*
-dontwarn io.sentry.**

# ---------- Kotlin ----------
-keepclassmembers class kotlin.Metadata { public <methods>; }

# ---------- Health Connect / необязательные зависимости ----------
-dontwarn androidx.health.connect.**
