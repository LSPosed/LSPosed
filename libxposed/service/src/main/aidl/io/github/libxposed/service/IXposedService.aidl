package io.github.libxposed.service;

interface IXposedService {
    const int API = 100;
    const String AUTHORITY_SUFFIX = ".XposedService";
    const String SEND_BINDER = "SendBinder";

    long getAPIVersion() = 1;
}
