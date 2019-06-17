package de.robv.android.xposed;

import android.app.ActivityThread;
import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.os.ZygoteInit;
import com.elderdrivers.riru.edxp.config.EdXpConfigGlobal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XCallback;
import de.robv.android.xposed.services.BaseService;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.closeSilently;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getParameterIndexByType;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticLongField;
import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

public final class XposedInit {
    private static final String TAG = XposedBridge.TAG;
    public static boolean startsSystemServer = false;
    private static final String startClassName = ""; // ed: no support for tool process anymore

    private static final String INSTANT_RUN_CLASS = "com.android.tools.fd.runtime.BootstrapApplication";
    public static volatile boolean disableResources = false;
    private static final String[] XRESOURCES_CONFLICTING_PACKAGES = {"com.sygic.aura"};

    private XposedInit() {
    }

    /**
     * Hook some methods which we want to create an easier interface for developers.
     */
    /*package*/
    public static void initForZygote(boolean isSystem) throws Throwable {
        // TODO Are these still needed for us?
        // MIUI
        if (findFieldIfExists(ZygoteInit.class, "BOOT_START_TIME") != null) {
            setStaticLongField(ZygoteInit.class, "BOOT_START_TIME", XposedBridge.BOOT_START_TIME);
        }
        // Samsung
        if (Build.VERSION.SDK_INT >= 24) {
            Class<?> zygote = findClass("com.android.internal.os.Zygote", null);
            try {
                setStaticBooleanField(zygote, "isEnhancedZygoteASLREnabled", false);
            } catch (NoSuchFieldError ignored) {
            }
        }

        hookResources();
    }

