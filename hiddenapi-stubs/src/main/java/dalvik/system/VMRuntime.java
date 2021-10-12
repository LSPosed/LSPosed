package dalvik.system;

import androidx.annotation.RequiresApi;

public class VMRuntime {

    public static VMRuntime getRuntime() {
        throw new RuntimeException("Stub!");
    }

    public native boolean is64Bit();
    
    public native String vmInstructionSet();

    @RequiresApi(29)
    public static native void setProcessDataDirectory(String dataDir);
}
