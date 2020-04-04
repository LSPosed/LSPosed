package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.FileUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.databinding.ActivityModulesBinding;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.repo.RepoDb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import me.zhanghai.android.appiconloader.AppIconLoader;

@SuppressWarnings("OctalInteger")
public final class ModuleUtil {
    // xposedminversion below this
    public static String MODULES_LIST_FILE = XposedApp.BASE_DIR + "conf/modules.list";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    public static int MIN_MODULE_VERSION = 2; // reject modules with
    private static ModuleUtil instance = null;
    private final PackageManager pm;
    private final String frameworkPackageName;
    private final List<ModuleListener> listeners = new CopyOnWriteArrayList<>();
    private SharedPreferences pref;
    //private InstalledModule framework = null;
    private Map<String, InstalledModule> installedModules;
    private boolean isReloading = false;
    private Toast toast;

    private ModuleUtil() {
        pref = XposedApp.getInstance().getSharedPreferences("enabled_modules", Context.MODE_PRIVATE);
        pm = XposedApp.getInstance().getPackageManager();
        frameworkPackageName = XposedApp.getInstance().getPackageName();
    }

    public static synchronized ModuleUtil getInstance() {
        if (instance == null) {
            instance = new ModuleUtil();
            instance.reloadInstalledModules();
        }
        return instance;
    }

    public static int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public void reloadInstalledModules() {
        synchronized (this) {
            if (isReloading)
                return;
            isReloading = true;
        }

        Map<String, InstalledModule> modules = new HashMap<>();
        RepoDb.beginTransation();
        try {
            RepoDb.deleteAllInstalledModules();

            for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
                ApplicationInfo app = pkg.applicationInfo;
                if (!app.enabled)
                    continue;

                InstalledModule installed = null;
                if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                    installed = new InstalledModule(pkg, false);
                    modules.put(pkg.packageName, installed);
                }/* else if (isFramework(pkg.packageName)) {
                    framework = installed = new InstalledModule(pkg, true);
                }*/

                if (installed != null)
                    RepoDb.insertInstalledModule(installed);
            }

            RepoDb.setTransactionSuccessful();
        } finally {
            RepoDb.endTransation();
        }

        installedModules = modules;
        synchronized (this) {
            isReloading = false;
        }
        for (ModuleListener listener : listeners) {
            listener.onInstalledModulesReloaded(instance);
        }
    }

    public InstalledModule reloadSingleModule(String packageName) {
        PackageInfo pkg;
        try {
            pkg = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            RepoDb.deleteInstalledModule(packageName);
            InstalledModule old = installedModules.remove(packageName);
            if (old != null) {
                for (ModuleListener listener : listeners) {
                    listener.onSingleInstalledModuleReloaded(instance, packageName, null);
                }
            }
            return null;
        }

        ApplicationInfo app = pkg.applicationInfo;
        if (app.enabled && app.metaData != null && app.metaData.containsKey("xposedmodule")) {
            InstalledModule module = new InstalledModule(pkg, false);
            RepoDb.insertInstalledModule(module);
            installedModules.put(packageName, module);
            for (ModuleListener listener : listeners) {
                listener.onSingleInstalledModuleReloaded(instance, packageName,
                        module);
            }
            return module;
        } else {
            RepoDb.deleteInstalledModule(packageName);
            InstalledModule old = installedModules.remove(packageName);
            if (old != null) {
                for (ModuleListener listener : listeners) {
                    listener.onSingleInstalledModuleReloaded(instance, packageName, null);
                }
            }
            return null;
        }
    }

    public synchronized boolean isLoading() {
        return isReloading;
    }

/*    public InstalledModule getFramework() {
        return framework;
    }*/

    public String getFrameworkPackageName() {
        return frameworkPackageName;
    }

/*    private boolean isFramework(String packageName) {
        return frameworkPackageName.equals(packageName);
    }*/

