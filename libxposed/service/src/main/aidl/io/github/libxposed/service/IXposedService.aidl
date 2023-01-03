package io.github.libxposed.service;

interface IXposedService {
    const int API = 100;
    const String AUTHORITY_SUFFIX = ".XposedService";
    const String SEND_BINDER = "SendBinder";

    // framework details
    int getAPIVersion() = 1;
    String getFrameworkName() = 2;
    String getFrameworkVersion() = 3;
    long getFrameworkVersionCode() = 4;

    // scope utilities
    List<String> getScope() = 10;
    oneway void requestScope(String packageName) = 11;

    // remote preference utilities
    Bundle requestRemotePreferences(String group) = 20;
    void updateRemotePreferences(String group, in Bundle diff) = 21;
    void deleteRemotePreferences(String group) = 22;

    // remote file utilities
    ParcelFileDescriptor openRemoteFile(String path, int mode) = 30;
    boolean deleteRemoteFile(String path) = 31;
}
