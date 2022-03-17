package android.os;

public class SELinux {
    public static boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm) {
        throw new UnsupportedOperationException("Stub");
    }

    public static boolean setFileContext(String path, String context) {
        throw new UnsupportedOperationException("Stub");
    }

    public static boolean setFSCreateContext(String context){
        throw new UnsupportedOperationException("Stub");
    }
}
