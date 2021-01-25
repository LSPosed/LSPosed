package org.meowcat.edxposed.manager.repo;

import android.database.Cursor;
import android.provider.BaseColumns;

public class RepoDbDefinitions {
    static final int DATABASE_VERSION = 4;
    static final String DATABASE_NAME = "repo_cache.db";
    static final String SQL_CREATE_TABLE_REPOSITORIES = "CREATE TABLE "
            + RepositoriesColumns.TABLE_NAME + " (" + RepositoriesColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + RepositoriesColumns.URL
            + " TEXT NOT NULL, " + RepositoriesColumns.TITLE + " TEXT, "
            + RepositoriesColumns.PARTIAL_URL + " TEXT, "
            + RepositoriesColumns.VERSION + " TEXT, " + "UNIQUE ("
            + RepositoriesColumns.URL + ") ON CONFLICT REPLACE)";
    static final String SQL_CREATE_TABLE_MODULES = "CREATE TABLE "
            + ModulesColumns.TABLE_NAME + " (" + ModulesColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            ModulesColumns.REPO_ID + " INTEGER NOT NULL REFERENCES "
            + RepositoriesColumns.TABLE_NAME + " ON DELETE CASCADE, "
            + ModulesColumns.PKGNAME + " TEXT NOT NULL, " + ModulesColumns.TITLE
            + " TEXT NOT NULL, " + ModulesColumns.SUMMARY + " TEXT, "
            + ModulesColumns.DESCRIPTION + " TEXT, "
            + ModulesColumns.DESCRIPTION_IS_HTML + " INTEGER DEFAULT 0, "
            + ModulesColumns.AUTHOR + " TEXT, " + ModulesColumns.SUPPORT
            + " TEXT, " + ModulesColumns.CREATED + " INTEGER DEFAULT -1, "
            + ModulesColumns.UPDATED + " INTEGER DEFAULT -1, "
            + ModulesColumns.PREFERRED + " INTEGER DEFAULT 1, "
            + ModulesColumns.LATEST_VERSION + " INTEGER REFERENCES "
            + ModuleVersionsColumns.TABLE_NAME + ", " + "UNIQUE ("
            + ModulesColumns.PKGNAME + ", " + ModulesColumns.REPO_ID
            + ") ON CONFLICT REPLACE)";
    static final String SQL_CREATE_TABLE_MODULE_VERSIONS = "CREATE TABLE "
            + ModuleVersionsColumns.TABLE_NAME + " ("
            + ModuleVersionsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ModuleVersionsColumns.MODULE_ID + " INTEGER NOT NULL REFERENCES "
            + ModulesColumns.TABLE_NAME + " ON DELETE CASCADE, "
            + ModuleVersionsColumns.NAME + " TEXT NOT NULL, "
            + ModuleVersionsColumns.CODE + " INTEGER NOT NULL, "
            + ModuleVersionsColumns.DOWNLOAD_LINK + " TEXT, "
            + ModuleVersionsColumns.MD5SUM + " TEXT, "
            + ModuleVersionsColumns.CHANGELOG + " TEXT, "
            + ModuleVersionsColumns.CHANGELOG_IS_HTML + " INTEGER DEFAULT 0, "
            + ModuleVersionsColumns.RELTYPE + " INTEGER DEFAULT 0, "
            + ModuleVersionsColumns.UPLOADED + " INTEGER DEFAULT -1)";
    static final String SQL_CREATE_INDEX_MODULE_VERSIONS_MODULE_ID = "CREATE INDEX "
            + ModuleVersionsColumns.IDX_MODULE_ID + " ON "
            + ModuleVersionsColumns.TABLE_NAME + " ("
            + ModuleVersionsColumns.MODULE_ID + ")";
    static final String SQL_CREATE_TABLE_MORE_INFO = "CREATE TABLE "
            + MoreInfoColumns.TABLE_NAME + " (" + MoreInfoColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + MoreInfoColumns.MODULE_ID
            + " INTEGER NOT NULL REFERENCES " + ModulesColumns.TABLE_NAME
            + " ON DELETE CASCADE, " + MoreInfoColumns.LABEL
            + " TEXT NOT NULL, " + MoreInfoColumns.VALUE + " TEXT)";
    static final String SQL_CREATE_TEMP_TABLE_INSTALLED_MODULES = "CREATE TEMP TABLE "
            + InstalledModulesColumns.TABLE_NAME + " ("
            + InstalledModulesColumns.PKGNAME
            + " TEXT PRIMARY KEY ON CONFLICT REPLACE, "
            + InstalledModulesColumns.VERSION_CODE + " INTEGER NOT NULL, "
            + InstalledModulesColumns.VERSION_NAME + " TEXT)";
    static final String SQL_CREATE_TEMP_VIEW_INSTALLED_MODULES_UPDATES = "CREATE TEMP VIEW "
            + InstalledModulesUpdatesColumns.VIEW_NAME + " AS SELECT " + "m."
            + ModulesColumns._ID + " AS "
            + InstalledModulesUpdatesColumns.MODULE_ID + ", " + "i."
            + InstalledModulesColumns.PKGNAME + " AS "
            + InstalledModulesUpdatesColumns.PKGNAME + ", " + "i."
            + InstalledModulesColumns.VERSION_CODE + " AS "
            + InstalledModulesUpdatesColumns.INSTALLED_CODE + ", " + "i."
            + InstalledModulesColumns.VERSION_NAME + " AS "
            + InstalledModulesUpdatesColumns.INSTALLED_NAME + ", " + "v."
            + ModuleVersionsColumns._ID + " AS "
            + InstalledModulesUpdatesColumns.LATEST_ID + ", " + "v."
            + ModuleVersionsColumns.CODE + " AS "
            + InstalledModulesUpdatesColumns.LATEST_CODE + ", " + "v."
            + ModuleVersionsColumns.NAME + " AS "
            + InstalledModulesUpdatesColumns.LATEST_NAME + " FROM "
            + InstalledModulesColumns.TABLE_NAME + " AS i" + " INNER JOIN "
            + ModulesColumns.TABLE_NAME + " AS m" + " ON m."
            + ModulesColumns.PKGNAME + " = i." + InstalledModulesColumns.PKGNAME
            + " INNER JOIN " + ModuleVersionsColumns.TABLE_NAME + " AS v"
            + " ON v." + ModuleVersionsColumns._ID + " = m."
            + ModulesColumns.LATEST_VERSION + " WHERE "
            + InstalledModulesUpdatesColumns.LATEST_CODE + " > "
            + InstalledModulesUpdatesColumns.INSTALLED_CODE + " AND "
            + ModulesColumns.PREFERRED + " = 1";

