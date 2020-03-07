package org.meowcat.edxposed.manager.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.DownloadActivity;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.repo.ReleaseType;
import org.meowcat.edxposed.manager.repo.RepoDb;
import org.meowcat.edxposed.manager.repo.RepoParser;
import org.meowcat.edxposed.manager.repo.RepoParser.RepoParserCallback;
import org.meowcat.edxposed.manager.repo.Repository;
import org.meowcat.edxposed.manager.util.DownloadsUtil.SyncDownloadInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

public class RepoLoader {
    private static final int UPDATE_FREQUENCY = 24 * 60 * 60 * 1000;
    private static String DEFAULT_REPOSITORIES;
    private static RepoLoader instance = null;
    private final List<RepoListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, ReleaseType> localReleaseTypesCache = new HashMap<>();
    private XposedApp app;
    private SharedPreferences pref;
    private SharedPreferences modulePref;
    private ConnectivityManager conMgr;
    private boolean isLoading = false;
    private boolean reloadTriggeredOnce = false;
    private Map<Long, Repository> repositories = null;
    private ReleaseType globalReleaseType;
    private SwipeRefreshLayout swipeRefreshLayout;

    private RepoLoader() {
        instance = this;
        app = XposedApp.getInstance();
        pref = app.getSharedPreferences("repo", Context.MODE_PRIVATE);
        DEFAULT_REPOSITORIES = XposedApp.getPreferences().getBoolean("custom_list", false) ? "https://cdn.jsdelivr.net/gh/ElderDrivers/Repository-Website@gh-pages/assets/full.xml.gz" : "https://dl-xda.xposed.info/repo/full.xml.gz";
        modulePref = app.getSharedPreferences("module_settings", Context.MODE_PRIVATE);
        conMgr = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        globalReleaseType = ReleaseType.fromString(XposedApp.getPreferences().getString("release_type_global", "stable"));
        refreshRepositories();
    }

    public static synchronized RepoLoader getInstance() {
        if (instance == null)
            new RepoLoader();
        return instance;
    }

    private boolean refreshRepositories() {
        repositories = RepoDb.getRepositories();

        // Unlikely case (usually only during initial load): DB state doesn't
        // fit to configuration
        boolean needReload = false;
        String[] config = (pref.getString("repositories", DEFAULT_REPOSITORIES) + "").split("\\|");
        if (repositories.size() != config.length) {
            needReload = true;
        } else {
            int i = 0;
            for (Repository repo : repositories.values()) {
                if (!repo.url.equals(config[i++])) {
                    needReload = true;
                    break;
                }
            }
        }

        if (!needReload)
            return false;

        clear(false);
        for (String url : config) {
            RepoDb.insertRepository(url);
        }
        repositories = RepoDb.getRepositories();
        return true;
    }

    public void setReleaseTypeGlobal(String relTypeString) {
        ReleaseType relType = ReleaseType.fromString(relTypeString);
        if (globalReleaseType == relType)
            return;

        globalReleaseType = relType;

        // Updating the latest version for all modules takes a moment
        new Thread("DBUpdate") {
            @Override
            public void run() {
                RepoDb.updateAllModulesLatestVersion();
                notifyListeners();
            }
        }.start();
    }

    public void setReleaseTypeLocal(String packageName, String relTypeString) {
        ReleaseType relType = (!TextUtils.isEmpty(relTypeString)) ? ReleaseType.fromString(relTypeString) : null;

        if (getReleaseTypeLocal(packageName) == relType)
            return;

        synchronized (localReleaseTypesCache) {
            if (relType != null) {
                localReleaseTypesCache.put(packageName, relType);
            }
        }

        RepoDb.updateModuleLatestVersion(packageName);
        notifyListeners();
    }

