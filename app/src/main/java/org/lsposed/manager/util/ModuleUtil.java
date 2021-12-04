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

package org.lsposed.manager.util;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.repo.model.OnlineModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleUtil {
    // xposedminversion below this
    public static int MIN_MODULE_VERSION = 2; // reject modules with
    private static ModuleUtil instance = null;
    private final PackageManager pm;
    private final Set<ModuleListener> listeners = ConcurrentHashMap.newKeySet();
    private HashSet<String> enabledModules = new HashSet<>();
    private List<UserInfo> users = new ArrayList<>();
    private Map<Pair<String, Integer>, InstalledModule> installedModules = new HashMap<>();
    private boolean modulesLoaded = false;

    private ModuleUtil() {
        pm = App.getInstance().getPackageManager();
    }

    public boolean isModulesLoaded() {
        return modulesLoaded;
    }

    public static synchronized ModuleUtil getInstance() {
        if (instance == null) {
            instance = new ModuleUtil();
            App.getExecutorService().submit(instance::reloadInstalledModules);
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

    synchronized public void reloadInstalledModules() {
        modulesLoaded = false;
        if (!ConfigManager.isBinderAlive()) {
            modulesLoaded = true;
            return;
        }

        Map<Pair<String, Integer>, InstalledModule> modules = new HashMap<>();
        var users = ConfigManager.getUsers();
        for (PackageInfo pkg : ConfigManager.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA, false)) {
            ApplicationInfo app = pkg.applicationInfo;

            if (app.metaData != null && app.metaData.containsKey("xposedminversion")) {
                InstalledModule installed = new InstalledModule(pkg);
                modules.put(Pair.create(pkg.packageName, app.uid / 100000), installed);
            }
        }

        installedModules = modules;

        this.users = users;

        enabledModules = new HashSet<>(Arrays.asList(ConfigManager.getEnabledModules()));
        modulesLoaded = true;
        listeners.forEach(ModuleListener::onModulesReloaded);
    }

    @Nullable
    public List<UserInfo> getUsers() {
        return modulesLoaded ? users : null;
    }

    public InstalledModule reloadSingleModule(String packageName, int userId) {
        return reloadSingleModule(packageName, userId, false);
    }

    public InstalledModule reloadSingleModule(String packageName, int userId, boolean packageFullyRemoved) {
        if (packageFullyRemoved && isModuleEnabled(packageName)) {
            enabledModules.remove(packageName);
            listeners.forEach(ModuleListener::onModulesReloaded);
        }
        PackageInfo pkg;

        try {
            pkg = ConfigManager.getPackageInfo(packageName, PackageManager.GET_META_DATA, userId);
        } catch (NameNotFoundException e) {
            InstalledModule old = installedModules.remove(Pair.create(packageName, userId));
            if (old != null) listeners.forEach(i -> i.onSingleModuleReloaded(old));
            return null;
        }

        ApplicationInfo app = pkg.applicationInfo;
        if (app.metaData != null && app.metaData.containsKey("xposedminversion")) {
            InstalledModule module = new InstalledModule(pkg);
            installedModules.put(Pair.create(packageName, userId), module);
            listeners.forEach(i -> i.onSingleModuleReloaded(module));
            return module;
        } else {
            InstalledModule old = installedModules.remove(Pair.create(packageName, userId));
            if (old != null) listeners.forEach(i -> i.onSingleModuleReloaded(old));
            return null;
        }
    }

    @Nullable
    public InstalledModule getModule(String packageName, int userId) {
        return modulesLoaded ? installedModules.get(Pair.create(packageName, userId)) : null;
    }

    @Nullable
    public InstalledModule getModule(String packageName) {
        return getModule(packageName, 0);
    }

    @Nullable
    synchronized public Map<Pair<String, Integer>, InstalledModule> getModules() {
        return modulesLoaded ? installedModules : null;
    }

    public boolean setModuleEnabled(String packageName, boolean enabled) {
        if (!ConfigManager.setModuleEnabled(packageName, enabled)) {
            return false;
        }
        if (enabled) {
            enabledModules.add(packageName);
        } else {
            enabledModules.remove(packageName);
        }
        return true;
    }

    public boolean isModuleEnabled(String packageName) {
        return enabledModules.contains(packageName);
    }

    public int getEnabledModulesCount() {
        return modulesLoaded ? enabledModules.size() : -1;
    }

    public void addListener(ModuleListener listener) {
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
        default void onSingleModuleReloaded(InstalledModule module) {

        }

        default void onModulesReloaded() {

        }
    }

    public class InstalledModule {
        //private static final int FLAG_FORWARD_LOCK = 1 << 29;
        public final int userId;
        public final String packageName;
        public final String versionName;
        public final long versionCode;
        public final int minVersion;
        public final long installTime;
        public final long updateTime;
        public ApplicationInfo app;
        public PackageInfo pkg;
        private String appName; // loaded lazyily
        private String description; // loaded lazyily
        private List<String> scopeList; // loaded lazyily

        private InstalledModule(PackageInfo pkg) {
            this.app = pkg.applicationInfo;
            this.pkg = pkg;
            this.userId = pkg.applicationInfo.uid / 100000;
            this.packageName = pkg.packageName;
            this.versionName = pkg.versionName;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                this.versionCode = pkg.versionCode;
            } else {
                this.versionCode = pkg.getLongVersionCode();
            }
            this.installTime = pkg.firstInstallTime;
            this.updateTime = pkg.lastUpdateTime;

            Object minVersionRaw = app.metaData.get("xposedminversion");
            if (minVersionRaw instanceof Integer) {
                this.minVersion = (Integer) minVersionRaw;
            } else if (minVersionRaw instanceof String) {
                this.minVersion = extractIntPart((String) minVersionRaw);
            } else {
                this.minVersion = 0;
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
                    } else {
                        String scopeListString = app.metaData.getString("xposedscope");
                        if (scopeListString != null)
                            scopeList = Arrays.asList(scopeListString.split(";"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RepoLoader repoLoader = RepoLoader.getInstance();
                if (scopeList == null) {
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
