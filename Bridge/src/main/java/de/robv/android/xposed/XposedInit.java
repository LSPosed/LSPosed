package de.robv.android.xposed;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.os.ZygoteInit;
import com.elderdrivers.riru.xposed.BuildConfig;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.services.BaseService;

import static com.elderdrivers.riru.xposed.entry.hooker.XposedBlackListHooker.BLACK_LIST_PACKAGE_NAME;
import static de.robv.android.xposed.XposedHelpers.closeSilently;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticLongField;

public final class XposedInit {
    private static final String TAG = XposedBridge.TAG;
    private static boolean startsSystemServer = false;
    private static final String startClassName = ""; // ed: no support for tool process anymore

    public static final String INSTALLER_PACKAGE_NAME = "com.solohsu.android.edxp.manager";
    public static final String INSTALLER_LEGACY_PACKAGE_NAME = "de.robv.android.xposed.installer";
    @SuppressLint("SdCardPath")
    public static final String INSTALLER_DATA_BASE_DIR = Build.VERSION.SDK_INT >= 24
            ? "/data/user_de/0/" + INSTALLER_PACKAGE_NAME + "/"
            : "/data/data/" + INSTALLER_PACKAGE_NAME + "/";
    private static final String INSTANT_RUN_CLASS = "com.android.tools.fd.runtime.BootstrapApplication";
    // TODO not supported yet
    private static boolean disableResources = true;
    private static final String[] XRESOURCES_CONFLICTING_PACKAGES = {"com.sygic.aura"};

    private XposedInit() {
    }

    private static volatile AtomicBoolean bootstrapHooked = new AtomicBoolean(false);

    /**
     * Hook some methods which we want to create an easier interface for developers.
     */
    /*package*/
    public static void initForZygote(boolean isSystem) throws Throwable {
        if (!bootstrapHooked.compareAndSet(false, true)) {
            return;
        }
        startsSystemServer = isSystem;
        Router.startBootstrapHook(isSystem);
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
    }

    /*package*/
    static void hookResources() throws Throwable {
        // ed: not for now
    }

    private static boolean needsToCloseFilesForFork() {
        // ed: we always start to do our work after forking finishes
        return false;
    }

    /**
     * Try to load all modules defined in <code>INSTALLER_DATA_BASE_DIR/conf/modules.list</code>
     */
    private static volatile AtomicBoolean modulesLoaded = new AtomicBoolean(false);

    public static void loadModules() throws IOException {
        if (!modulesLoaded.compareAndSet(false, true)) {
            return;
        }
        final String filename = INSTALLER_DATA_BASE_DIR + "conf/modules.list";
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
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Loading modules from " + apk);

        if (!TextUtils.isEmpty(apk) && apk.contains(BLACK_LIST_PACKAGE_NAME)) {
            if (BuildConfig.DEBUG)
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
                    if (BuildConfig.DEBUG)
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
                            ((IXposedHookZygoteInit) moduleInstance).initZygote(param);
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

    public static void logD(String prefix) {
        Utils.logD(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()));
    }

    public static void logE(String prefix, Throwable throwable) {
        Utils.logE(String.format("%s: pkg=%s, prc=%s", prefix, AndroidAppHelper.currentPackageName(),
                AndroidAppHelper.currentProcessName()), throwable);
    }
}
