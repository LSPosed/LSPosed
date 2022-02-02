package org.lsposed.lspd.cli.exception;

public class NotXposedModuleException extends Exception {

    private final String packageName;

    public NotXposedModuleException(String packageName) {
        super(packageName + "is not a xposed module");
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }
}
