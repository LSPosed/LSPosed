package org.lsposed.lspd.cli.handler;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import org.lsposed.lspd.ILSPManagerService;
import org.lsposed.lspd.cli.Utils;
import org.lsposed.lspd.cli.exception.FormatException;
import org.lsposed.lspd.cli.exception.NoEnoughArgumentException;
import org.lsposed.lspd.cli.exception.NotXposedModuleException;
import org.lsposed.lspd.cli.exception.UnknownCommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModulesHandler implements ICommandHandler {

    @Parameters(commandNames = "ls", commandDescription = "List xposed modules")
    public static class LSCommand {
        @Parameter(names = {"-d", "--disabled"})
        public boolean disabled = false;
        @Parameter(names = {"-e", "--enabled"})
        public boolean enabled = false;

        public void exec(ILSPManagerService manager, Set<XposedModuleInfo> modules) throws FormatException {
            if (enabled && disabled) {
                throw new FormatException("Conflict argument `enable` and `disable`");
            }

            List<XposedModuleInfo> outputModules = new ArrayList<>();
            if (!enabled && !disabled) {
                outputModules.addAll(modules);
            } else {
                for (var module : modules) {
                    if (module.enable == enabled) {
                        outputModules.add(module);
                    }
                }
            }
            outputModules(outputModules);
        }

    }

    @Parameters(commandNames = "enable", commandDescription = "Enable xposed modules")
    public static class EnableCommand {
        @Parameter(names = {"-i", "--ignore"}, description = "If package not installed, ignore error")
        public boolean ignore = false;
        @Parameter(description = "Modules will be enabled")
        public List<String> modules;

        public void exec(ILSPManagerService manager) throws NotXposedModuleException, RemoteException {
            List<String> modulesTmp = new ArrayList<>();
            for (var module : modules) {
                if (Utils.validXposedModule(manager, module)) {
                    modulesTmp.add(module);
                } else if (!ignore) {
                    throw new NotXposedModuleException(module);
                }
            }
            for (var module : modulesTmp) {
                manager.enableModule(module);
            }
        }

    }

    @Parameters(commandNames = "disable", commandDescription = "Disable xposed modules")
    public static class DisableCommand {
        @Parameter(names = {"-i", "--ignore"}, description = "If package not installed, ignore error")
        public boolean ignore = false;
        @Parameter(description = "Modules will be disabled")
        public List<String> modules;

        public void exec(ILSPManagerService manager) throws RemoteException, NotXposedModuleException {
            List<String> modulesTmp = new ArrayList<>();
            for (var module : modules) {
                if (Utils.validXposedModule(manager, module)) {
                    modulesTmp.add(module);
                } else if (!ignore) {
                    throw new NotXposedModuleException(module);
                }
            }
            for (var module : modulesTmp) {
                manager.disableModule(module);
            }
        }
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

        @Override
        public boolean equals(Object o) {
            if (o instanceof XposedModuleInfo) {
                return packageName.equals(((XposedModuleInfo) o).packageName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
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
    private void showUsage() {
        JCommander.newBuilder()
                .addCommand("ls", new LSCommand())
//                .addCommand("enable", new EnableCommand())
//                .addCommand("disable", new DisableCommand())
                .programName("lsposed modules")
                .build()
                .usage();
    }

    @Override
    public String getHandlerName() {
        return "modules";
    }

    @Override
    public void handle(ILSPManagerService manager, String[] args) throws RemoteException, NotXposedModuleException, NoEnoughArgumentException, UnknownCommandException, FormatException {

        if (args.length < 2) {
            throw new NoEnoughArgumentException("No enough argument: no sub command found");
        }

        LSCommand lsCommand = new LSCommand();
        EnableCommand enableCommand = new EnableCommand();
        DisableCommand disableCommand = new DisableCommand();
        // TODO: permission manager
        var jc = JCommander.newBuilder()
                .addCommand("ls", lsCommand)
//                .addCommand("enable", enableCommand)
//                .addCommand("disable", disableCommand)
                .addCommand("help", new Utils.HelpCommandStub())
                .build();
        jc.parse(Arrays.copyOfRange(args, 1, args.length));
        switch (jc.getParsedCommand()) {
            case "ls":
                var packages = manager.getInstalledPackagesFromAllUsers(PackageManager.GET_META_DATA, false);
                var enableModulesSet = new HashSet<>(Arrays.asList(manager.enabledModules()));
                Set<XposedModuleInfo> modules = new HashSet<>();
                for (var packageInfo : packages.getList()) {
                    var metaData = packageInfo.applicationInfo.metaData;
                    if (metaData != null && metaData.containsKey("xposedminversion")) {
                        var module = new XposedModuleInfo(packageInfo, enableModulesSet.contains(packageInfo.packageName));
                        modules.add(module);
                    }
                }
                lsCommand.exec(manager, modules);
                break;
            case "enable":
                enableCommand.exec(manager);
                System.out.println("success");
                break;
            case "disable":
                disableCommand.exec(manager);
                System.out.println("success");
            case "help":
                showUsage();
        }

    }
}
