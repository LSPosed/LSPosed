package org.meowcat.edxposed.manager.xposed;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;
import android.os.FileObserver;
import android.os.StrictMode;
import android.os.UserHandle;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.installer.XposedApp;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static java.util.Collections.binarySearch;
import static org.meowcat.edxposed.manager.BuildConfig.APPLICATION_ID;

@Keep
public class Enhancement implements IXposedHookLoadPackage {

    private static final String mPretendXposedInstallerFlag = "pretend_xposed_installer";
    private static final String mHideEdXposedManagerFlag = "hide_edxposed_manager";

    private static final String LEGACY_INSTALLER = "de.robv.android.xposed.installer";

    private static final List<String> HIDE_WHITE_LIST = Arrays.asList( // TODO: more whitelist packages
            APPLICATION_ID, // Whitelist or crash
            LEGACY_INSTALLER, // for safety
            "com.android.providers.downloads", // For download modules
            "com.android.providers.downloads.ui",
            "com.android.packageinstaller", // For uninstall EdXposed Manager
            "com.google.android.packageinstaller",
            "com.android.systemui", // For notifications
            "com.android.permissioncontroller", // For permissions grant
            "com.topjohnwu.magisk", // For superuser root grant
            "eu.chainfire.supersu"
    ); // UserHandle.isCore(uid) will auto pass

    private static final SparseArray<List<String>> modulesList = new SparseArray<>();
    private static final SparseArray<FileObserver> modulesListObservers = new SparseArray<>();

    static {
        Collections.sort(HIDE_WHITE_LIST);
    }

