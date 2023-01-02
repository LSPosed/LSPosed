package io.github.libxposed.service;

interface IXposedService {
    const int API = 100;
    const String AUTHORITY_SUFFIX = ".XposedService";
    const String SEND_BINDER = "SendBinder";

    // framework details
    long getAPIVersion() = 1;
    String implementationName() = 2;
    String implementationVersion() = 3;
    long implementationVersionCode() = 4;

    // scope utilities
    List<String> getScope() = 10;
    oneway void requestScope(String packageName) = 11;

    // remote preference utilities
    Bundle requestRemotePreferences(String group) = 20;
    void updateRemotePreferences(String group, in Bundle diff) = 21;

    // remote file utilities
    ParcelFileDescriptor openRemoteFile(String path, int mode) = 30;
    boolean deleteRemoteFile(String path) = 31;
}
