-keep class de.robv.android.xposed.** {*;}
-keep class io.github.libxposed.** {*;}
-keep class android.** { *; }
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers class org.lsposed.lspd.impl.LSPosedContext {
    getAssets(...);
    getResources(...);
    getPackageManager(...);
    getMainLooper(...);
    setTheme(...);
    getTheme(...);
    getClassLoader(...);
    getPackageName(...);
    getApplicationInfo(...);
    getPackageResourcePath(...);
    getPackageCodePath(...);
    getSharedPreferences(...);
    moveSharedPreferencesFrom(...);
    deleteSharedPreferences(...);
    openFileInput(...);
    deleteFile(...);
    getFileStreamPath(...);
    getDataDir(...);
    getFilesDir(...);
    getNoBackupFilesDir(...);
    getCacheDir(...);
    getCodeCacheDir(...);
    fileList(...);
    getDir(...);
    openOrCreateDatabase(...);
    moveDatabaseFrom(...);
    deleteDatabase(...);
    getDatabasePath(...);
    databaseList(...);
    getSystemService(...);
    getSystemServiceName(...);
    createPackageContext(...);
    createConfigurationContext(...);
    getFrameworkName(...);
    getFrameworkVersion(...);
    getFrameworkVersionCode(...);
    getFrameworkPrivilege(...);
    featuredMethod(...);
    hookBefore(...);
    hookAfter(...);
    hook(...);
    deoptimize(...);
    invokeOrigin(...);
    invokeSpecial(...);
    newInstanceOrigin(...);
    newInstanceSpecial(...);
    log(...);
    parseDex(...);
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}
-repackageclasses
-allowaccessmodification
-dontwarn org.slf4j.impl.StaticLoggerBinder
