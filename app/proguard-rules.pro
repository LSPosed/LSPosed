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
-overloadaggressively

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,InnerClasses
