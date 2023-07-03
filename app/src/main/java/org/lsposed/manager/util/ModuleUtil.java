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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.repo.model.OnlineModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

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

    static final int MATCH_ANY_USER = 0x00400000; // PackageManager.MATCH_ANY_USER

    static final int MATCH_ALL_FLAGS = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE | PackageManager.MATCH_UNINSTALLED_PACKAGES | MATCH_ANY_USER;

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

    public static ZipFile getModernModuleApk(ApplicationInfo info) {
        String[] apks;
        if (info.splitSourceDirs != null) {
            apks = Arrays.copyOf(info.splitSourceDirs, info.splitSourceDirs.length + 1);
            apks[info.splitSourceDirs.length] = info.sourceDir;
        } else apks = new String[]{info.sourceDir};
        ZipFile zip = null;
        for (var apk : apks) {
            try {
                zip = new ZipFile(apk);
                if (zip.getEntry("META-INF/xposed/java_init.list") != null) {
                    return zip;
                }
                zip.close();
                zip = null;
            } catch (IOException ignored) {
            }
        }
        return zip;
    }

    public static boolean isLegacyModule(ApplicationInfo info) {
        return info.metaData != null && info.metaData.containsKey("xposedminversion");
    }

    synchronized public void reloadInstalledModules() {
        modulesLoaded = false;
        if (!ConfigManager.isBinderAlive()) {
            modulesLoaded = true;
            return;
        }

        Map<Pair<String, Integer>, InstalledModule> modules = new HashMap<>();
        var users = ConfigManager.getUsers();
        for (PackageInfo pkg : ConfigManager.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA | MATCH_ALL_FLAGS, false)) {
            ApplicationInfo app = pkg.applicationInfo;

            var modernApk = getModernModuleApk(app);
            if (modernApk != null || isLegacyModule(app)) {
                modules.computeIfAbsent(Pair.create(pkg.packageName, app.uid / App.PER_USER_RANGE), k -> new InstalledModule(pkg, modernApk));
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
        var modernApk = getModernModuleApk(app);
        if (modernApk != null || isLegacyModule(app)) {
            InstalledModule module = new InstalledModule(pkg, modernApk);
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
        public final boolean legacy;
        public final int minVersion;
        public final int targetVersion;
        public final boolean staticScope;
        public final long installTime;
        public final long updateTime;
        public final ApplicationInfo app;
        public final PackageInfo pkg;
        private String appName; // loaded lazily
        private String description; // loaded lazily
        private List<String> scopeList; // loaded lazily

        private InstalledModule(PackageInfo pkg, ZipFile modernModuleApk) {
            app = pkg.applicationInfo;
            this.pkg = pkg;
            userId = pkg.applicationInfo.uid / App.PER_USER_RANGE;
            packageName = pkg.packageName;
            versionName = pkg.versionName;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                versionCode = pkg.versionCode;
            } else {
                versionCode = pkg.getLongVersionCode();
            }
            installTime = pkg.firstInstallTime;
            updateTime = pkg.lastUpdateTime;
            legacy = modernModuleApk == null;

            if (legacy) {
                Object minVersionRaw = app.metaData.get("xposedminversion");
                if (minVersionRaw instanceof Integer) {
                    minVersion = (Integer) minVersionRaw;
                } else if (minVersionRaw instanceof String) {
                    minVersion = extractIntPart((String) minVersionRaw);
                } else {
                    minVersion = 0;
                }
                targetVersion = minVersion; // legacy modules don't have a target version
                staticScope = false;
            } else {
                int minVersion = 100;
                int targetVersion = 100;
                boolean staticScope = false;
                try (modernModuleApk) {
                    var propEntry = modernModuleApk.getEntry("META-INF/xposed/module.prop");
                    if (propEntry != null) {
                        var prop = new Properties();
                        prop.load(modernModuleApk.getInputStream(propEntry));
                        minVersion = extractIntPart(prop.getProperty("minApiVersion"));
                        targetVersion = extractIntPart(prop.getProperty("targetApiVersion"));
                        staticScope = TextUtils.equals(prop.getProperty("staticScope"), "true");
                    }
                    var scopeEntry = modernModuleApk.getEntry("META-INF/xposed/scope.list");
                    if (scopeEntry != null) {
                        try (var reader = new BufferedReader(new InputStreamReader(modernModuleApk.getInputStream(scopeEntry)))) {
                            scopeList = reader.lines().collect(Collectors.toList());
                        }
                    } else {
                        scopeList = Collections.emptyList();
                    }
                } catch (IOException | OutOfMemoryError e) {
                    Log.e(App.TAG, "Error while closing modern module APK", e);
                }
                this.minVersion = minVersion;
                this.targetVersion = targetVersion;
                this.staticScope = staticScope;
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
            if (this.description != null) return this.description;
            String descriptionTmp = "";
            if (legacy) {
                Object descriptionRaw = app.metaData.get("xposeddescription");
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
            } else {
                var des = app.loadDescription(pm);
                if (des != null) descriptionTmp = des.toString();
            }
            this.description = descriptionTmp;
            return this.description;
        }

        public List<String> getScopeList() {
            if (scopeList != null) return scopeList;
            List<String> list = null;
            try {
                int scopeListResourceId = app.metaData.getInt("xposedscope");
                if (scopeListResourceId != 0) {
                    list = Arrays.asList(pm.getResourcesForApplication(app).getStringArray(scopeListResourceId));
                } else {
                    String scopeListString = app.metaData.getString("xposedscope");
                    if (scopeListString != null)
                        list = Arrays.asList(scopeListString.split(";"));
                }
            } catch (Exception ignored) {
            }
            if (list == null) {
                OnlineModule module = RepoLoader.getInstance().getOnlineModule(packageName);
                if (module != null && module.getScope() != null) {
                    list = module.getScope();
                }
            }
            if (list != null) {
                //For historical reasons, legacy modules use the opposite name.
                //https://github.com/rovo89/XposedBridge/commit/6b49688c929a7768f3113b4c65b429c7a7032afa
                list.replaceAll(s ->
                    switch (s) {
                        case "android" -> "system";
                        case "system" -> "android";
                        default -> s;
                    }
                );
                scopeList = list;
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
