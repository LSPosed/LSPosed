package org.lsposed.lspd.cli.handler;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.Utils;
import org.lsposed.lspd.cli.exception.NoEnoughArgumentException;
import org.lsposed.lspd.cli.exception.NotXposedModuleException;
import org.lsposed.lspd.cli.exception.UnknownCommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ModulesHandler implements ICommandHandler {

    private enum ShowModel {
        ALL,
        ENABLED_ONLY,
        DISABLED_ONLY
    }

    private static class XposedModuleInfo {
        private final String packageName;
        private final int uid;
        private final boolean enable;

        public XposedModuleInfo(PackageInfo packageInfo, boolean enable) {
            this.enable = enable;
            packageName = packageInfo.packageName;
            uid = packageInfo.applicationInfo.uid;
        }

        public String getPackageName() {
            return packageName;
        }

        public int getUid() {
            return uid;
        }

        public boolean isEnable() {
            return enable;
        }
    }

    private static void outputModules(List<XposedModuleInfo> modules) {
        int moduleNameMaxLen = 0;
        for(var module : modules) {
            if (moduleNameMaxLen < module.getPackageName().length()) {
                moduleNameMaxLen = module.getPackageName().length();
            }
        }
        var formatStr = "%-" + (moduleNameMaxLen + 4) + "s %10d %-8s";
        for(var module : modules) {
            System.out.println(String.format(formatStr,
                    module.getPackageName(), module.getUid(), module.isEnable()? "enable" : "disable"));
        }
    }

    @Override
    public String getUsage() {
        return "\tls [-e/--enabled -d/--disabled] \t list all modules. -e/--enabled, -d/--disabled to show enabled/disable modules.\n" +
                "\tenable <modules package name>   \t enable provided module.\n" +
                "\tdisable <modules package name>  \t disable provided module.\n";
    }

    @Override
    public String getHandlerName() {
        return "modules";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException, NotXposedModuleException, NoEnoughArgumentException, UnknownCommandException {

        if (args.length < 2) {
            throw new NoEnoughArgumentException("No enough argument: no sub command found");
        }

        List<String> packageOperated = null;
        if (!args[1].equals("ls")) {
            if (args.length < 3) {
                throw new NoEnoughArgumentException("Command " + args[1] + " requires at least one argument");
            }
            packageOperated = Arrays.asList(Arrays.copyOfRange(args, 2, args.length).clone());
            for(var packageName : packageOperated) {
                if (!Utils.validXposedModule(manager, packageName)) {
                    throw new NotXposedModuleException(packageName);
                }
            }
        }

        switch (args[1]) {
            case "enable":
                for(var packageName : packageOperated) {
                    manager.enableModule(packageName);
                }
                System.out.println("success");
                break;
            case "disable":
                for(var packageName : packageOperated) {
                    manager.disableModule(packageName);
                }
                System.out.println("success");
                break;
            case "ls":
                var packages = manager.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA, false);
                var showModel = ShowModel.ALL;
                var enableModulesSet = new HashSet<>(Arrays.asList(manager.enabledModules()));
                List<XposedModuleInfo> modules = new ArrayList<>();
                if (args.length == 3) {
                    if (args[2].equals("-e") || args[2].equals("--enabled")) {
                        showModel = ShowModel.ENABLED_ONLY;
                    } else if (args[2].equals("-d") || args[2].equals("--disabled")) {
                        showModel = ShowModel.DISABLED_ONLY;
                    }
                }
                for (var packageInfo : packages.getList()) {
                    var metaData = packageInfo.applicationInfo.metaData;
                    if (metaData != null && metaData.containsKey("xposedminversion")) {
                        var module = new XposedModuleInfo(packageInfo, enableModulesSet.contains(packageInfo.packageName));
                        if (showModel == ShowModel.ALL ||
                            (showModel == ShowModel.ENABLED_ONLY && enableModulesSet.contains(packageInfo.packageName)) ||
                            (showModel == ShowModel.DISABLED_ONLY && !enableModulesSet.contains(packageInfo.packageName))) {
                            modules.add(module);
                        }
                    }
                }
                outputModules(modules);
                break;
            default:
                throw new UnknownCommandException("scope", args[1]);
        }

    }
}
