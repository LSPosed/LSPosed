-keep class de.robv.android.xposed.** {*;}
-keep class android.** { *; }
-keepclasseswithmembers class org.lsposed.lspd.core.Main {
    public static void forkSystemServerPost(android.os.IBinder);
    public static void forkAndSpecializePost(java.lang.String, java.lang.String, android.os.IBinder);
}
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keepclasseswithmembers class org.lsposed.lspd.nativebridge.ClassLinker {
    public static void onPostFixupStaticTrampolines(java.lang.Class);
}
-keepclasseswithmembers class org.lsposed.lspd.service.BridgeService {
    public static boolean *(android.os.IBinder, int, long, long, int);
    public static android.os.IBinder getApplicationServiceForSystemServer(android.os.IBinder, android.os.IBinder);
}

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
-dontwarn org.slf4j.impl.StaticLoggerBinder
