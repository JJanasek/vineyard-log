# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class cz.janek.vineyardlog.**$$serializer { *; }
-keepclassmembers class cz.janek.vineyardlog.** { *** Companion; }
-keepclasseswithmembers class cz.janek.vineyardlog.** { kotlinx.serialization.KSerializer serializer(...); }
