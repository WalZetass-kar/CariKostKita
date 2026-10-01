# ProGuard rules for CariKostKita
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.mindrot.jbcrypt.* <methods>;
}
-keep class com.carikostkita.data.model.** { *; }
