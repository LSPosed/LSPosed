package org.lsposed.lspd.cli.handler;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.exception.UnknownCommandException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;


public class LogHandler implements ICommandHandler {

    @Parameters(commandDescription = "Query or clear log")
    public static class Args {
        @Parameter(names = {"-v", "--verbose"}, description = "Is verbose log")
        public boolean verbose = false;

        @Parameter(names = "clear", description = "Clear log")
        public boolean clear;

        @Parameter(names = "help", help = true)
        public boolean help;

        public void run(ILSPManagerService manager) throws RemoteException {

            if (help) {
                JCommander.newBuilder()
                        .addObject(new Args())
                        .programName("lsposed log")
                        .build()
                        .usage();
                return;
            }

            if (clear) {
                manager.clearLogs(verbose);
            } else {
                ParcelFileDescriptor fileDescriptor = verbose ? manager.getVerboseLog() : manager.getModulesLog();
                try {
                    LogHandler.printLog(fileDescriptor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void printLog(ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        var br = new BufferedReader(new InputStreamReader(new FileInputStream(parcelFileDescriptor.getFileDescriptor())));
        var logs = br.lines().parallel().collect(Collectors.toList());
        System.out.println(String.join( "\n", logs));
    }

    @Override
    public String getHandlerName() {
        return "log";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException, UnknownCommandException {

        Args obj = new Args();
        JCommander.newBuilder()
                .addObject(obj)
                .build()
                .parse(Arrays.copyOfRange(args, 1, args.length));
        obj.run(manager);

    }
}
