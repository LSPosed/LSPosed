package org.lsposed.lspd.models;
import org.lsposed.lspd.models.PreLoadedApk;
import org.lsposed.lspd.service.ILSPInjectedModuleService;

parcelable Module {
    String packageName;
    int appId;
    String apkPath;
    PreLoadedApk file;
    ApplicationInfo applicationInfo;
    ILSPInjectedModuleService service;
}
