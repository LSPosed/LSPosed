package org.lsposed.lspd.impl;

interface IRemotePreferenceCallback {
    oneway void onUpdate(in Bundle map);
}
