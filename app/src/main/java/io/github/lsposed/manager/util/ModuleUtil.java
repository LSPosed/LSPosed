package io.github.lsposed.manager.util;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.AppHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModuleUtil {
    // xposedminversion below this
    public static int MIN_MODULE_VERSION = 2; // reject modules with
    private static ModuleUtil instance = null;
    private final PackageManager pm;
    private final List<ModuleListener> listeners = new CopyOnWriteArrayList<>();
    //private InstalledModule framework = null;
    private Map<String, InstalledModule> installedModules;
    private final List<String> enabledModules;
    private boolean isReloading = false;
    private Toast toast;
    private final SharedPreferences prefs;

    private ModuleUtil() {
        pm = App.getInstance().getPackageManager();
        enabledModules = AppHelper.getEnabledModuleList();
        prefs = App.getPreferences();
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
        AppHelper.forceWhiteList.clear();

        for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            ApplicationInfo app = pkg.applicationInfo;
            if (!app.enabled)
                continue;

            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                InstalledModule installed = new InstalledModule(pkg, false);
                modules.put(pkg.packageName, installed);
                AppHelper.forceWhiteList.add(pkg.packageName);
            }
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
            installedModules.put(packageName, module);
            for (ModuleListener listener : listeners) {
                listener.onSingleInstalledModuleReloaded(instance, packageName,
                        module);
            }
            return module;
        } else {
            InstalledModule old = installedModules.remove(packageName);
            if (old != null) {
                for (ModuleListener listener : listeners) {
                    listener.onSingleInstalledModuleReloaded(instance, packageName, null);
                }
            }
            return null;
        }
    }

    public InstalledModule getModule(String packageName) {
        return installedModules.get(packageName);
    }

    public Map<String, InstalledModule> getModules() {
        return installedModules;
    }

    public void setModuleEnabled(String packageName, boolean enabled) {
        if (enabled) {
            if (!enabledModules.contains(packageName)) {
                enabledModules.add(packageName);
            }
        } else {
            enabledModules.remove(packageName);
        }
    }

    public boolean isModuleEnabled(String packageName) {
        return enabledModules.contains(packageName);
    }

    public List<InstalledModule> getEnabledModules() {
        LinkedList<InstalledModule> result = new LinkedList<>();
        Iterator<String> iterator = enabledModules.iterator();
        while (iterator.hasNext()) {
            InstalledModule module = getModule(iterator.next());
            if (module != null) {
                result.add(module);
            } else {
                iterator.remove();
            }
        }
        return result;
    }

    public synchronized void updateModulesList(boolean showToast) {
        updateModulesList(showToast, null);
    }

    public synchronized void updateModulesList(boolean showToast, View view) {
        try {
            Log.i(App.TAG, "ModuleUtil -> updating modules.list");
            int installedXposedVersion = Constants.getXposedApiVersion();
            if (!prefs.getBoolean("skip_xposedminversion_check", false) && installedXposedVersion <= 0 && showToast) {
                showToast(view, R.string.notinstalled);
                return;
            }

            PrintWriter modulesList = new PrintWriter(Constants.getModulesListFile());
            PrintWriter enabledModulesList = new PrintWriter(Constants.getEnabledModulesListFile());
            List<InstalledModule> enabledModules = getEnabledModules();
            for (InstalledModule module : enabledModules) {

                if (!prefs.getBoolean("skip_xposedminversion_check", false) && (module.minVersion > installedXposedVersion || module.minVersion < MIN_MODULE_VERSION) && showToast) {
                    showToast(view, R.string.notinstalled);
                    continue;
                }

                modulesList.println(module.app.sourceDir);
                enabledModulesList.println(module.app.packageName);
            }
            modulesList.close();
            enabledModulesList.close();

            if (showToast) {
                showToast(view, R.string.xposed_module_list_updated);
            }
        } catch (IOException e) {
            Log.e(App.TAG, "ModuleUtil -> cannot write " + Constants.getModulesListFile(), e);
            showToast(view, "cannot write " + Constants.getModulesListFile() + e);
        }
    }

    private void showToast(View view, int message) {
        showToast(view, App.getInstance().getString(message));
    }

    private void showToast(View view, String message) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            if (toast != null) {
                toast.cancel();
                toast = null;
            }
            toast = Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT);
            toast.show();
        }
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
        public PackageInfo pkg;
        private String appName; // loaded lazyily
        private String description; // loaded lazyily

        private InstalledModule(PackageInfo pkg, boolean isFramework) {
            this.app = pkg.applicationInfo;
            this.pkg = pkg;
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
                int version = Constants.getXposedApiVersion();
                if (version > 0 && prefs.getBoolean("skip_xposedminversion_check", false)) {
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

        public PackageInfo getPackageInfo() {
            return pkg;
        }

        @NonNull
        @Override
        public String toString() {
            return getAppName();
        }
    }
}