package org.meowcat.edxposed.manager.xposed;import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;

import androidx.annotation.Keep;

import org.meowcat.edxposed.manager.StatusInstallerFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static org.meowcat.edxposed.manager.BuildConfig.APPLICATION_ID;

@Keep
public class Enhancement implements IXposedHookLoadPackage {

    private static final String mPretendXposedInstallerFlag = "pretend_xposed_installer";
    private static final String mHideEdXposedManagerFlag = "hide_edxposed_manager";

    private static final String LEGACY_INSTALLER = "de.robv.android.xposed.installer";

    private static final List HIDE_WHITE_LIST = Arrays.asList( // TODO: more whitelist packages
            APPLICATION_ID, // Whitelist or crash
            "com.android.providers.downloads", // For download modules
            "com.android.providers.downloads.ui",
            "com.android.packageinstaller", // For uninstall EdXposed Manager
            "com.google.android.packageinstaller",
            "com.android.systemui", // For notifications
            "com.android.permissioncontroller", // For permissions grant
            "com.topjohnwu.magisk", // For superuser root grant
            "eu.chainfire.supersu"
    ); // System server (uid <= 1000) will auto pass

    private static List modulesList = null;

    private static boolean getFlagState(int user, String flag) {
        return new File(String.format("/data/user_de/%s/%s/conf/%s", user, APPLICATION_ID, flag)).exists();
    }

    private static List getModulesList(int user) {
        if (modulesList != null) {
            return modulesList;
        }
        final File listFile = new File(String.format("/data/user_de/%s/%s/conf/enabled_modules.list", user, APPLICATION_ID));
        List<String> list = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(listFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                list.add(str);
            }
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        modulesList = list;
        return list;
    }

    private static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedBridge.hookAllMethods(hookClass, methodName, callback).size() == 0)
                XposedBridge.log("Failed to hook " + methodName + " method in " + className);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("android")) {
            // android.app.ApplicationPackageManager.getInstalledApplicationsAsUser(int flag, int userId)
            findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplicationsAsUser", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int userId = (int) param.args[1];
                        final int packageUid = Binder.getCallingUid();

                        boolean isXposedModule = false;
                        final String[] packages =
                                (String[]) XposedHelpers.callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0 || packageUid <= 1000) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (getModulesList(userId).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") List<ApplicationInfo> applicationInfoList = (List<ApplicationInfo>) param.getResult();
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
                        param.setResult(applicationInfoList);
                    }
                }
            });
            // android.app.ApplicationPackageManager.getInstalledPackagesAsUser(int flag, int userId)
            findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackagesAsUser", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int userId = (int) param.args[1];
                        final int packageUid = Binder.getCallingUid();

                        boolean isXposedModule = false;
                        final String[] packages =
                                (String[]) XposedHelpers.callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0 || packageUid <= 1000) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (getModulesList(userId).contains(packageName)) {
                                isXposedModule = true;
                                break;
                            }
                        }

                        @SuppressWarnings("unchecked") List<PackageInfo> packageInfoList = (List<PackageInfo>) param.getResult();
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
                        param.setResult(packageInfoList);
                    }
                }
            });
            // com.android.server.pm.PackageManagerService.getApplicationInfo(String packageName, int flag, int userId)
            hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.classLoader, "getApplicationInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args[0] != null) {
                        final int userId = (int) param.args[2];
                        final int packageUid = Binder.getCallingUid();

                        boolean isXposedModule = false;
                        final String[] packages =
                                (String[]) XposedHelpers.callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0 || packageUid <= 1000) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (getModulesList(userId).contains(packageName)) {
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
                        final int userId = (int) param.args[2];
                        final int packageUid = Binder.getCallingUid();

                        boolean isXposedModule = false;
                        final String[] packages =
                                (String[]) XposedHelpers.callMethod(param.thisObject, "getPackagesForUid", packageUid);
                        if (packages == null || packages.length == 0 || packageUid <= 1000) {
                            return;
                        }
                        for (String packageName : packages) {
                            if (HIDE_WHITE_LIST.contains(packageName)) {
                                return;
                            }
                            if (getModulesList(userId).contains(packageName)) {
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
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appRestrictedInBackgroundLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "appServicesRestrictedInBackgroundLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
                hookAllMethods("com.android.server.am.ActivityManagerService", lpparam.classLoader, "getAppStartModeLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args != null && param.args[1] != null) {
                            if (param.args[1].equals(APPLICATION_ID)) {
                                param.setResult(0);
                            }
                        }
                    }
                });
            }
        } else if (lpparam.packageName.equals(APPLICATION_ID)) {
            // Make sure Xposed work
            XposedHelpers.findAndHookMethod(StatusInstallerFragment.class.getName(), lpparam.classLoader, "isEnhancementEnabled", XC_MethodReplacement.returnConstant(true));
            // XposedHelpers.findAndHookMethod(StatusInstallerFragment.class.getName(), lpparam.classLoader, "isSELinuxEnforced", XC_MethodReplacement.returnConstant(SELinuxHelper.isSELinuxEnforced()));
        }
    }

}