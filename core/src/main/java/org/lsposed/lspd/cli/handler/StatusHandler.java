package org.lsposed.lspd.cli.handler;

import android.os.RemoteException;

import org.lsposed.lspd.ILSPManagerService;

public class StatusHandler implements ICommandHandler {
    @Override
    public String getUsage() {
        return "\tLSPosed status";
    }

    @Override
    public String getHandlerName() {
        return "status";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException {
        String s = "API: " + manager.getApi() + '\n' +
                "LSPosed version: " + manager.getXposedVersionName() +
                '(' + manager.getXposedVersionCode() + ")\n" +
                "Xposed API Version: " + manager.getXposedApiVersion();
        System.out.println(s);
    }
}