    private static void hookResources() throws Throwable {
        if (!EdXpConfigGlobal.getConfig().isResourcesHookEnabled() || disableResources) {
            return;
        }

        if (!EdXpConfigGlobal.getHookProvider().initXResourcesNative()) {
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

        if (Build.VERSION.SDK_INT <= 18) {
            classGTLR = ActivityThread.class;
            classResKey = Class.forName("android.app.ActivityThread$ResourcesKey");
        } else {
            classGTLR = Class.forName("android.app.ResourcesManager");
            classResKey = Class.forName("android.content.res.ResourcesKey");
        }

        if (Build.VERSION.SDK_INT >= 24) {
            hookAllMethods(classGTLR, "getOrCreateResources", new XC_MethodHook() {
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
        } else {
            hookAllConstructors(classResKey, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    latestResKey.set(param.thisObject);
                }
            });

            hookAllMethods(classGTLR, "getTopLevelResources", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    latestResKey.set(null);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object key = latestResKey.get();
                    if (key == null) {
                        return;
                    }
                    latestResKey.set(null);

                    String resDir = (String) getObjectField(key, "mResDir");
                    XResources newRes = cloneToXResources(param, resDir);
                    if (newRes == null) {
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    Map<Object, WeakReference<Resources>> mActiveResources =
                            (Map<Object, WeakReference<Resources>>) getObjectField(param.thisObject, "mActiveResources");
                    Object lockObject = (Build.VERSION.SDK_INT <= 18)
                            ? getObjectField(param.thisObject, "mPackages") : param.thisObject;

                    synchronized (lockObject) {
                        WeakReference<Resources> existing = mActiveResources.put(key, new WeakReference<Resources>(newRes));
                        if (existing != null && existing.get() != null && existing.get().getAssets() != newRes.getAssets()) {
                            existing.get().getAssets().close();
                        }
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= 19) {
                // This method exists only on CM-based ROMs
                hookAllMethods(classGTLR, "getTopLevelThemedResources", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String resDir = (String) param.args[0];
                        cloneToXResources(param, resDir);
                    }
                });
            }
        }

        // Invalidate callers of methods overridden by XTypedArray
//        if (Build.VERSION.SDK_INT >= 24) {
//            Set<Method> methods = getOverriddenMethods(XResources.XTypedArray.class);
//            XposedBridge.invalidateCallersNative(methods.toArray(new Member[methods.size()]));
//        }

        // Replace TypedArrays with XTypedArrays
//        hookAllConstructors(TypedArray.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                TypedArray typedArray = (TypedArray) param.thisObject;
//                Resources res = typedArray.getResources();
//                if (res instanceof XResources) {
//                    XResources.XTypedArray newTypedArray = new XResources.XTypedArray(res);
//                    XposedBridge.setObjectClass(typedArray, XResources.XTypedArray.class);
//                }
//            }
//        });

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
                                TypedArray.class, "resize", new Class[]{int.class});
                        resizeMethod.setAccessible(true);
                        resizeMethod.invoke(newResult, len);
                        param.setResult(newResult);
                    }
                });

        // Replace system resources
        XResources systemRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(Resources.getSystem(), "mClassLoader"));
        systemRes.setImpl((ResourcesImpl) XposedHelpers.getObjectField(Resources.getSystem(), "mResourcesImpl"));
        systemRes.initObject(null);
        setStaticObjectField(Resources.class, "mSystem", systemRes);

        XResources.init(latestResKey);
    }

    private static XResources cloneToXResources(XC_MethodHook.MethodHookParam param, String resDir) {
        Object result = param.getResult();
        if (result == null || result instanceof XResources ||
                Arrays.binarySearch(XRESOURCES_CONFLICTING_PACKAGES, AndroidAppHelper.currentPackageName()) == 0) {
            return null;
        }

        // Replace the returned resources with our subclass.
        XResources newRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(param.getResult(), "mClassLoader"));
        newRes.setImpl((ResourcesImpl) XposedHelpers.getObjectField(param.getResult(), "mResourcesImpl"));
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

    private static boolean needsToCloseFilesForFork() {
        // ed: we always start to do our work after forking finishes
        return false;
    }

    /**
     * Try to load all modules defined in <code>INSTALLER_DATA_BASE_DIR/conf/modules.list</code>
     */
    private static volatile AtomicBoolean modulesLoaded = new AtomicBoolean(false);

    public static void loadModules(boolean isInZygote) throws IOException {
        boolean hasLoaded = !modulesLoaded.compareAndSet(false, true);
        // dynamic module list mode doesn't apply to loading in zygote
        if (hasLoaded && (isInZygote || !EdXpConfigGlobal.getConfig().isDynamicModulesMode())) {
            return;
        }
        // FIXME module list is cleared but never could be reload again when using dynamic-module-list under multi-user environment
        XposedBridge.clearLoadedPackages();
        final String filename = EdXpConfigGlobal.getConfig().getInstallerBaseDir() + "conf/modules.list";
        BaseService service = SELinuxHelper.getAppDataFileService();
        if (!service.checkFileExists(filename)) {
            Log.e(TAG, "Cannot load any modules because " + filename + " was not found");
            return;
        }

        ClassLoader topClassLoader = XposedBridge.BOOTCLASSLOADER;
        ClassLoader parent;
        while ((parent = topClassLoader.getParent()) != null) {
            topClassLoader = parent;
        }

        InputStream stream = service.getFileInputStream(filename);
        BufferedReader apks = new BufferedReader(new InputStreamReader(stream));
        String apk;
        while ((apk = apks.readLine()) != null) {
            loadModule(apk, topClassLoader);
        }
        apks.close();
    }


    /**
     * Load a module from an APK by calling the init(String) method for all classes defined
     * in <code>assets/xposed_init</code>.
     */
    private static void loadModule(String apk, ClassLoader topClassLoader) {
        Log.i(TAG, "Loading modules from " + apk);

        // todo remove this legacy logic
        String blackListModulePackageName = EdXpConfigGlobal.getConfig().getBlackListModulePackageName();
        if (!TextUtils.isEmpty(apk) && !TextUtils.isEmpty(blackListModulePackageName)
                && apk.contains(blackListModulePackageName)) {
            Log.i(TAG, "We are going to take over black list's job...");
            return;
        }

        if (!new File(apk).exists()) {
            Log.e(TAG, "  File does not exist");
            return;
        }

        DexFile dexFile;
        try {
            dexFile = new DexFile(apk);
        } catch (IOException e) {
            Log.e(TAG, "  Cannot load module", e);
            return;
        }

        if (dexFile.loadClass(INSTANT_RUN_CLASS, topClassLoader) != null) {
            Log.e(TAG, "  Cannot load module, please disable \"Instant Run\" in Android Studio.");
            closeSilently(dexFile);
            return;
        }

        if (dexFile.loadClass(XposedBridge.class.getName(), topClassLoader) != null) {
            Log.e(TAG, "  Cannot load module:");
            Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
            Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
            Log.e(TAG, "  For details, see: http://api.xposed.info/using.html");
            closeSilently(dexFile);
            return;
        }

        closeSilently(dexFile);

        ZipFile zipFile = null;
        InputStream is;
        try {
            zipFile = new ZipFile(apk);
            ZipEntry zipEntry = zipFile.getEntry("assets/xposed_init");
            if (zipEntry == null) {
                Log.e(TAG, "  assets/xposed_init not found in the APK");
                closeSilently(zipFile);
                return;
            }
            is = zipFile.getInputStream(zipEntry);
        } catch (IOException e) {
            Log.e(TAG, "  Cannot read assets/xposed_init in the APK", e);
            closeSilently(zipFile);
            return;
        }

        ClassLoader mcl = new PathClassLoader(apk, XposedInit.class.getClassLoader());
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
                    if (XposedBridge.isZygote) {
                        if (moduleInstance instanceof IXposedHookZygoteInit) {
                            IXposedHookZygoteInit.StartupParam param = new IXposedHookZygoteInit.StartupParam();
                            param.modulePath = apk;
                            param.startsSystemServer = startsSystemServer;
                            if (EdXpConfigGlobal.getConfig().isBlackWhiteListMode()
                                    && !EdXpConfigGlobal.getConfig().isDynamicModulesMode()) {
                                // postpone initZygote callbacks under black/white list mode
                                // if dynamic modules mode is on, callback directly cause we
                                // are already in app process here
                                XposedBridge.hookInitZygote(new IXposedHookZygoteInit.Wrapper(
                                        (IXposedHookZygoteInit) moduleInstance, param));
                            } else {
                                // FIXME under dynamic modules mode, initZygote is called twice
                                ((IXposedHookZygoteInit) moduleInstance).initZygote(param);
                            }
                        }

                        if (moduleInstance instanceof IXposedHookLoadPackage)
                            XposedBridge.hookLoadPackage(new IXposedHookLoadPackage.Wrapper((IXposedHookLoadPackage) moduleInstance));

                        if (moduleInstance instanceof IXposedHookInitPackageResources)
                            XposedBridge.hookInitPackageResources(new IXposedHookInitPackageResources.Wrapper((IXposedHookInitPackageResources) moduleInstance));
                    } else {
                        if (moduleInstance instanceof IXposedHookCmdInit) {
                            IXposedHookCmdInit.StartupParam param = new IXposedHookCmdInit.StartupParam();
                            param.modulePath = apk;
                            param.startClassName = startClassName;
                            ((IXposedHookCmdInit) moduleInstance).initCmdApp(param);
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "    Failed to load class " + moduleClassName, t);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "  Failed to load module from " + apk, e);
        } finally {
            closeSilently(is);
            closeSilently(zipFile);
        }
    }

    public final static HashSet<String> loadedPackagesInProcess = new HashSet<>(1);
}
