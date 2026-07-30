# kotlinx.serialization keeps generated serializers off the entry-point graph.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.uaehn.shasha.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.uaehn.shasha.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.uaehn.shasha.data.model.**$$serializer { *; }

# OkHttp / Okio ship references to optional platform classes.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Media3 loads decoder extensions reflectively when present.
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.hls.** { *; }
