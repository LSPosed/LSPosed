package io.github.libxposed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface XposedUtils {
    @NonNull
    <T> Class<T> classByName(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException;

    @Nullable
    <T> Class<T> classOrNullByName(@NonNull String className, @Nullable ClassLoader classLoader);

    @NonNull
    <T> Class<T> classByBinaryName(@NonNull String binaryClassName, @Nullable ClassLoader classLoader) throws ClassNotFoundException;

    @Nullable
    <T> Class<T> classOrNullByBinaryName(@NonNull String binaryClassName, @Nullable ClassLoader classLoader);
}
