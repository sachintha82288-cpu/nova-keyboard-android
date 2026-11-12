-keep class com.nova.keyboard.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.nova.keyboard.**$$serializer { *; }
-keepclassmembers class com.nova.keyboard.** { *** Companion; }
-keepclasseswithmembers class com.nova.keyboard.** { kotlinx.serialization.KSerializer serializer(...); }
# OkHttp
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
