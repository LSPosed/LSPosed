package io.github.lsposed.lspd.service;

interface ILSPApplicationService {
    void registerHeartBeat(IBinder handle) = 1;

    int getVariant() = 2;

    IBinder requestModuleBinder() = 3;

    IBinder requestManagerBinder() = 4;
}
