package io.github.libxposed;

public interface XposedInterface {
    void hook();
    void log(String message);
    void log(String message, Throwable throwable);
}
