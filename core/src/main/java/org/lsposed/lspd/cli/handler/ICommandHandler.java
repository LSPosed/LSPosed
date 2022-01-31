package org.lsposed.lspd.cli.handler;

import android.os.RemoteException;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.exception.FormatException;
import org.lsposed.lspd.cli.exception.NoEnoughArgumentException;
import org.lsposed.lspd.cli.exception.NotXposedModuleException;
import org.lsposed.lspd.cli.exception.UninstalledPackageException;
import org.lsposed.lspd.cli.exception.UnknownCommandException;

public interface ICommandHandler {
    String getUsage();
    String getHandlerName();
    void handle(ILSPManagerService manager, String[] args) throws RemoteException, NotXposedModuleException, NoEnoughArgumentException, UnknownCommandException, UninstalledPackageException, FormatException;
}
