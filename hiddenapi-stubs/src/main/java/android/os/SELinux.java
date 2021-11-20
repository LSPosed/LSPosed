package android.os;

public class SELinux {
    public static boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm) {
        throw new UnsupportedOperationException("Stub");
    }

    public static boolean setFileContext(String path, String context) {
        throw new UnsupportedOperationException("Stub");
    }

    public static native boolean isSELinuxEnabled();

    public static native boolean isSELinuxEnforced();

    public static native String getPidContext(int pid);
}
