package org.lsposed.lspd.cli.handler;

import android.os.RemoteException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.Utils;
import org.lsposed.lspd.cli.exception.FormatException;
import org.lsposed.lspd.cli.exception.NoEnoughArgumentException;
import org.lsposed.lspd.cli.exception.NotXposedModuleException;
import org.lsposed.lspd.cli.exception.UninstalledPackageException;
import org.lsposed.lspd.cli.exception.UnknownCommandException;
import org.lsposed.lspd.models.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.github.xposed.xposedservice.utils.ParceledListSlice;

public class ScopeHandler implements ICommandHandler {


    @Parameters(commandNames = "ls", commandDescription = "List module scope")
    public static class LSCommand {
        @Parameter(description = "Module package name")
        public String packageName;

        public void exec(ILSPManagerService manager) throws RemoteException, NoEnoughArgumentException, NotXposedModuleException {
            if (packageName == null) {
                throw new NoEnoughArgumentException("Module package name required");
            }

            if (!Utils.validXposedModule(manager, packageName)) {
                throw new NotXposedModuleException(packageName);
            }

            var scope = manager.getModuleScope(packageName).getList();
            for (var app : scope) {
                System.out.println(app.packageName + "/" + app.userId);
            }
        }

    }

    public static class ScopeEditCommand {
        @Parameter(names = {"-m", "--module"}, description = "Module package name", required = true, order = 0)
        public String packageName;
        @Parameter(description = "scope")
        public List<String> scope;
        @Parameter(names = {"-i", "--ignore"}, description = "Will not throw an exception if app not installed")
        public boolean ignore = false;
        protected List<Application> apps;

        public void exec(ILSPManagerService manager) throws NoEnoughArgumentException, FormatException, RemoteException, UninstalledPackageException, NotXposedModuleException {
            if (packageName == null) {
                throw new NoEnoughArgumentException("No module package name");
            }

            if (!Utils.validXposedModule(manager, packageName)) {
                throw new NotXposedModuleException(packageName);
            }

            if (scope == null || scope.size() == 0) {
                throw new NoEnoughArgumentException("Need at least one scope");
            }

            apps = new ArrayList<>();
            for(var item : scope) {
                var app = parseApplication(item);
                if (Utils.validPackageNameAndUserId(manager, app.packageName, app.userId)) {
                    apps.add(app);
                } else if (!ignore) {
                    throw new UninstalledPackageException(app.packageName, app.userId);
                }
            }

        }

    }

    @Parameters(commandNames = "set", commandDescription = "Set module scope")
    public static class SetCommand extends ScopeEditCommand {
        @Override
        public void exec(ILSPManagerService manager) throws NoEnoughArgumentException, UninstalledPackageException, RemoteException, FormatException, NotXposedModuleException {
            super.exec(manager);
            manager.setModuleScope(packageName, new ParceledListSlice<>(apps));
            System.out.println("success");
        }
    }

    @Parameters(commandNames = "append", commandDescription = "Append module scope")
    public static class AppendCommand extends ScopeEditCommand {
        @Override
        public void exec(ILSPManagerService manager) throws NoEnoughArgumentException, UninstalledPackageException, RemoteException, FormatException, NotXposedModuleException {
            super.exec(manager);
            var oldScope = manager.getModuleScope(packageName).getList();
            var oldScopeSet = new HashSet<String>();
            for (var app : oldScope) {
                oldScopeSet.add(app.packageName + "/" + app.userId);
            }
            for (var app: apps) {
                oldScopeSet.add(app.packageName + "/" + app.userId);
            }
            var newScope = new ArrayList<Application>();
            for (var app : oldScopeSet) {
                newScope.add(parseApplication(app));
            }
            manager.setModuleScope(packageName, new ParceledListSlice<>(newScope));
            System.out.println("success");
        }
    }

    @Parameters(commandNames = "remove", commandDescription = "Remove module scope")
    public static class RemoveCommand extends ScopeEditCommand {
        @Override
        public void exec(ILSPManagerService manager) throws NoEnoughArgumentException, UninstalledPackageException, RemoteException, FormatException, NotXposedModuleException {
            super.exec(manager);
            var oldScope = manager.getModuleScope(packageName).getList();
            var oldScopeSet = new HashSet<String>();
            for (var app : oldScope) {
                oldScopeSet.add(app.packageName + "/" + app.userId);
            }
            for (var app : apps) {
                oldScopeSet.remove(app.packageName + "/" + app.userId);
            }
            var newScope = new ArrayList<Application>();
            for (var app : oldScopeSet) {
                newScope.add(parseApplication(app));
            }
            manager.setModuleScope(packageName, new ParceledListSlice<>(newScope));
            System.out.println("success");
        }
    }

    private static Application parseApplication(String name) throws FormatException {
        var splitIndex = name.indexOf('/');
        if (splitIndex < 0) {
            throw new FormatException("illegal scope format, format should be <package name>/<user id>");
        }
        var packageName = name.substring(0, splitIndex);
        var userId = Integer.parseInt(name.substring(splitIndex+1));

        var res = new Application();
        res.packageName = packageName;
        res.userId = userId;

        return res;
    }

    public void showUsage() {
        JCommander.newBuilder()
                .addCommand(new LSCommand())
//                .addCommand(new AppendCommand())
//                .addCommand(new SetCommand())
//                .addCommand(new RemoveCommand())
                .programName("lsposed scope")
                .build()
                .usage();
    }

    @Override
    public String getHandlerName() {
        return "scope";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException, NotXposedModuleException, NoEnoughArgumentException, UnknownCommandException, UninstalledPackageException, FormatException {

        if (args.length < 2) {
            throw new NoEnoughArgumentException("No enough argument: no sub command found");
        }


        LSCommand lsCommand = new LSCommand();
        AppendCommand appendCommand = new AppendCommand();
        SetCommand setCommand = new SetCommand();
        RemoveCommand removeCommand = new RemoveCommand();

        // TODO: permission manager
        JCommander jc = JCommander.newBuilder()
                .addCommand("ls", lsCommand)
//                .addCommand("append", appendCommand)
//                .addCommand("remove", removeCommand)
//                .addCommand("set", setCommand)
                .addCommand("help", new Utils.HelpCommandStub())
                .build();

        jc.parse(Arrays.copyOfRange(args, 1, args.length));

        switch (jc.getParsedCommand()) {
            case "ls":
                lsCommand.exec(manager);
                break;
            case "append":
                appendCommand.exec(manager);
                break;
            case "remove":
                removeCommand.exec(manager);
                break;
            case "set":
                setCommand.exec(manager);
                break;
            case "help":
                showUsage();
                break;
        }
    }
}
