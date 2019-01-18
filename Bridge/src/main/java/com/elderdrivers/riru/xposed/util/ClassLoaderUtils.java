package com.elderdrivers.riru.xposed.util;

import android.os.Build;
import android.util.ArrayMap;

import com.elderdrivers.riru.xposed.BuildConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dalvik.system.PathClassLoader;

public class ClassLoaderUtils {

    public static final String DEXPATH = "/system/framework/edxposed.dex:/system/framework/eddalvikdx.dex:/system/framework/eddexmaker.dex";

    public static void replaceParentClassLoader(ClassLoader appClassLoader) {
        if (appClassLoader == null) {
            Utils.logE("appClassLoader is null, you might be kidding me?");
            return;
        }
        try {
            ClassLoader curCL = ClassLoaderUtils.class.getClassLoader();
            ClassLoader parent = appClassLoader;
            ClassLoader lastChild = appClassLoader;
            while (parent != null) {
                ClassLoader tmp = parent.getParent();
                if (tmp == curCL) {
                    Utils.logD("replacing has been done before, skip.");
                    return;
                }
                if (tmp == null) {
                    Utils.logD("before replacing =========================================>");
                    dumpClassLoaders(appClassLoader);
                    Field parentField = ClassLoader.class.getDeclaredField("parent");
                    parentField.setAccessible(true);
                    parentField.set(curCL, parent);
                    parentField.set(lastChild, curCL);
                    Utils.logD("after replacing ==========================================>");
                    dumpClassLoaders(appClassLoader);
                }
                lastChild = parent;
                parent = tmp;
            }
        } catch (Throwable throwable) {
            Utils.logE("error when replacing class loader.", throwable);
        }
    }

    private static void dumpClassLoaders(ClassLoader classLoader) {
        if (BuildConfig.DEBUG) {
            while (classLoader != null) {
                Utils.logD(classLoader + " =>");
                classLoader = classLoader.getParent();
            }
        }
    }

    public static List<ClassLoader> getAppClassLoader() {
        List<ClassLoader> cacheLoaders = new ArrayList<>(0);
        try {
            Utils.logD("start getting app classloader");
            Class appLoadersClass = Class.forName("android.app.ApplicationLoaders");
            Field loadersField = appLoadersClass.getDeclaredField("gApplicationLoaders");
            loadersField.setAccessible(true);
            Object loaders = loadersField.get(null);
            Field mLoaderMapField = loaders.getClass().getDeclaredField("mLoaders");
            mLoaderMapField.setAccessible(true);
            ArrayMap<String, ClassLoader> mLoaderMap = (ArrayMap<String, ClassLoader>) mLoaderMapField.get(loaders);
            Utils.logD("mLoaders size = " + mLoaderMap.size());
            cacheLoaders = new ArrayList<>(mLoaderMap.values());
        } catch (Exception ex) {
            Utils.logE("error get app class loader.", ex);
        }
        return cacheLoaders;
    }

    private static HashSet<ClassLoader> classLoaders = new HashSet<>();

    public static boolean addPathToClassLoader(ClassLoader classLoader) {
        if (!(classLoader instanceof PathClassLoader)) {
            Utils.logW(classLoader + " is not a BaseDexClassLoader!!!");
            return false;
        }
        if (classLoaders.contains(classLoader)) {
            Utils.logD(classLoader + " has been hooked before");
            return true;
        }
        try {
            PathClassLoader baseDexClassLoader = (PathClassLoader) classLoader;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                baseDexClassLoader.addDexPath(DEXPATH);
            } else {
                DexUtils.injectDexAtFirst(DEXPATH, baseDexClassLoader);
            }
            classLoaders.add(classLoader);
            return true;
        } catch (Throwable throwable) {
            Utils.logE("error when addPath to ClassLoader: " + classLoader, throwable);
        }
        return false;
    }

}
