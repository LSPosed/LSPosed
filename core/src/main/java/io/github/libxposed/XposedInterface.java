package io.github.libxposed;

import androidx.annotation.NonNull;

public interface XposedInterface {

    long API_VERSION = 100;

    void hook();

    @NonNull String implementationName();
    @NonNull String implementationVersion();
    long implementationVersionCode();

    void log(String message);
    void log(String message, Throwable throwable);
}
