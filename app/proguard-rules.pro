-keep class org.lsposed.manager.Constants {
    public static void showErrorToast(int);
}
-keepclasseswithmembers class org.lsposed.manager.receivers.LSPManagerServiceClient {
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
# temporarily disable it: https://issuetracker.google.com/issues/155606069 
# -allowaccessmodification
-overloadaggressively

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,InnerClasses

-dontwarn org.jetbrains.annotations.NotNull
-dontwarn org.jetbrains.annotations.Nullable
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt*
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
