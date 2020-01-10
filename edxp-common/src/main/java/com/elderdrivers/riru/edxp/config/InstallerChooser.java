package com.elderdrivers.riru.edxp.config;

import android.annotation.SuppressLint;
import android.os.Build;

import com.elderdrivers.riru.edxp.util.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.SELinuxHelper;
import de.robv.android.xposed.services.BaseService;

public class InstallerChooser {

    private static final AtomicBoolean hasSet = new AtomicBoolean(false);
    @SuppressLint("SdCardPath")
    private static final String DATA_DIR_PATH_PREFIX =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? "/data/user_de/0/" : "/data/user/0/";


    public static final String PRIMARY_INSTALLER_PACKAGE_NAME = "org.meowcat.edxposed.manager";
    public static final String LEGACY_INSTALLER_PACKAGE_NAME = "de.robv.android.xposed.installer";

    public static String INSTALLER_PACKAGE_NAME;
    public static String INSTALLER_DATA_BASE_DIR;

    public static void setInstallerPackageName(String packageName) {
        INSTALLER_PACKAGE_NAME = packageName;
        INSTALLER_DATA_BASE_DIR = DATA_DIR_PATH_PREFIX + INSTALLER_PACKAGE_NAME + "/";
    }

    public static void setup() {
        if (!hasSet.compareAndSet(false, true)) {
            // init once
            return;
        }
        String dataBaseDir;
        if (checkDataDirValid(dataBaseDir = DATA_DIR_PATH_PREFIX + PRIMARY_INSTALLER_PACKAGE_NAME + "/")) {
            INSTALLER_PACKAGE_NAME = PRIMARY_INSTALLER_PACKAGE_NAME;
            INSTALLER_DATA_BASE_DIR = dataBaseDir;
            Utils.logI("using " + PRIMARY_INSTALLER_PACKAGE_NAME + "as installer app");
            return;
        }
        if (checkDataDirValid(dataBaseDir = DATA_DIR_PATH_PREFIX + LEGACY_INSTALLER_PACKAGE_NAME + "/")) {
            INSTALLER_PACKAGE_NAME = LEGACY_INSTALLER_PACKAGE_NAME;
            INSTALLER_DATA_BASE_DIR = dataBaseDir;
            Utils.logI("using " + LEGACY_INSTALLER_PACKAGE_NAME + "as installer app");
            return;
        }
        Utils.logE("no supported installer app found");
    }

    /**
     * For some reason checkFileExists(dirPath) is not reliable when forkSystemServerPre
     */
    private static boolean checkDataDirValid(String dirPath) {
        BaseService fileService = SELinuxHelper.getAppDataFileService();
        return fileService.checkFileExists(dirPath + "code_cache")
                || fileService.checkFileExists(dirPath + "cache");
    }
}
