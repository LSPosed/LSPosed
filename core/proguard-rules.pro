-keep class de.robv.android.xposed.** {*;}
-keep class android.** { *; }
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
-dontwarn org.slf4j.impl.StaticLoggerBinder
