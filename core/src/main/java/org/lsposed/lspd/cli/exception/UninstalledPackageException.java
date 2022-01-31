package org.lsposed.lspd.cli.exception;

public class UninstalledPackageException extends Exception {

    private final String packageName;
    private final int uid;

    public UninstalledPackageException(String packageName, int uid) {
        super(packageName + "/" + uid + " not installed");
        this.packageName = packageName;
        this.uid = uid;
    }

    public UninstalledPackageException(String packageName) {
        super(packageName + " not installed");
        this.packageName = packageName;
        this.uid = -1;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getUid() {
        return uid;
    }
}
