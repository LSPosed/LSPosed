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

package io.github.lsposed.manager.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.ConfigManager;
import io.github.lsposed.manager.repo.RepoLoader;
import io.github.lsposed.manager.repo.model.OnlineModule;

public final class ModuleUtil {
    // xposedminversion below this
    public static int MIN_MODULE_VERSION = 2; // reject modules with
    private static ModuleUtil instance = null;
    private final PackageManager pm;
    private final List<ModuleListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> enabledModules;
    private Map<String, InstalledModule> installedModules;
    private boolean isReloading = false;

    private ModuleUtil() {
        pm = App.getInstance().getPackageManager();
        enabledModules = new ArrayList<>(Arrays.asList(ConfigManager.getEnabledModules()));
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

    public void reloadInstalledModules() {
        synchronized (this) {
            if (isReloading)
                return;
            isReloading = true;
        }

        Map<String, InstalledModule> modules = new HashMap<>();
        for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            ApplicationInfo app = pkg.applicationInfo;
            if (!app.enabled)
                continue;

            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                InstalledModule installed = new InstalledModule(pkg, false);
                modules.put(pkg.packageName, installed);
            }
        }


        installedModules = modules;
        synchronized (this) {
            isReloading = false;
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

    public boolean setModuleEnabled(String packageName, boolean enabled) {
        if (!ConfigManager.setModuleEnabled(packageName, enabled)) {
            return false;
        }
        if (enabled) {
            if (!enabledModules.contains(packageName)) {
                enabledModules.add(packageName);
            }
        } else {
            enabledModules.remove(packageName);
        }
        return true;
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

    public synchronized void updateModulesList() {
        /*try {
            Log.i(App.TAG, "ModuleUtil -> updating modules.list");
            PrintWriter modulesList = new PrintWriter(Constants.getModulesListFile());
            PrintWriter enabledModulesList = new PrintWriter(Constants.getEnabledModulesListFile());
            List<InstalledModule> enabledModules = getEnabledModules();
            for (InstalledModule module : enabledModules) {
                modulesList.println(module.app.sourceDir);
                enabledModulesList.println(module.app.packageName);
            }
            modulesList.close();
            enabledModulesList.close();
        } catch (IOException e) {
            Log.e(App.TAG, "ModuleUtil -> cannot write " + Constants.getModulesListFile(), e);
        }*/
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
        private List<String> scopeList; // loaded lazyily

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

        public List<String> getScopeList() {
            if (scopeList == null) {
                try {
                    int scopeListResourceId = app.metaData.getInt("xposedscope");
                    if (scopeListResourceId != 0) {
                        scopeList = Arrays.asList(pm.getResourcesForApplication(app).getStringArray(scopeListResourceId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RepoLoader repoLoader = RepoLoader.getInstance();
                if (scopeList == null && repoLoader.isRepoLoaded()) {
                    OnlineModule module = repoLoader.getOnlineModule(packageName);
                    if (module != null && module.getScope() != null) {
                        scopeList = module.getScope();
                    }
                }
            }
            return scopeList;
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