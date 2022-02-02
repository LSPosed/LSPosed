package org.lsposed.lspd.cli.handler;

import android.os.RemoteException;

import org.lsposed.lspd.ILSPManagerService;

public class StatusHandler implements ICommandHandler {

    @Override
    public String getHandlerName() {
        return "status";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException {

        if (args.length == 2 && args[1].equals("help")) {
            System.out.println("No argument required, just show status");
            return;
        }

        String s = "API: " + manager.getApi() + '\n' +
                "LSPosed version: " + manager.getXposedVersionName() +
                '(' + manager.getXposedVersionCode() + ")\n" +
                "Xposed API Version: " + manager.getXposedApiVersion();
        System.out.println(s);
    }
}
