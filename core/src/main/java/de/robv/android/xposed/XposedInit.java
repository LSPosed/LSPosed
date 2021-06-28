/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package de.robv.android.xposed;

import static org.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.sInitPackageResourcesCallbacks;
import static de.robv.android.xposed.XposedBridge.sInitZygoteCallbacks;
import static de.robv.android.xposed.XposedBridge.sLoadedPackageCallbacks;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.closeSilently;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getParameterIndexByType;
import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.ArraySet;
import android.util.Log;

import org.lsposed.lspd.nativebridge.NativeAPI;
import org.lsposed.lspd.nativebridge.ResourcesHook;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import dalvik.system.DelegateLastClassLoader;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitZygote;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import hidden.HiddenApiBridge;

public final class XposedInit {
    private static final String TAG = XposedBridge.TAG;
    public static boolean startsSystemServer = false;

    public static volatile boolean disableResources = false;

    public static void hookResources() throws Throwable {
        if (!serviceClient.isResourcesHookEnabled() || disableResources) {
            return;
        }

        if (!ResourcesHook.initXResourcesNative()) {
            Log.e(TAG, "Cannot hook resources");
            disableResources = true;
            return;
        }

        findAndHookMethod("android.app.ApplicationPackageManager", null, "getResourcesForApplication",
                ApplicationInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ApplicationInfo app = (ApplicationInfo) param.args[0];
                        XResources.setPackageNameForResDir(app.packageName,
                                app.uid == Process.myUid() ? app.sourceDir : app.publicSourceDir);
                    }
                });

        /*
         * getTopLevelResources(a)
         *   -> getTopLevelResources(b)
         *     -> key = new ResourcesKey()
         *     -> r = new Resources()
         *     -> mActiveResources.put(key, r)
         *     -> return r
         */

        final Class<?> classGTLR;
        final Class<?> classResKey;
        final ThreadLocal<Object> latestResKey = new ThreadLocal<>();
        final String createResourceMethod;

        classGTLR = android.app.ResourcesManager.class;
        classResKey = android.content.res.ResourcesKey.class;

        if (Build.VERSION.SDK_INT < 30) {
            createResourceMethod = "getOrCreateResources";
        } else {
            createResourceMethod = "createResources";
        }

        hookAllMethods(classGTLR, createResourceMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // At least on OnePlus 5, the method has an additional parameter compared to AOSP.
                final int activityTokenIdx = getParameterIndexByType(param.method, IBinder.class);
                final int resKeyIdx = getParameterIndexByType(param.method, classResKey);

                String resDir = (String) getObjectField(param.args[resKeyIdx], "mResDir");
                XResources newRes = cloneToXResources(param, resDir);
                if (newRes == null) {
                    return;
                }

                Object activityToken = param.args[activityTokenIdx];
                synchronized (param.thisObject) {
                    ArrayList<WeakReference<Resources>> resourceReferences;
                    if (activityToken != null) {
                        Object activityResources = callMethod(param.thisObject, "getOrCreateActivityResourcesStructLocked", activityToken);
                        resourceReferences = (ArrayList<WeakReference<Resources>>) getObjectField(activityResources, "activityResources");
                    } else {
                        resourceReferences = (ArrayList<WeakReference<Resources>>) getObjectField(param.thisObject, "mResourceReferences");
                    }
                    resourceReferences.add(new WeakReference(newRes));
                }
            }
        });

        findAndHookMethod(TypedArray.class, "obtain", Resources.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.getResult() instanceof XResources.XTypedArray) {
                            return;
                        }
                        if (!(param.args[0] instanceof XResources)) {
                            return;
                        }
                        XResources.XTypedArray newResult =
                                new XResources.XTypedArray((Resources) param.args[0]);
                        int len = (int) param.args[1];
                        Method resizeMethod = XposedHelpers.findMethodBestMatch(
                                TypedArray.class, "resize", int.class);
                        resizeMethod.setAccessible(true);
                        resizeMethod.invoke(newResult, len);
                        param.setResult(newResult);
                    }
                });

        // Replace system resources
        XResources systemRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(Resources.getSystem(), "mClassLoader"));
        HiddenApiBridge.Resources_setImpl(systemRes, (ResourcesImpl) XposedHelpers.getObjectField(Resources.getSystem(), "mResourcesImpl"));
        systemRes.initObject(null);
        setStaticObjectField(Resources.class, "mSystem", systemRes);

        XResources.init(latestResKey);
    }

    private static XResources cloneToXResources(XC_MethodHook.MethodHookParam param, String resDir) {
        Object result = param.getResult();
        if (result == null || result instanceof XResources) {
            return null;
        }

        // Replace the returned resources with our subclass.
        XResources newRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(param.getResult(), "mClassLoader"));
        HiddenApiBridge.Resources_setImpl(newRes, (ResourcesImpl) XposedHelpers.getObjectField(param.getResult(), "mResourcesImpl"));
        newRes.initObject(resDir);

        // Invoke handleInitPackageResources().
        if (newRes.isFirstLoad()) {
            String packageName = newRes.getPackageName();
            XC_InitPackageResources.InitPackageResourcesParam resparam = new XC_InitPackageResources.InitPackageResourcesParam(XposedBridge.sInitPackageResourcesCallbacks);
            resparam.packageName = packageName;
            resparam.res = newRes;
            XCallback.callAll(resparam);
        }

        param.setResult(newRes);
        return newRes;
    }

    /**
     * Try to load all modules defined in <code>INSTALLER_DATA_BASE_DIR/conf/modules.list</code>
     */
    private static final AtomicBoolean modulesLoaded = new AtomicBoolean(false);
    private static final Object moduleLoadLock = new Object();
    // @GuardedBy("moduleLoadLock")
    private static final ArraySet<String> loadedModules = new ArraySet<>();

    public static ArraySet<String> getLoadedModules() {
        synchronized (moduleLoadLock) {
            return loadedModules;
        }
    }

    public static boolean loadModules() throws IOException {
        boolean hasLoaded = !modulesLoaded.compareAndSet(false, true);
        if (hasLoaded) {
            return false;
        }
        synchronized (moduleLoadLock) {
            var moduleList = serviceClient.getModulesList();
            ArraySet<String> newLoadedApk = new ArraySet<>();
            moduleList.forEach((name, apk) -> {
                if (loadedModules.contains(apk)) {
                    newLoadedApk.add(apk);
                } else {
                    loadedModules.add(apk); // temporarily add it for XSharedPreference
                    boolean loadSuccess = loadModule(name, apk);
                    if (loadSuccess) {
                        newLoadedApk.add(apk);
                    }
                }

                loadedModules.clear();
                loadedModules.addAll(newLoadedApk);

                // refresh callback according to current loaded module list
                pruneCallbacks(loadedModules);
            });
        }
        return true;
    }

    // remove deactivated or outdated module callbacks
    private static void pruneCallbacks(Set<String> loadedModules) {
        synchronized (moduleLoadLock) {
            Object[] loadedPkgSnapshot = sLoadedPackageCallbacks.getSnapshot();
            Object[] initPkgResSnapshot = sInitPackageResourcesCallbacks.getSnapshot();
            Object[] initZygoteSnapshot = sInitZygoteCallbacks.getSnapshot();
            for (Object loadedPkg : loadedPkgSnapshot) {
                if (loadedPkg instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) loadedPkg).getApkPath())) {
                        sLoadedPackageCallbacks.remove((XC_LoadPackage) loadedPkg);
                    }
                }
            }
            for (Object initPkgRes : initPkgResSnapshot) {
                if (initPkgRes instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) initPkgRes).getApkPath())) {
                        sInitPackageResourcesCallbacks.remove((XC_InitPackageResources) initPkgRes);
                    }
                }
            }
            for (Object initZygote : initZygoteSnapshot) {
                if (initZygote instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) initZygote).getApkPath())) {
                        sInitZygoteCallbacks.remove((XC_InitZygote) initZygote);
                    }
                }
            }
        }
    }

    /**
     * Load all so from an APK by reading <code>assets/native_init</code>.
     * It will only store the so names but not doing anything.
     */
    private static boolean initNativeModule(ClassLoader mcl, String name) {
        InputStream is = mcl.getResourceAsStream("assets/native_init");
        if (is == null) return true;
        BufferedReader moduleLibraryReader = new BufferedReader(new InputStreamReader(is));
        String moduleLibraryName;
        try {
            while ((moduleLibraryName = moduleLibraryReader.readLine()) != null) {
                if (!moduleLibraryName.startsWith("#")) {
                    NativeAPI.recordNativeEntrypoint(moduleLibraryName);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "  Failed to load native library list from " + name, e);
            return false;
        } finally {
            closeSilently(is);
        }
        return true;

    }

    private static boolean initModule(ClassLoader mcl, String name, String apk) {
        InputStream is = mcl.getResourceAsStream("assets/xposed_init");
        if (is == null) {
            return true;
        }
        BufferedReader moduleClassesReader = new BufferedReader(new InputStreamReader(is));
        try {
            String moduleClassName;
            while ((moduleClassName = moduleClassesReader.readLine()) != null) {
                moduleClassName = moduleClassName.trim();
                if (moduleClassName.isEmpty() || moduleClassName.startsWith("#"))
                    continue;

                try {
                    Log.i(TAG, "  Loading class " + moduleClassName);
                    Class<?> moduleClass = mcl.loadClass(moduleClassName);

                    if (!IXposedMod.class.isAssignableFrom(moduleClass)) {
                        Log.e(TAG, "    This class doesn't implement any sub-interface of IXposedMod, skipping it");
                        continue;
                    } else if (disableResources && IXposedHookInitPackageResources.class.isAssignableFrom(moduleClass)) {
                        Log.e(TAG, "    This class requires resource-related hooks (which are disabled), skipping it.");
                        continue;
                    }

                    final Object moduleInstance = moduleClass.newInstance();
                    if (moduleInstance instanceof IXposedHookZygoteInit) {
                        IXposedHookZygoteInit.StartupParam param = new IXposedHookZygoteInit.StartupParam();
                        param.modulePath = apk;
                        param.startsSystemServer = startsSystemServer;

                        XposedBridge.hookInitZygote(new IXposedHookZygoteInit.Wrapper(
                                (IXposedHookZygoteInit) moduleInstance, param));
                        ((IXposedHookZygoteInit) moduleInstance).initZygote(param);
                    }

                    if (moduleInstance instanceof IXposedHookLoadPackage)
                        XposedBridge.hookLoadPackage(new IXposedHookLoadPackage.Wrapper(
                                (IXposedHookLoadPackage) moduleInstance, apk));

                    if (moduleInstance instanceof IXposedHookInitPackageResources)
                        XposedBridge.hookInitPackageResources(new IXposedHookInitPackageResources.Wrapper(
                                (IXposedHookInitPackageResources) moduleInstance, apk));
                } catch (Throwable t) {
                    Log.e(TAG, "    Failed to load class " + moduleClassName, t);
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "  Failed to load module " + name + " from " + apk, e);
            return false;
        } finally {
            closeSilently(is);
        }
        return true;
    }

    /**
     * Load a module from an APK by calling the init(String) method for all classes defined
     * in <code>assets/xposed_init</code>.
     */
    @SuppressLint("PrivateApi")
    private static boolean loadModule(String name, String apk) {
        Log.i(TAG, "Loading module " + name + " from " + apk);

        if (!new File(apk).exists()) {
            Log.e(TAG, "  File does not exist");
            return false;
        }

        // module can load it's own so
        StringBuilder nativePath = new StringBuilder();
        // Compatible with applications with 32-bit native libraries only
        if (android.os.Process.is64Bit()) {
           for (String i : Build.SUPPORTED_64_BIT_ABIS) {
                nativePath.append(apk).append("!/lib/").append(i).append(File.pathSeparator);
           }
        } else {
            for (String i : Build.SUPPORTED_32_BIT_ABIS) {
                nativePath.append(apk).append("!/lib/").append(i).append(File.pathSeparator);
            }
        }
        // Log.d(TAG, "Allowed native path" + nativePath.toString());
        ClassLoader initLoader = XposedInit.class.getClassLoader();
        ClassLoader mcl = new DelegateLastClassLoader(apk, nativePath.toString(), initLoader);

        try {
            if (mcl.loadClass(XposedBridge.class.getName()).getClassLoader() != initLoader) {
                Log.e(TAG, "  Cannot load module: " + name);
                Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
                Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
                Log.e(TAG, "  For details, see: http://api.xposed.info/using.html");
                return false;
            }
        } catch (ClassNotFoundException ignored) {
        }

        return initNativeModule(mcl, apk) && initModule(mcl, name, apk);
    }

    public final static HashSet<String> loadedPackagesInProcess = new HashSet<>(1);
}