//    public boolean isInstalled(String packageName) {
//        return installedModules.containsKey(packageName) || isFramework(packageName);
//    }

    public InstalledModule getModule(String packageName) {
        return installedModules.get(packageName);
    }

    public Map<String, InstalledModule> getModules() {
        return installedModules;
    }

    public void setModuleEnabled(String packageName, boolean enabled) {
        if (enabled) {
            pref.edit().putInt(packageName, 1).apply();
        } else {
            pref.edit().remove(packageName).apply();
        }
    }

    public boolean isModuleEnabled(String packageName) {
        return pref.contains(packageName);
    }

    public List<InstalledModule> getEnabledModules() {
        LinkedList<InstalledModule> result = new LinkedList<>();

        for (String packageName : pref.getAll().keySet()) {
            InstalledModule module = getModule(packageName);
            if (module != null)
                result.add(module);
            else
                setModuleEnabled(packageName, false);
        }

        return result;
    }

    public synchronized void updateModulesList(boolean showToast) {
        updateModulesList(showToast, null);
    }

    public synchronized void updateModulesList(boolean showToast, ActivityModulesBinding binding) {
        try {
            Log.i(XposedApp.TAG, "ModuleUtil -> updating modules.list");
            int installedXposedVersion = XposedApp.getXposedVersion();
            if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false) && installedXposedVersion <= 0 && showToast) {
                if (binding != null) {
                    Snackbar.make(binding.snackbar, R.string.notinstalled, Snackbar.LENGTH_SHORT).show();
                } else {
                    showToast(R.string.notinstalled);
                }
                return;
            }

            PrintWriter modulesList = new PrintWriter(MODULES_LIST_FILE);
            PrintWriter enabledModulesList = new PrintWriter(XposedApp.ENABLED_MODULES_LIST_FILE);
            List<InstalledModule> enabledModules = getEnabledModules();
            for (InstalledModule module : enabledModules) {

                if (!XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false) && (module.minVersion > installedXposedVersion || module.minVersion < MIN_MODULE_VERSION) && showToast) {
                    if (binding != null) {
                        Snackbar.make(binding.snackbar, R.string.notinstalled, Snackbar.LENGTH_SHORT).show();
                    } else {
                        showToast(R.string.notinstalled);
                    }
                    continue;
                }

                modulesList.println(module.app.sourceDir);

                try {
                    String installer = pm.getInstallerPackageName(module.app.packageName);
                    if (!PLAY_STORE_PACKAGE.equals(installer))
                        enabledModulesList.println(module.app.packageName);
                } catch (Exception ignored) {
                }
            }
            modulesList.close();
            enabledModulesList.close();

            FileUtils.setPermissions(MODULES_LIST_FILE, 00664, -1, -1);
            FileUtils.setPermissions(XposedApp.ENABLED_MODULES_LIST_FILE, 00664, -1, -1);

            if (showToast) {
                if (binding != null) {
                    Snackbar.make(binding.snackbar, R.string.xposed_module_list_updated, Snackbar.LENGTH_SHORT).show();
                } else {
                    showToast(R.string.xposed_module_list_updated);
                }
            }
        } catch (IOException e) {
            Log.e(XposedApp.TAG, "ModuleUtil -> cannot write " + MODULES_LIST_FILE, e);
            if (binding != null) {
                Snackbar.make(binding.snackbar, "cannot write " + MODULES_LIST_FILE + e, Snackbar.LENGTH_SHORT).show();
            } else {
                Toast.makeText(XposedApp.getInstance(), "cannot write " + MODULES_LIST_FILE + e, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void showToast(int message) {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        toast = Toast.makeText(XposedApp.getInstance(), XposedApp.getInstance().getString(message), Toast.LENGTH_SHORT);
        toast.show();
    }

    public void addListener(ModuleListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(ModuleListener listener) {
        listeners.remove(listener);
    }

    public interface ModuleListener {
        /**
         * Called whenever one (previously or now) installed module has been
         * reloaded
         */
        void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, InstalledModule module);

        /**
         * Called whenever all installed modules have been reloaded
         */
        void onInstalledModulesReloaded(ModuleUtil moduleUtil);
    }

    public class InstalledModule {
        //private static final int FLAG_FORWARD_LOCK = 1 << 29;
        public final String packageName;
        public final String versionName;
        public final long versionCode;
        public final int minVersion;
        public final long installTime;
        public final long updateTime;
        final boolean isFramework;
        public ApplicationInfo app;
        private String appName; // loaded lazyily
        private String description; // loaded lazyily

        private Drawable.ConstantState iconCache = null;

        private InstalledModule(PackageInfo pkg, boolean isFramework) {
            this.app = pkg.applicationInfo;
            this.packageName = pkg.packageName;
            this.isFramework = isFramework;
            this.versionName = pkg.versionName;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                this.versionCode = pkg.versionCode;
            } else {
                this.versionCode = pkg.getLongVersionCode();
            }
            this.installTime = pkg.firstInstallTime;
            this.updateTime = pkg.lastUpdateTime;

            if (isFramework) {
                this.minVersion = 0;
                this.description = "";
            } else {
                int version = XposedApp.getXposedVersion();
                if (version > 0 && XposedApp.getPreferences().getBoolean("skip_xposedminversion_check", false)) {
                    this.minVersion = version;
                } else {
                    Object minVersionRaw = app.metaData.get("xposedminversion");
                    if (minVersionRaw instanceof Integer) {
                        this.minVersion = (Integer) minVersionRaw;
                    } else if (minVersionRaw instanceof String) {
                        this.minVersion = extractIntPart((String) minVersionRaw);
                    } else {
                        this.minVersion = 0;
                    }
                }
            }
        }

        public boolean isInstalledOnExternalStorage() {
            return (app.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
        }

        public String getAppName() {
            if (appName == null)
                appName = app.loadLabel(pm).toString();
            return appName;
        }

        public String getDescription() {
            if (this.description == null) {
                Object descriptionRaw = app.metaData.get("xposeddescription");
                String descriptionTmp = null;
                if (descriptionRaw instanceof String) {
                    descriptionTmp = ((String) descriptionRaw).trim();
                } else if (descriptionRaw instanceof Integer) {
                    try {
                        int resId = (Integer) descriptionRaw;
                        if (resId != 0)
                            descriptionTmp = pm.getResourcesForApplication(app).getString(resId).trim();
                    } catch (Exception ignored) {
                    }
                }
                this.description = (descriptionTmp != null) ? descriptionTmp : "";
            }
            return this.description;
        }

        public boolean isUpdate(ModuleVersion version) {
            return (version != null) && version.code > versionCode;
        }

        public Bitmap getIcon(Context context) {
            return XposedApp.getInstance().getAppIconLoader().loadIcon(app, false);
        }

        @NonNull
        @Override
        public String toString() {
            return getAppName();
        }
    }
}