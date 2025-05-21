# ##############################################################################
# Este es tu archivo de reglas ProGuard/R8 personalizadas (proguard-rules.pro)
# Android Studio aplicará estas reglas además de las reglas del archivo
# especificado en getDefaultProguardFile('proguard-android-optimize.txt').
#
# Más información:
# - https://www.guardsquare.com/manual/configuration/usage
# - https://developer.android.com/studio/build/shrink-code
# ##############################################################################

# ###################################
# Reglas Generales de Android y Kotlin
# ###################################
# Descomenta -dontobfuscate SOLO para depuración extrema. NO para release final.
# -dontobfuscate

# Mantener puntos de entrada estándar de Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
# -keep public class com.android.vending.licensing.ILicensingService # Solo si usas licencias de Play Store antiguas

# Mantener constructores de Vistas personalizadas
-keepclassmembers class * extends android.view.View {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
   public <init>(android.content.Context, android.util.AttributeSet, int);
   public void set*(...);
}

# Mantener clases Parcelable y sus Creators
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
  public static final ** CREATOR;
}

# Mantener métodos estáticos de Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Reglas para Coroutines de Kotlin
-keepclasseswithmembernames class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
}
-keep class kotlin.coroutines.jvm.internal.DebugMetadataKt
-keepnames class kotlinx.coroutines.** { *; }

# Atributos importantes para Kotlin, reflexión y anotaciones
-keepattributes RuntimeVisibleAnnotations,Signature,InnerClasses,EnclosingMethod,AnnotationDefault,Exceptions,SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }
-keep class kotlin.annotation.** { *; }
-keep class kotlin.jvm.internal.** { *; } # Importante para la interoperabilidad y lambdas

# ###################################
# Jetpack Compose
# ###################################
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
-keepclassmembers class * { @androidx.compose.runtime.Composable <fields>; }
-keepclassmembernames class * { @androidx.compose.runtime.Composable <methods>; } # Mantiene nombres para inspección
-keepclassmembernames class * { @androidx.compose.runtime.Composable <fields>; } # Mantiene nombres para inspección
-keep class androidx.compose.runtime.Composer
-keep class androidx.compose.runtime.internal.ComposableLambda # Necesario para que Compose funcione correctamente con R8
-keepclassmembers class androidx.compose.runtime.Recomposer { <init>(...); } # Si tienes problemas con Recomposer

# ###################################
# Tus Modelos de Datos (DTOs, Entidades) - VITAL PARA GSON y TypeToken
# ¡¡¡MUY IMPORTANTE!!! Ajusta 'com.example.foodieclub.data.model' a tu paquete real si es diferente.
# ###################################
-keepattributes Signature, InnerClasses, EnclosingMethod # ASEGÚRATE DE ESTA LÍNEA AQUÍ para tus modelos
-keep public class com.example.foodieclub.data.model.** { # Asume que tus modelos están aquí
    public <init>(...); # Mantener todos los constructores públicos
    <fields>;          # Mantener todos los campos
    <methods>;         # Mantener todos los métodos (puede ser demasiado amplio, considera especificar si hay problemas)
}
# Para asegurar que los campos anotados con @SerializedName se mantengan (aunque la regla anterior debería cubrirlo)
-keepclassmembers class com.example.foodieclub.data.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Regla específica para ParsedIngredient (por si la general no es suficiente)
-keep public class com.example.foodieclub.data.model.ParsedIngredient { *; }


# ###################################
# Firebase (Auth, Firestore, Storage, Config, Crashlytics, Analytics)
# ###################################
-keepattributes Signature, InnerClasses, *Annotation*
-keep public class com.google.firebase.** { *; }
-keep public class com.google.android.gms.common.api.** { *; } # Firebase depende de Play Services
-keep public class com.google.android.gms.tasks.** { *; }

-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    # @com.google.firebase.database.PropertyName *; # Si usas Realtime DB
}

# Firebase UI Auth
-keep class com.firebase.ui.auth.** { *; }
-keepclassmembers class com.firebase.ui.auth.** { *; }

# Evitar warnings comunes de Firebase y Play Services
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.api.**
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.checker.nullness.qual.** # Relacionado con dependencias de Google

