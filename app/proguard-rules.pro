-keep class io.github.lsposed.manager.Constants { *; }
-keepclasseswithmembers class io.github.lsposed.manager.receivers.LSPosedManagerServiceClient {
    private static android.os.IBinder binder;
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

-repackageclasses
-allowaccessmodification