    //////////////////////////////////////////////////////////////////////////
    public interface RepositoriesColumns extends BaseColumns {
        String TABLE_NAME = "repositories";

        String URL = "url";
        String TITLE = "title";
        String PARTIAL_URL = "partial_url";
        String VERSION = "version";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface ModulesColumns extends BaseColumns {
        String TABLE_NAME = "modules";

        String REPO_ID = "repo_id";
        String PKGNAME = "pkgname";
        String TITLE = "title";
        String SUMMARY = "summary";
        String DESCRIPTION = "description";
        String DESCRIPTION_IS_HTML = "description_is_html";
        String AUTHOR = "author";
        String SUPPORT = "support";
        String CREATED = "created";
        String UPDATED = "updated";

        String PREFERRED = "preferred";
        String LATEST_VERSION = "latest_version_id";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface ModuleVersionsColumns extends BaseColumns {
        String TABLE_NAME = "module_versions";
        String IDX_MODULE_ID = "module_versions_module_id_idx";

        String MODULE_ID = "module_id";
        String NAME = "name";
        String CODE = "code";
        String DOWNLOAD_LINK = "download_link";
        String MD5SUM = "md5sum";
        String CHANGELOG = "changelog";
        String CHANGELOG_IS_HTML = "changelog_is_html";
        String RELTYPE = "reltype";
        String UPLOADED = "uploaded";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface MoreInfoColumns extends BaseColumns {
        String TABLE_NAME = "more_info";

        String MODULE_ID = "module_id";
        String LABEL = "label";
        String VALUE = "value";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface InstalledModulesColumns {
        String TABLE_NAME = "installed_modules";

        String PKGNAME = "pkgname";
        String VERSION_CODE = "version_code";
        String VERSION_NAME = "version_name";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface InstalledModulesUpdatesColumns {
        String VIEW_NAME = InstalledModulesColumns.TABLE_NAME + "_updates";

        String MODULE_ID = "module_id";
        String PKGNAME = "pkgname";
        String INSTALLED_CODE = "installed_code";
        String INSTALLED_NAME = "installed_name";
        String LATEST_ID = "latest_id";
        String LATEST_CODE = "latest_code";
        String LATEST_NAME = "latest_name";
    }

    //////////////////////////////////////////////////////////////////////////
    public interface OverviewColumns extends BaseColumns {
        String PKGNAME = ModulesColumns.PKGNAME;
        String TITLE = ModulesColumns.TITLE;
        String SUMMARY = ModulesColumns.SUMMARY;
        String CREATED = ModulesColumns.CREATED;
        String UPDATED = ModulesColumns.UPDATED;

        String INSTALLED_VERSION = "installed_version";
        String LATEST_VERSION = "latest_version";

        String IS_FRAMEWORK = "is_framework";
        String IS_INSTALLED = "is_installed";
        String HAS_UPDATE = "has_update";
    }

    public static class OverviewColumnsIndexes {
        public static int PKGNAME = -1;
        public static int TITLE = -1;
        public static int SUMMARY = -1;
        public static int CREATED = -1;
        public static int UPDATED = -1;
        public static int INSTALLED_VERSION = -1;
        public static int LATEST_VERSION = -1;
        public static int IS_FRAMEWORK = -1;
        public static int IS_INSTALLED = -1;
        public static int HAS_UPDATE = -1;
        private static boolean isFilled = false;

        private OverviewColumnsIndexes() {
        }

        static void fillFromCursor(Cursor cursor) {
            if (isFilled || cursor == null)
                return;

            PKGNAME = cursor.getColumnIndexOrThrow(OverviewColumns.PKGNAME);
            TITLE = cursor.getColumnIndexOrThrow(OverviewColumns.TITLE);
            SUMMARY = cursor.getColumnIndexOrThrow(OverviewColumns.SUMMARY);
            CREATED = cursor.getColumnIndexOrThrow(OverviewColumns.CREATED);
            UPDATED = cursor.getColumnIndexOrThrow(OverviewColumns.UPDATED);
            INSTALLED_VERSION = cursor.getColumnIndexOrThrow(OverviewColumns.INSTALLED_VERSION);
            LATEST_VERSION = cursor.getColumnIndexOrThrow(OverviewColumns.LATEST_VERSION);
            INSTALLED_VERSION = cursor.getColumnIndexOrThrow(OverviewColumns.INSTALLED_VERSION);
            IS_FRAMEWORK = cursor.getColumnIndexOrThrow(OverviewColumns.IS_FRAMEWORK);
            IS_INSTALLED = cursor.getColumnIndexOrThrow(OverviewColumns.IS_INSTALLED);
            HAS_UPDATE = cursor.getColumnIndexOrThrow(OverviewColumns.HAS_UPDATE);

            isFilled = true;
        }
    }
}