package io.github.lsposed.lspd.service;

interface ILSPApplicationService {
    void registerHeartBeat(IBinder handle) = 1;

    IBinder requestModuleBinder() = 2;

    IBinder requestManagerBinder() = 3;
}