    private ReleaseType getReleaseTypeLocal(String packageName) {
        synchronized (localReleaseTypesCache) {
            if (localReleaseTypesCache.containsKey(packageName))
                return localReleaseTypesCache.get(packageName);

            String value = modulePref.getString(packageName + "_release_type",
                    null);
            ReleaseType result = (!TextUtils.isEmpty(value)) ? ReleaseType.fromString(value) : null;
            if (result != null) {
                localReleaseTypesCache.put(packageName, result);
            }
            return result;
        }
    }

    public Repository getRepository(long repoId) {
        return repositories.get(repoId);
    }

    public Module getModule(String packageName) {
        return RepoDb.getModuleByPackageName(packageName);
    }

    public ModuleVersion getLatestVersion(Module module) {
        if (module == null || module.versions.isEmpty())
            return null;

        for (ModuleVersion version : module.versions) {
            if (version.downloadLink != null && isVersionShown(version))
                return version;
        }
        return null;
    }

    public boolean isVersionShown(ModuleVersion version) {
        return version.relType
                .ordinal() <= getMaxShownReleaseType(version.module.packageName).ordinal();
    }

    public ReleaseType getMaxShownReleaseType(String packageName) {
        ReleaseType localSetting = getReleaseTypeLocal(packageName);
        if (localSetting != null)
            return localSetting;
        else
            return globalReleaseType;
    }

    public void triggerReload(final boolean force) {
        reloadTriggeredOnce = true;

        if (force) {
            resetLastUpdateCheck();
        } else {
            long lastUpdateCheck = pref.getLong("last_update_check", 0);
            if (System.currentTimeMillis() < lastUpdateCheck + UPDATE_FREQUENCY)
                return;
        }

        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected())
            return;

        synchronized (this) {
            if (isLoading)
                return;
            isLoading = true;
        }
        app.updateProgressIndicator(swipeRefreshLayout);

