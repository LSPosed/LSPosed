package org.lsposed.lspd.cli.handler;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.exception.UnknownCommandException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class LogHandler implements ICommandHandler {

    private static void printLog(ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        var br = new BufferedReader(new InputStreamReader(new FileInputStream(parcelFileDescriptor.getFileDescriptor())));
        var logs = br.lines().parallel().collect(Collectors.toList());
        System.out.println(String.join( "\n", logs));
    }

    @Override
    public String getUsage() {
        return "\t[-v]        \t show modules log, -v to show verbose log\n" +
                "\tclear [-v] \t clear modules log, -v to clear verbose log.\n";
    }

    @Override
    public String getHandlerName() {
        return "log";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException, UnknownCommandException {

        if (args.length >= 2 && args[1].equals("clear")) {
            if (args.length == 2) {
                manager.clearLogs(false);
            } else if (args.length == 3 && args[2].equals("-v")) {
                manager.clearLogs(true);
            } else {
                throw new UnknownCommandException("log", "clear");
            }
            return;
        }
        ParcelFileDescriptor parcelFileDescriptor;
        if (args.length == 1) {
            parcelFileDescriptor = manager.getModulesLog();
        } else if (args.length == 2 && args[1].equals("-v")) {
            parcelFileDescriptor = manager.getVerboseLog();
        } else {
            throw new UnknownCommandException("log", args[1]);
        }

        try {
            printLog(parcelFileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
