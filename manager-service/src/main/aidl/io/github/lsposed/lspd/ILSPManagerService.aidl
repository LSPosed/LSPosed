package io.github.lsposed.lspd;

import io.github.lsposed.lspd.utils.ParceledListSlice;

interface ILSPManagerService {
    int getVersion() = 1;
    ParceledListSlice<PackageInfo> getInstalledPackagesFromAllUsers(int flags) = 2;
}
