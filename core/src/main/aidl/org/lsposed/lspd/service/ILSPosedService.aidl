package org.lsposed.lspd.service;

import org.lsposed.lspd.service.ILSPApplicationService;

interface ILSPosedService {
    ILSPApplicationService requestApplicationService(int uid, int pid, String processName, IBinder heartBeat) = 1;

    oneway void dispatchSystemServerContext(in IBinder activityThread, in IBinder activityToken) = 4;
}