    private static boolean getFlagState(int user, String flag) {
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            return new File(String.format("/data/user_de/%s/%s/conf/%s", user, APPLICATION_ID, flag)).exists();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private static List<String> getModulesList(final int user) {
        final int index = modulesList.indexOfKey(user);
        if (index >= 0) {
            return modulesList.valueAt(index);
        }

        final String filename = String.format("/data/user_de/%s/%s/conf/enabled_modules.list", user, APPLICATION_ID);
        final FileObserver observer = new FileObserver(filename) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                switch (event) {
                    case FileObserver.MODIFY:
                        modulesList.put(user, readModulesList(filename));
                        break;
                    case FileObserver.MOVED_FROM:
                    case FileObserver.MOVED_TO:
                    case FileObserver.MOVE_SELF:
                    case FileObserver.DELETE:
                    case FileObserver.DELETE_SELF:
                        modulesList.remove(user);
                        modulesListObservers.remove(user);
                        break;
                }
            }
        };
        modulesListObservers.put(user, observer);
        final List<String> list = readModulesList(filename);
        modulesList.put(user, list);
        observer.startWatching();
        return list;
    }

    private static List<String> readModulesList(final String filename) {
        XposedBridge.log("EdXposedManager: Reading modules list " + filename + "...");
        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            final File listFile = new File(filename);
            final List<String> list = new ArrayList<>();
            try {
                final FileReader fileReader = new FileReader(listFile);
                final BufferedReader bufferedReader = new BufferedReader(fileReader);
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    list.add(str);
                }
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e) {
                XposedBridge.log(e);
            }
            Collections.sort(list);
            return list;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            final Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                XposedBridge.log("Failed to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {
            // com.android.server.pm.PackageManagerService.getInstalledApplications(int flag, int userId)
            findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getInstalledApplications", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if ((boolean) callStaticMethod(UserHandle.class, "isCore", packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[1];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (binarySearch(HIDE_WHITE_LIST, packageName) >= 0) {
                                return;
                            }
                            if (binarySearch(getModulesList(userId), packageName) >= 0) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") final List<ApplicationInfo> applicationInfoList = (List<ApplicationInfo>) callMethod(param.getResult(), "getList");
                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                for (ApplicationInfo applicationInfo : applicationInfoList) {
                                    if (applicationInfo.packageName.equals(APPLICATION_ID)) {
                                        applicationInfo.packageName = LEGACY_INSTALLER;
                                        applicationInfoList.add(applicationInfo);
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                for (ApplicationInfo applicationInfo : applicationInfoList) {
                                    if (applicationInfo.packageName.equals(APPLICATION_ID) || applicationInfo.packageName.equals(LEGACY_INSTALLER)) {
                                        applicationInfoList.remove(applicationInfo);
                                        break;
                                    }
                                }
                            }
                        }
                        param.setResult(param.getResult()); // "reset" the result to indicate that we handled it
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getInstalledPackages(int flag, int userId)
            findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getInstalledPackages", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if ((boolean) callStaticMethod(UserHandle.class, "isCore", packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[1];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (binarySearch(HIDE_WHITE_LIST, packageName) >= 0) {
                                return;
                            }
                            if (binarySearch(getModulesList(userId), packageName) >= 0) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") final List<PackageInfo> packageInfoList = (List<PackageInfo>) callMethod(param.getResult(), "getList");
                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                for (PackageInfo packageInfo : packageInfoList) {
                                    if (packageInfo.packageName.equals(APPLICATION_ID)) {
                                        packageInfo.packageName = LEGACY_INSTALLER;
                                        packageInfoList.add(packageInfo);
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                for (PackageInfo packageInfo : packageInfoList) {
                                    if (packageInfo.packageName.equals(APPLICATION_ID) || packageInfo.packageName.equals(LEGACY_INSTALLER)) {
                                        packageInfoList.remove(packageInfo);
                                        break;
                                    }
                                }
                            }
                        }
                        param.setResult(param.getResult()); // "reset" the result to indicate that we handled it
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getApplicationInfo(String packageName, int flag, int userId)
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if ((boolean) callStaticMethod(UserHandle.class, "isCore", packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[2];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (binarySearch(HIDE_WHITE_LIST, packageName) >= 0) {
                                return;
                            }
                            if (binarySearch(getModulesList(userId), packageName) >= 0) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                if (param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.args[0] = APPLICATION_ID;
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                if (param.args[0].equals(APPLICATION_ID) || param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.setResult(null);
                                }
                            }
                        }
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getPackageInfo(String packageName, int flag, int userId)
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getPackageInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int packageUid = Binder.getCallingUid();
                        if ((boolean) callStaticMethod(UserHandle.class, "isCore", packageUid)) {
                            return;
                        }

                        final int userId = (int) param.args[2];
                        boolean isXposedModule = false;
                        final String[] packages = (String[]) callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (binarySearch(HIDE_WHITE_LIST, packageName) >= 0) {
                                return;
                            }
                            if (binarySearch(getModulesList(userId), packageName) >= 0) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        if (isXposedModule) {
                            if (getFlagState(userId, mPretendXposedInstallerFlag)) {
                                if (param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.args[0] = APPLICATION_ID;
                                }
                            }
                        } else {
                            if (getFlagState(userId, mHideEdXposedManagerFlag)) {
                                if (param.args[0].equals(APPLICATION_ID) || param.args[0].equals(LEGACY_INSTALLER)) {
                                    param.setResult(null);
                                }
                            }
                        }
                    }
                }
            });
            // Hook AM to remove restrict of EdXposed Manager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final XC_MethodHook hook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                };
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appRestrictedInBackgroundLocked", hook);
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appServicesRestrictedInBackgroundLocked", hook);
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "getAppStartModeLocked", hook);
            }
        } else if (lpparam.packageName.equals(APPLICATION_ID)) {
            // Make sure Xposed work
            XposedHelpers.findAndHookMethod(XposedApp.class.getName(), lpparam.classLoader, "isEnhancementEnabled", XC_MethodReplacement.returnConstant(true));
            // XposedHelpers.findAndHookMethod(StatusInstallerFragment.class.getName(), lpparam.classLoader, "isSELinuxEnforced", XC_MethodReplacement.returnConstant(SELinuxHelper.isSELinuxEnforced()));
        }
    }

}