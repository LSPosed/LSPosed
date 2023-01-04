package io.github.libxposed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

public interface XposedUtils {
    @NonNull
    <T> Class<T> findClass(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException;

    @Nullable
    <T> Class<T> findClassOrNull(@NonNull String className, @Nullable ClassLoader classLoader);

    @NonNull
    Field findField(@NonNull Class<?> clazz, @NonNull String fieldName) throws NoSuchFieldException;

    @Nullable
    Field findFieldOrNull(@NonNull Class<?> clazz, @NonNull String fieldName);

    @NonNull
    Field findField(@NonNull String className, @NonNull String fieldName, @Nullable ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException;

    @Nullable
    Field findFieldOrNull(@NonNull String className, @NonNull String fieldName, @Nullable ClassLoader classLoader);
}
