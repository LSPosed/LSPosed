package com.elderdrivers.riru.edxp.service;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.Toast;

import com.elderdrivers.riru.edxp.util.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import static com.elderdrivers.riru.edxp.service.ServiceProxy.CONFIG_PATH;

public class PackageReceiver {
    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {

        private PackageManager pm = null;

        private final String MODULES_LIST_FILENAME = "conf/modules.list";
        private final String ENABLED_MODULES_LIST_FILENAME = "conf/enabled_modules.list";

        private String getPackageName(Intent intent) {
            Uri uri = intent.getData();
            return (uri != null) ? uri.getSchemeSpecificPart() : null;
        }

        private void getPackageManager() {
            if (pm != null) return;
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            if (activityThread == null) {
                Utils.logW("ActivityThread is null");
                return;
            }
            Context context = activityThread.getSystemContext();
            if (context == null) {
                Utils.logW("context is null");
                return;
            }
            pm = context.getPackageManager();
        }

        private boolean isXposedModule(ApplicationInfo app) {
            return app != null && app.enabled && app.metaData != null && app.metaData.containsKey("xposedmodule");
        }

        private PackageInfo getPackageInfo(String packageName) {
            getPackageManager();
            if (pm == null) {
                Utils.logW("PM is null");
                return null;
            }
            try {
                return pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        private Map<String, String> loadEnabledModules(int uid) {
            HashMap<String, String> result = new HashMap<>();
            try {
                File enabledModules = new File(CONFIG_PATH, uid + "/" + ENABLED_MODULES_LIST_FILENAME);
                if (!enabledModules.exists()) return result;
                Scanner scanner = new Scanner(enabledModules);
                while (scanner.hasNextLine()) {
                    String packageName = scanner.nextLine();
                    PackageInfo info = getPackageInfo(packageName);
                    if (info != null && isXposedModule(info.applicationInfo))
                        result.put(packageName, info.applicationInfo.sourceDir);
                    else if (info == null)
                        result.put(packageName, null);
                }
            } catch (Throwable e) {
                Utils.logE("Unable to read enabled modules", e);
            }
            return result;
        }

        private boolean updateModuleList(int uid, String packageName) {
            Map<String, String> enabledModules = loadEnabledModules(uid);

            if (!enabledModules.containsKey(packageName)) return false;

            try {
                File moduleListFile = new File(CONFIG_PATH, uid + "/" + MODULES_LIST_FILENAME);
                File enabledModuleListFile = new File(CONFIG_PATH, uid + "/" + ENABLED_MODULES_LIST_FILENAME);
                if (moduleListFile.exists() && !moduleListFile.canWrite()) {
                    moduleListFile.delete();
                    moduleListFile.createNewFile();
                }
                if (enabledModuleListFile.exists() && !enabledModuleListFile.canWrite()) {
                    enabledModuleListFile.delete();
                    enabledModuleListFile.createNewFile();
                }
                PrintWriter modulesList = new PrintWriter(moduleListFile);
                PrintWriter enabledModulesList = new PrintWriter(enabledModuleListFile);
                for (Map.Entry<String, String> module : enabledModules.entrySet()) {
                    String apkPath = module.getValue();
                    if (apkPath != null) {
                        modulesList.println(module.getValue());
                        enabledModulesList.println(module.getKey());
                    } else {
                        Utils.logI(String.format("remove obsolete package %s", packageName));
                        File prefsDir = new File(CONFIG_PATH, uid + "/prefs/" + packageName);
                        File[] fileList = prefsDir.listFiles();
                        if (fileList != null) {
                            for (File childFile : fileList) {
                                childFile.delete();
                            }
                        }
                    }
                }
                modulesList.close();
                enabledModulesList.close();
            } catch (Throwable e) {
                Utils.logE("Fail to update module list", e);
            }
            return true;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_PACKAGE_REMOVED) && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                // Ignore existing packages being removed in order to be updated
                return;
            String packageName = getPackageName(intent);
            if (packageName == null)
                return;

            if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
                // make sure that the change is for the complete package, not only a
                // component
                String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
                if (components != null) {
                    boolean isForPackage = false;
                    for (String component : components) {
                        if (packageName.equals(component)) {
                            isForPackage = true;
                            break;
                        }
                    }
                    if (!isForPackage)
                        return;
                }
            }

            PackageInfo pkgInfo = getPackageInfo(packageName);

            if (pkgInfo != null && !isXposedModule(pkgInfo.applicationInfo)) return;

            try {
                UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                @SuppressLint("DiscouragedPrivateApi")
                Method m = UserManager.class.getDeclaredMethod("getUsers");
                m.setAccessible(true);
                boolean res = false;
                for (Object uh : (List<Object>) m.invoke(um)) {
                    int uid = (int) uh.getClass().getDeclaredField("id").get(uh);
                    Utils.logI("updating uid: " + uid);
                    res = updateModuleList(uid, packageName) || res;
                }
                if (res)
                    Toast.makeText(context, "EdXposed: Updated " + packageName, Toast.LENGTH_SHORT).show();
            } catch (Throwable e) {
                Utils.logW("update failed", e);
            }
        }
    };

    public static void register() {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            Utils.logW("ActivityThread is null");
            return;
        }
        Context context = activityThread.getSystemContext();
        if (context == null) {
            Utils.logW("context is null");
            return;
        }

        UserHandle userHandleAll;
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = UserHandle.class.getDeclaredField("ALL");
            userHandleAll = (UserHandle) field.get(null);
        } catch (Throwable e) {
            Utils.logW("UserHandle.ALL", e);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        HandlerThread thread = new HandlerThread("edxp-PackageReceiver");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        try {
            @SuppressLint("DiscouragedPrivateApi")
            Method method = Context.class.getDeclaredMethod("registerReceiverAsUser", BroadcastReceiver.class, UserHandle.class, IntentFilter.class, String.class, Handler.class);
            method.invoke(context, RECEIVER, userHandleAll, intentFilter, null, handler);
            Utils.logI("registered package receiver");
        } catch (Throwable e) {
            Utils.logW("registerReceiver failed", e);
        }

    }
}
