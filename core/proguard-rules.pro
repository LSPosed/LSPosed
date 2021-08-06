-keep class de.robv.android.xposed.** {*;}
-keep class android.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclasseswithmembers class org.lsposed.lspd.core.Main {
    public static void forkSystemServerPost(android.os.IBinder);
    public static void forkAndSpecializePost(java.lang.String, java.lang.String, android.os.IBinder);
    public static void main(java.lang.String[]);
}
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keepclasseswithmembers class org.lsposed.lspd.nativebridge.ClassLinker {
    public static void onPostFixupStaticTrampolines(java.lang.Class);
}
-keepclasseswithmembers class org.lsposed.lspd.service.BridgeService {
    public static boolean execTransact(int, long, long, int);
    public static android.os.IBinder getApplicationServiceForSystemServer(android.os.IBinder, android.os.IBinder);
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
-dontwarn org.slf4j.impl.StaticLoggerBinder
