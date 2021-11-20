package org.lsposed.lspd.models;
import org.lsposed.lspd.models.PreLoadedApk;

parcelable Module {
    String packageName;
    int appId;
    String apkPath;
    PreLoadedApk file;
    IBinder moduleService;
    boolean self;
}
