# Keep kotlinx.serialization working with R8
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class com.pizzadough.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
