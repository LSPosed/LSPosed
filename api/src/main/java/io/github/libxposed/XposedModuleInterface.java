package io.github.libxposed;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public interface XposedModuleInterface {
    interface ModuleLoadedParam {
        boolean isSystemServer();

        @NonNull
        String getProcessName();

        @NonNull
        String getAppDataDir();

        @Nullable
        Bundle getExtras();
    }

    interface PackageLoadedParam {
        @NonNull
        String getPackageName();

        @NonNull
        ApplicationInfo getAppInfo();

        @NonNull
        ClassLoader getClassLoader();

        @NonNull
        String getProcessName();

        boolean isFirstApplication();

        @Nullable
        Bundle getExtras();
    }

    interface ResourcesLoadedParam {
        @NonNull
        String getPackageName();

        @NonNull
        XposedResources getResources();

        @Nullable
        Bundle getExtras();
    }

    default void onPackageLoaded(@NonNull PackageLoadedParam param) {
    }

    default void onResourceLoaded(@NonNull ResourcesLoadedParam param) {
    }
}
