package org.lsposed.lspd.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.libxposed.XposedUtils;

public class LSPosedUtils implements XposedUtils {
    private final LSPosedContext context;

    LSPosedUtils(LSPosedContext context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T> Class<T> classByName(@NonNull String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public <T> Class<T> classOrNullByName(@NonNull String className, @Nullable ClassLoader classLoader) {
        throw new AbstractMethodError();
    }

    @NonNull
    @Override
    public <T> Class<T> classByBinaryName(@NonNull String binaryClassName, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        throw new AbstractMethodError();
    }

    @Nullable
    @Override
    public <T> Class<T> classOrNullByBinaryName(@NonNull String binaryClassName, @Nullable ClassLoader classLoader) {
        throw new AbstractMethodError();
    }
}