        new Thread("RepositoryReload") {
            public void run() {
                final List<String> messages = new LinkedList<>();
                boolean hasChanged = downloadAndParseFiles(messages);

                pref.edit().putLong("last_update_check", System.currentTimeMillis()).apply();

                if (!messages.isEmpty()) {
                    XposedApp.runOnUiThread(() -> {
                        for (String message : messages) {
                            Toast.makeText(app, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                if (hasChanged)
                    notifyListeners();

                synchronized (this) {
                    isLoading = false;
                }
                app.updateProgressIndicator(swipeRefreshLayout);
            }
        }.start();
    }

    public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    public void triggerFirstLoadIfNecessary() {
        if (!reloadTriggeredOnce)
            triggerReload(false);
    }

    public void resetLastUpdateCheck() {
        pref.edit().remove("last_update_check").apply();
    }

    public synchronized boolean isLoading() {
        return isLoading;
    }

    public void clear(boolean notify) {
        synchronized (this) {
            // TODO Stop reloading repository when it should be cleared
            if (isLoading)
                return;

            RepoDb.deleteRepositories();
            repositories = new LinkedHashMap<>(0);
            DownloadsUtil.clearCache(null);
            resetLastUpdateCheck();
        }

        if (notify)
            notifyListeners();
    }

    public void setRepositories(String... repos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repos.length; i++) {
            if (i > 0)
                sb.append("|");
            sb.append(repos[i]);
        }
        pref.edit().putString("repositories", sb.toString()).apply();
        if (refreshRepositories())
            triggerReload(true);
    }

    public boolean hasModuleUpdates() {
        return RepoDb.hasModuleUpdates();
    }

    public String getFrameworkUpdateVersion() {
        return RepoDb.getFrameworkUpdateVersion();
    }

    private File getRepoCacheFile(String repo) {
        String filename = "repo_" + HashUtil.md5(repo) + ".xml";
        if (repo.endsWith(".gz"))
            filename += ".gz";
        return new File(app.getCacheDir(), filename);
    }

    private boolean downloadAndParseFiles(List<String> messages) {
        // These variables don't need to be atomic, just mutable
        final AtomicBoolean hasChanged = new AtomicBoolean(false);
        final AtomicInteger insertCounter = new AtomicInteger();
        final AtomicInteger deleteCounter = new AtomicInteger();

        for (Entry<Long, Repository> repoEntry : repositories.entrySet()) {
            final long repoId = repoEntry.getKey();
            final Repository repo = repoEntry.getValue();

            String url = (repo.partialUrl != null && repo.version != null) ? String.format(repo.partialUrl, repo.version) : repo.url;

            File cacheFile = getRepoCacheFile(url);
            SyncDownloadInfo info = DownloadsUtil.downloadSynchronously(url,
                    cacheFile);

            Log.i(XposedApp.TAG, String.format(
                    "RepoLoader -> Downloaded %s with status %d (error: %s), size %d bytes",
                    url, info.status, info.errorMessage, cacheFile.length()));

            if (info.status != SyncDownloadInfo.STATUS_SUCCESS) {
                if (info.errorMessage != null)
                    messages.add(info.errorMessage);
                continue;
            }

            InputStream in = null;
            RepoDb.beginTransation();
            try {
                in = new FileInputStream(cacheFile);
                if (url.endsWith(".gz"))
                    in = new GZIPInputStream(in);

                RepoParser.parse(in, new RepoParserCallback() {
                    @Override
                    public void onRepositoryMetadata(Repository repository) {
                        if (!repository.isPartial) {
                            RepoDb.deleteAllModules(repoId);
                            hasChanged.set(true);
                        }
                    }

                    @Override
                    public void onNewModule(Module module) {
                        RepoDb.insertModule(repoId, module);
                        hasChanged.set(true);
                        insertCounter.incrementAndGet();
                    }

                    @Override
                    public void onRemoveModule(String packageName) {
                        RepoDb.deleteModule(repoId, packageName);
                        hasChanged.set(true);
                        deleteCounter.decrementAndGet();
                    }

                    @Override
                    public void onCompleted(Repository repository) {
                        if (!repository.isPartial) {
                            RepoDb.updateRepository(repoId, repository);
                            repo.name = repository.name;
                            repo.partialUrl = repository.partialUrl;
                            repo.version = repository.version;
                        } else {
                            RepoDb.updateRepositoryVersion(repoId, repository.version);
                            repo.version = repository.version;
                        }

                        Log.i(XposedApp.TAG, String.format(
                                "RepoLoader -> Updated repository %s to version %s (%d new / %d removed modules)",
                                repo.url, repo.version, insertCounter.get(),
                                deleteCounter.get()));
                    }
                });

                RepoDb.setTransactionSuccessful();
            } catch (SQLiteException e) {
                XposedApp.runOnUiThread(() -> new MaterialAlertDialogBuilder(app)
                        .setTitle(R.string.restart_needed)
                        .setMessage(R.string.cache_cleaned)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent i = new Intent(app, DownloadActivity.class);
                            PendingIntent pi = PendingIntent.getActivity(app, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
                            System.exit(0);
                        })
                        .setCancelable(false)
                        .show());

                DownloadsUtil.clearCache(url);
            } catch (Throwable t) {
                Log.e(XposedApp.TAG, "RepoLoader -> Cannot load repository from " + url, t);
                messages.add(app.getString(R.string.repo_load_failed, url, t.getMessage()));
                DownloadsUtil.clearCache(url);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                //noinspection ResultOfMethodCallIgnored
                cacheFile.delete();
                RepoDb.endTransation();
            }
        }

        // TODO Set ModuleColumns.PREFERRED for modules which appear in multiple
        // repositories
        return hasChanged.get();
    }

    public void addListener(RepoListener listener, boolean triggerImmediately) {
        if (!listeners.contains(listener))
            listeners.add(listener);

        if (triggerImmediately)
            listener.onRepoReloaded(this);
    }

    public void removeListener(RepoListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (RepoListener listener : listeners) {
            listener.onRepoReloaded(instance);
        }
    }

    public interface RepoListener {
        /**
         * Called whenever the list of modules from repositories has been
         * successfully reloaded
         */
        void onRepoReloaded(RepoLoader loader);
    }
}
