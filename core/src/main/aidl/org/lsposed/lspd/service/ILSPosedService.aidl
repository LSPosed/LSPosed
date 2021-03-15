package org.lsposed.lspd.service;

import org.lsposed.lspd.service.ILSPApplicationService;

interface ILSPosedService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat) = 1;

    oneway void dispatchPackageChanged(in Intent intent) = 2;
    oneway void dispatchBootCompleted(in Intent intent) = 3;
}
