package org.lsposed.lspd.service;

interface IRemotePreferenceCallback {
    oneway void onUpdate(in Bundle map);
}
