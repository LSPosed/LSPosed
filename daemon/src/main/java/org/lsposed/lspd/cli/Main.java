package org.lsposed.lspd.cli;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.beust.jcommander.ParameterException;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.exception.FormatException;
import org.lsposed.lspd.cli.exception.NoEnoughArgumentException;
import org.lsposed.lspd.cli.exception.NotXposedModuleException;
import org.lsposed.lspd.cli.exception.UninstalledPackageException;
import org.lsposed.lspd.cli.exception.UnknownCommandException;
import org.lsposed.lspd.cli.handler.ICommandHandler;
import org.lsposed.lspd.cli.handler.LogHandler;
import org.lsposed.lspd.cli.handler.ModulesHandler;
import org.lsposed.lspd.cli.handler.ScopeHandler;
import org.lsposed.lspd.cli.handler.StatusHandler;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.util.SignInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main {

    private static final Map<String, ICommandHandler> handlerMap = new HashMap<>();

    private static void registerHandler(ICommandHandler handler) {
        handlerMap.put(handler.getHandlerName(), handler);
    }

    private static void initHandlers() {
        registerHandler(new ModulesHandler());
        registerHandler(new ScopeHandler());
        registerHandler(new LogHandler());
        registerHandler(new StatusHandler());
    }

    private static ILSPManagerService getLSPManagerService() throws RemoteException {
        var activity_service = ServiceManager.getService("activity");
        var heartBeat = new Binder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken("LSPosed");
        data.writeInt(2);
        data.writeString("lsp-cli:" + SignInfo.CLI_UUID);
        data.writeStrongBinder(heartBeat);

        var resArr = new ArrayList<IBinder>(1);
        if(activity_service.transact(1598837584, data, reply, 0)) {
            reply.readException();
            var serviceBinder = reply.readStrongBinder();
            if (serviceBinder == null) {
                System.out.println("binder null");
                return null;
            }
            var service = ILSPApplicationService.Stub.asInterface(serviceBinder);
            if(service.requestInjectedManagerBinder(resArr) == null) {
                System.out.println("not a manager");
                return null;
            }
            if (resArr.size() > 0) {
                return ILSPManagerService.Stub.asInterface(resArr.get(0));
            } else {
                System.out.println("arr size 0");
            }
        } else {
            System.out.println("transact fail");
        }

        return null;
    }

    private static void showUsage() {
        System.out.println("Usage: lsposed <subcommand> [...arguments]");
        System.out.println("Support subcommand: " + String.join(", ", handlerMap.keySet()));
        System.out.println("Please use `lsposed <subcommand> help` to get detail usage");
    }

    private static int exec(String[] args) {

        initHandlers();
        if (args.length < 1 || args[0].equals("-h") || args[0].equals("--help") || args[0].equals("usage")) {
            showUsage();
            return 1;
        }

        try {
            var manager = getLSPManagerService();
            if (manager == null) {
                System.out.println("Get manager binder fail, maybe the daemon hasn't started yet");
                return 1;
            }

            if (handlerMap.containsKey(args[0])) {
                Objects.requireNonNull(handlerMap.get(args[0])).handle(manager, args);
                return 0;
            } else {
                System.out.println("No command named \"" + args[0] + '"');
                System.out.println("Support command: " + String.join(", ", handlerMap.keySet()));
                return 1;
            }
        } catch (RemoteException e) {
            System.out.println("Throws a RemoteException when executing:");
            e.printStackTrace();
            return 1;
        } catch (NotXposedModuleException e) {
            System.out.println("Error: " + e.getPackageName() + " is not a enabled xposed module");
            return 1;
        } catch (UninstalledPackageException e) {
            System.out.println("Error: " + e.getPackageName() + (e.getUid() < 0? "" : ("/" + e.getUid())) + " is not a valid package name");
            return 1;
        } catch (NoEnoughArgumentException e) {
            System.out.println("Error: " + e.getInfo());
            return 1;
        } catch (UnknownCommandException e) {
            System.out.println("Unknown command: " + e.getMainCommand() + " " + e.getSubCommand());
            return 1;
        } catch (FormatException e) {
            System.out.println("Format error: " + e.getMessage());
            return 1;
        } catch (ParameterException e) {
            e.printStackTrace();
            return 1;
        }

    }

    public static void main(String[] args) {
        System.exit(exec(args));
    }
}