# ###################################
# Google Sign-In (play-services-auth)
# ###################################
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep interface com.google.android.gms.auth.api.signin.** { *; }
# -keep class com.google.android.gms.common.api.ResultCallback { *; } # Ya cubierto por gms.common.api.**
# -keep class com.google.android.gms.common.api.PendingResult { *; } # Ya cubierto
-keep class com.google.android.gms.auth.api.credentials.** { *; }


# ###################################
# Retrofit & OkHttp & Gson
# ###################################
# Retrofit
-dontwarn retrofit2.Platform$Java8 # Común si tu minSdk es < 26
-keepclassmembers interface * { # Para anotaciones de métodos de Retrofit
    @retrofit2.http.GET *; @retrofit2.http.POST *; @retrofit2.http.PUT *;
    @retrofit2.http.DELETE *; @retrofit2.http.PATCH *; @retrofit2.http.OPTIONS *;
    @retrofit2.http.HEAD *; @retrofit2.http.HTTP *; @retrofit2.http.Streaming *;
    @retrofit2.http.Path *; @retrofit2.http.Query *; @retrofit2.http.QueryName *;
    @retrofit2.http.QueryMap *; @retrofit2.http.Header *; @retrofit2.http.HeaderMap *;
    @retrofit2.http.Field *; @retrofit2.http.FieldMap *; @retrofit2.http.Part *;
    @retrofit2.http.PartMap *; @retrofit2.http.Body *; @retrofit2.http.Url *;
}
-keepattributes Exceptions # Para Retrofit y manejo de excepciones declaradas

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# Si usas reflexión con OkHttp (poco común para apps)
# -keep class okhttp3.** { *; }
# -keep interface okhttp3.** { *; }

# Gson (TypeToken necesita 'Signature' en los modelos, ya cubierto arriba)
-keepattributes Signature,EnclosingMethod # 'Signature' es VITAL para TypeToken
-keep class com.google.gson.Gson { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class com.google.gson.reflect.TypeToken { *; } # Mantiene miembros de TypeToken
-keep class com.google.gson.stream.** { *; }
# La regla para @SerializedName en tus modelos ya está en la sección de Modelos de Datos.

# ###################################
# Coil (Image Loading)
# ###################################
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); } # Para ViewModels con Coil
-dontwarn coil.**
# Si usas extensiones de video o GIF con Coil, podrías necesitar reglas adicionales
# -keep class coil.decode.VideoFrameDecoder { *; }
# -keep class coil.decode.ImageDecoderDecoder { *; }


# ###################################
# Google AI (Gemini SDK)
# ###################################
# Estas reglas son a menudo proporcionadas por la propia librería o inferidas, pero es bueno tenerlas.
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-dontwarn io.ktor.** # Ktor es usado por Gemini internamente
-keep class io.ktor.** { *; } # Regla más fuerte para Ktor si hay problemas
-keepnames class kotlinx.serialization.** { *; } # Gemini podría usar kotlinx.serialization

# ###################################
# Shimmer (com.valentinilk.shimmer)
# ###################################
-keep class com.valentinilk.shimmer.** { *; }
-keepclassmembers class com.valentinilk.shimmer.** { *; }

# ###################################
# DataStore Preferences
# ###################################
-keepclassmembers class **.*DataStore { # El asterisco antes de DataStore es para cualquier prefijo de clase
    androidx.datastore.core.DataStore dataStore;
}
# Esto podría ser necesario si R8 elimina la clase anidada $PreferencesImpl
-keepclassmembers class **.*DataStore$PreferencesImpl { *; }
# Descomenta y ajusta si tienes problemas específicos con tus Preferences.Keys
# -keep class com.example.foodieclub.data.preferences.YourPreferencesFile$PreferencesKeys { *; }

# ###################################
# Reglas Específicas Adicionales de TU APP (Añade aquí si es necesario)
# ###################################
# Ejemplo: Si usas reflexión en alguna utilidad propia
# -keep class com.example.foodieclub.utils.MyReflectionUtils { *; }

# Si has añadido la regla -dontoptimize para depurar, ¡¡¡ASEGÚRATE DE QUITARLA PARA EL RELEASE FINAL!!!
# -dontoptimize

# --- FIN DE REGLAS ---