package org.lsposed.lspd.hooker;

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.Context;
import android.content.ContextParams;
import android.os.Build;
import android.os.Process;

import org.lsposed.lspd.util.Hookers;
import org.lsposed.lspd.util.LspModuleClassLoader;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;

public class AttachHooker extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        var at = (ActivityThread) param.thisObject;
        var moduleList = serviceClient.getModulesList();
        moduleList.forEach(module -> {
            try {
                XposedInit.getLoadedModules().add(module.packageName);
                var loadedApk = at.getPackageInfoNoCheck(module.applicationInfo, null);
                var sb = new StringBuilder();
                var abis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
                for (String abi : abis) {
                    sb.append(module.apkPath).append("!/lib/").append(abi).append(File.pathSeparator);
                }
                var librarySearchPath = sb.toString();

                var initLoader = XposedInit.class.getClassLoader();
                var mcl = LspModuleClassLoader.loadApk(module.apkPath, module.file.preLoadedDexes, librarySearchPath, initLoader);
                XposedHelpers.setObjectField(loadedApk, "mClassLoader", mcl);
                var c = Class.forName("android.app.ContextImpl");
                var ctor = c.getDeclaredConstructors()[0];
                ctor.setAccessible(true);
                var args = new Object[ctor.getParameterTypes().length];
                for (int i = 0; i < ctor.getParameterTypes().length; ++i) {
                    if (ctor.getParameterTypes()[i] == LoadedApk.class) {
                        args[i] = loadedApk;
                        continue;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ctor.getParameterTypes()[i] == ContextParams.class) {
                            args[i] = new ContextParams.Builder().build();
                            continue;
                        }
                    }
                    if (ctor.getParameterTypes()[i] == ActivityThread.class) {
                        args[i] = at;
                        continue;
                    }
                    if (ctor.getParameterTypes()[i] == int.class) {
                        args[i] = 0;
                        continue;
                    }
                    args[i] = null;
                }
                var ctx = (Context) ctor.newInstance(args);
                Hookers.logD("Loaded module " + module.packageName + ": " + ctx);
            } catch (Throwable e) {
                Hookers.logE("Loading module " + module.packageName, e);
            }
        });
    }
}
