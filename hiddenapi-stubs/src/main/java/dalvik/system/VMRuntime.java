package dalvik.system;

public class VMRuntime {

    public static VMRuntime getRuntime() {
        throw new RuntimeException("Stub!");
    }

    public native boolean is64Bit();
}