-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.beust.jcommander.** {
    <methods>;
}
-keep class org.lsposed.lspd.cli.handler.*$* {*;}
-keepattributes *Annotation*
-keepclasseswithmembers class org.lsposed.lspd.cli.Main {
    public static void main(java.lang.String[]);
}
-keepclasseswithmembers class org.lsposed.lspd.Main {
    public static void main(java.lang.String[]);
}
-keepclasseswithmembers class org.lsposed.lspd.service.LogcatService {
    private int refreshFd(boolean);
}
-keepclassmembers class ** implements android.content.ContextWrapper {
    public int getUserId();
    public android.os.UserHandle getUser();
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
