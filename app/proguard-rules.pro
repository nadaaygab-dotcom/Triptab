# kotlinx.serialization: keep generated serializers for the app's data classes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.triptab.app.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.triptab.app.data.**$$serializer { *; }
