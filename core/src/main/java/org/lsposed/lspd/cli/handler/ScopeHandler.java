package org.lsposed.lspd.cli.handler;

import android.os.RemoteException;

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

import io.github.xposed.xposedservice.utils.ParceledListSlice;

public class ScopeHandler implements ICommandHandler {

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

    @Override
    public String getUsage() {
        return "\tls <package name>                   \t output the scope of module.\n" +
                "\tappend <package name> [-i] <scopes>\t append provided apps to the scope, -i to ignore invalid apps.\n" +
                "\tremove <package name> [-i] <scopes>\t delete provided apps to the scope. -i to ignore invalid apps.\n" +
                "\tset <package name> <scopes>        \t replace the scope with apps provided.\n";
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

        var modulePackageName = args[2];
        boolean ignoreInvalidPackageName = false;
        if (!Utils.validXposedModule(manager, modulePackageName)) {
            throw new NotXposedModuleException(modulePackageName);
        }
        if (!args[1].equals("ls")) {
            var info = "No enough arguments for command" + args[1] + ", need at least one scope";
            if (args.length <= 3) {
                throw new NoEnoughArgumentException(info);
            }
            if (args[3].equals("-i") || args[3].equals("--ignore")) {
                ignoreInvalidPackageName = true;
                if (args.length <= 4) {
                    throw new NoEnoughArgumentException(info);
                }
            }
        }
        var scope = manager.getModuleScope(modulePackageName).getList();
        switch (args[1]) {
            case "ls":
                scope.forEach(application -> System.out.println(application.packageName + "/" + application.userId));
                break;
            case "append":
                var appendScope = new HashSet<>(
                        Arrays.asList(Arrays.copyOfRange(args, ignoreInvalidPackageName ? 4 : 3, args.length)));
                for(var scopeItemName : appendScope) {
                    var app = parseApplication(scopeItemName);
                    if (Utils.validPackageNameAndUserId(manager, app.packageName, app.userId)) {
                        scope.add(app);
                    } else if (!ignoreInvalidPackageName) {
                        throw new UninstalledPackageException(app.packageName, app.userId);
                    }
                }
                manager.setModuleScope(modulePackageName, new ParceledListSlice<>(scope));
                System.out.println("success");
                break;
            case "remove":
                var removeScope = new HashSet<>(
                        Arrays.asList(Arrays.copyOfRange(args, ignoreInvalidPackageName ? 4:3, args.length)));
                var newScope = new ArrayList<Application>();
                for(var item : scope) {
                    if (!removeScope.contains(item.packageName + "/" + item.userId)) {
                        newScope.add(item);
                    }
                }
                manager.setModuleScope(modulePackageName, new ParceledListSlice<>(newScope));
                System.out.println("success");
                break;
            case "set":
                var newScope2 = new ArrayList<Application>(args.length - 2);
                for (String scopeItemName : new HashSet<>(
                        Arrays.asList(Arrays.copyOfRange(args, ignoreInvalidPackageName ? 4 : 3, args.length)))) {
                    var app = parseApplication(scopeItemName);
                    if (Utils.validPackageNameAndUserId(manager, app.packageName, app.userId)) {
                        newScope2.add(app);
                    } else if (!ignoreInvalidPackageName) {
                        throw new UninstalledPackageException(app.packageName, app.userId);
                    }
                }
                manager.setModuleScope(modulePackageName, new ParceledListSlice<>(newScope2));
                System.out.println("success");
                break;
            default:
                throw new UnknownCommandException("scope", args[1]);
        }
    }
}
