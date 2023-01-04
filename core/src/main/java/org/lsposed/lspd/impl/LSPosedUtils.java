package org.lsposed.lspd.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;
import io.github.libxposed.XposedUtils;

public class LSPosedUtils implements XposedUtils {
    private final LSPosedContext context;

    LSPosedUtils(LSPosedContext context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T> Class<T> findClass(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        return (Class<T>) ClassUtils.getClass(classLoader, className.replace('/', '.'), false);
    }

    @Nullable
    @Override
    public <T> Class<T> findClassOrNull(@NonNull String className, @Nullable ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Field findField(@NonNull Class<?> clazz, @NonNull String fieldName) throws NoSuchFieldException {
        try {
            return XposedHelpers.findField(clazz, fieldName);
        } catch (NoSuchFieldError e) {
            throw new NoSuchFieldException(e.getMessage());
        }
    }

    @Override
    public Field findFieldOrNull(@NonNull Class<?> clazz, @NonNull String fieldName) {
        try {
            return findField(clazz, fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Field findField(@NonNull String className, @NonNull String fieldName, @Nullable ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException {
        var clazz = findClass(className, classLoader);
        return findField(clazz, fieldName);
    }

    @Nullable
    @Override
    public Field findFieldOrNull(@NonNull String className, @NonNull String fieldName, @Nullable ClassLoader classLoader) {
        try {
            return findField(className, fieldName, classLoader);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            return null;
        }
    }
}
