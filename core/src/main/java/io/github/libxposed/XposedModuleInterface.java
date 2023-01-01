package io.github.libxposed;

import android.content.pm.ApplicationInfo;
import android.content.res.XposedResources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public interface XposedModuleInterface {
    class PackageLoadedParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }

    class ResourceLoadedParam {
        public String packageName;
        public XposedResources res;
    }

    default void onPackageLoaded(@NonNull PackageLoadedParam param, @Nullable Bundle extra) {

    }

    default void onResourceLoaded(@NonNull ResourceLoadedParam param, @Nullable Bundle extra) {

    }

}
