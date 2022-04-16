package dalvik.system;

public class VMRuntime {

    public static VMRuntime getRuntime() {
        throw new RuntimeException("Stub!");
    }

    // Use `Process.is64Bit()` instead
    public native boolean is64Bit();

    public native String vmInstructionSet();

    public native boolean isJavaDebuggable();
}
