package org.lsposed.lspd.impl;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.lspd.core.BuildConfig;
import org.lsposed.lspd.impl.utils.LSPosedDexParser;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.nativebridge.HookBridge;
import org.lsposed.lspd.nativebridge.NativeAPI;
import org.lsposed.lspd.service.ILSPInjectedModuleService;
import org.lsposed.lspd.util.LspModuleClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.errors.XposedFrameworkError;
import io.github.libxposed.api.utils.DexParser;


@SuppressLint("NewApi")
public class LSPosedContext implements XposedInterface {

    private static final String TAG = "LSPosedContext";

    public static boolean isSystemServer;
    public static String appDir;
    public static String processName;

    static final Set<XposedModule> modules = ConcurrentHashMap.newKeySet();

    private final String mPackageName;
    private final ApplicationInfo mApplicationInfo;
    private final ILSPInjectedModuleService service;
    private final Map<String, SharedPreferences> mRemotePrefs = new ConcurrentHashMap<>();

    LSPosedContext(String packageName, ApplicationInfo applicationInfo, ILSPInjectedModuleService service) {
        this.mPackageName = packageName;
        this.mApplicationInfo = applicationInfo;
        this.service = service;
    }

    public static void callOnPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        for (XposedModule module : modules) {
            try {
                module.onPackageLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onPackageLoaded of " + module.getApplicationInfo().packageName, t);
            }
        }
    }

    public static void callOnSystemServerLoaded(XposedModuleInterface.SystemServerLoadedParam param) {
        for (XposedModule module : modules) {
            try {
                module.onSystemServerLoaded(param);
            } catch (Throwable t) {
                Log.e(TAG, "Error when calling onSystemServerLoaded of " + module.getApplicationInfo().packageName, t);
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    public static boolean loadModule(ActivityThread at, Module module) {
        try {
            Log.d(TAG, "Loading module " + module.packageName);
            var sb = new StringBuilder();
            var abis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
            for (String abi : abis) {
                sb.append(module.apkPath).append("!/lib/").append(abi).append(File.pathSeparator);
            }
            var librarySearchPath = sb.toString();
            var initLoader = XposedModule.class.getClassLoader();
            var mcl = LspModuleClassLoader.loadApk(module.apkPath, module.file.preLoadedDexes, librarySearchPath, initLoader);
            if (mcl.loadClass(XposedModule.class.getName()).getClassLoader() != initLoader) {
                Log.e(TAG, "  Cannot load module: " + module.packageName);
                Log.e(TAG, "  The Xposed API classes are compiled into the module's APK.");
                Log.e(TAG, "  This may cause strange issues and must be fixed by the module developer.");
                return false;
            }
            var ctx = new LSPosedContext(module.packageName, module.applicationInfo, module.service);
            for (var entry : module.file.moduleClassNames) {
                var moduleClass = mcl.loadClass(entry);
                Log.d(TAG, "  Loading class " + moduleClass);
                if (!XposedModule.class.isAssignableFrom(moduleClass)) {
                    Log.e(TAG, "    This class doesn't implement any sub-interface of XposedModule, skipping it");
                    continue;
                }
                try {
                    var moduleEntry = moduleClass.getConstructor(XposedInterface.class, XposedModuleInterface.ModuleLoadedParam.class);
                    var moduleContext = (XposedModule) moduleEntry.newInstance(ctx, new XposedModuleInterface.ModuleLoadedParam() {
                        @Override
                        public boolean isSystemServer() {
                            return isSystemServer;
                        }

                        @NonNull
                        @Override
                        public String getProcessName() {
                            return processName;
                        }
                    });
                    modules.add(moduleContext);
                } catch (Throwable e) {
                    Log.e(TAG, "    Failed to load class " + moduleClass, e);
                }
            }
            module.file.moduleLibraryNames.forEach(NativeAPI::recordNativeEntrypoint);
            Log.d(TAG, "Loaded module " + module.packageName + ": " + ctx);
        } catch (Throwable e) {
            Log.d(TAG, "Loading module " + module.packageName, e);
            return false;
        }
        return true;
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return BuildConfig.FRAMEWORK_NAME;
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public long getFrameworkVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public int getFrameworkPrivilege() {
        try {
            return service.getFrameworkPrivilege();
        } catch (RemoteException ignored) {
            return -1;
        }
    }

    @Override
    @NonNull
    public MethodUnhooker<Method> hook(@NonNull Method origin, @NonNull Class<? extends Hooker> hooker) {
        return LSPosedBridge.doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @NonNull
    public MethodUnhooker<Method> hook(@NonNull Method origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return LSPosedBridge.doHook(origin, priority, hooker);
    }

    @Override
    @NonNull
    public <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Class<? extends Hooker> hooker) {
        return LSPosedBridge.doHook(origin, PRIORITY_DEFAULT, hooker);
    }

    @Override
    @NonNull
    public <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return LSPosedBridge.doHook(origin, priority, hooker);
    }

    private static boolean doDeoptimize(@NonNull Executable method) {
        if (Modifier.isAbstract(method.getModifiers())) {
            throw new IllegalArgumentException("Cannot deoptimize abstract methods: " + method);
        } else if (Proxy.isProxyClass(method.getDeclaringClass())) {
            throw new IllegalArgumentException("Cannot deoptimize methods from proxy class: " + method);
        }
        return HookBridge.deoptimizeMethod(method);
    }

    @Override
    public boolean deoptimize(@NonNull Method method) {
        return doDeoptimize(method);
    }

    @Override
    public <T> boolean deoptimize(@NonNull Constructor<T> constructor) {
        return doDeoptimize(constructor);
    }

    @Nullable
    @Override
    public Object invokeOrigin(@NonNull Method method, @Nullable Object thisObject, Object[] args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        return HookBridge.invokeOriginalMethod(method, thisObject, args);
    }

    private static char getTypeShorty(Class<?> type) {
        if (type == int.class) {
            return 'I';
        } else if (type == long.class) {
            return 'J';
        } else if (type == float.class) {
            return 'F';
        } else if (type == double.class) {
            return 'D';
        } else if (type == boolean.class) {
            return 'Z';
        } else if (type == byte.class) {
            return 'B';
        } else if (type == char.class) {
            return 'C';
        } else if (type == short.class) {
            return 'S';
        } else if (type == void.class) {
            return 'V';
        } else {
            return 'L';
        }
    }

    private static char[] getExecutableShorty(Executable executable) {
        var parameterTypes = executable.getParameterTypes();
        var shorty = new char[parameterTypes.length + 1];
        shorty[0] = getTypeShorty(executable instanceof Method ? ((Method) executable).getReturnType() : void.class);
        for (int i = 1; i < shorty.length; i++) {
            shorty[i] = getTypeShorty(parameterTypes[i - 1]);
        }
        return shorty;
    }

    @Nullable
    @Override
    public Object invokeSpecial(@NonNull Method method, @NonNull Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Cannot invoke special on static method: " + method);
        }
        return HookBridge.invokeSpecialMethod(method, getExecutableShorty(method), method.getDeclaringClass(), thisObject, args);
    }

    @NonNull
    @Override
    public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, Object... args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        var obj = HookBridge.allocateObject(constructor.getDeclaringClass());
        HookBridge.invokeOriginalMethod(constructor, obj, args);
        return obj;
    }

    @NonNull
    @Override
    public <T, U> U newInstanceSpecial(@NonNull Constructor<T> constructor, @NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        var superClass = constructor.getDeclaringClass();
        if (!superClass.isAssignableFrom(subClass)) {
            throw new IllegalArgumentException(subClass + " is not inherited from " + superClass);
        }
        var obj = HookBridge.allocateObject(subClass);
        HookBridge.invokeSpecialMethod(constructor, getExecutableShorty(constructor), superClass, obj, args);
        return obj;
    }

    @Override
    public void log(@NonNull String message) {
        Log.i(TAG, mPackageName + ": " + message);
    }

    @Override
    public void log(@NonNull String message, @NonNull Throwable throwable) {
        Log.e(TAG, mPackageName + ": " + message, throwable);
    }

    @Override
    public DexParser parseDex(@NonNull ByteBuffer dexData, boolean includeAnnotations) throws IOException {
        return new LSPosedDexParser(dexData, includeAnnotations);
    }

    @NonNull
    @Override
    public ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    @NonNull
    @Override
    public SharedPreferences getRemotePreferences(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        return mRemotePrefs.computeIfAbsent(name, n -> {
            try {
                return new LSPosedRemotePreferences(service, n);
            } catch (RemoteException e) {
                log("Failed to get remote preferences", e);
                throw new XposedFrameworkError(e);
            }
        });
    }

    @NonNull
    @Override
    public String[] listRemoteFiles() {
        try {
            return service.getRemoteFileList();
        } catch (RemoteException e) {
            log("Failed to list remote files", e);
            throw new XposedFrameworkError(e);
        }
    }

    @NonNull
    @Override
    public ParcelFileDescriptor openRemoteFile(String name) throws FileNotFoundException {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        try {
            return service.openRemoteFile(name);
        } catch (RemoteException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}
