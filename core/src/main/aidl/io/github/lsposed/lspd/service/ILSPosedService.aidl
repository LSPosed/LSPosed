package io.github.lsposed.lspd.service;

import io.github.lsposed.lspd.service.ILSPApplicationService;

interface ILSPosedService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName) = 1;

    oneway void dispatchPackageChanged(in Intent intent) = 2;
}