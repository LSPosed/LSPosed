package android.os;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.PrintWriter;

public abstract class ShellCommand {
    public int exec(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err,
                    String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        throw new IllegalArgumentException("STUB!");
    }

    public abstract int onCommand(String cmd);
    public abstract void onHelp();

    public String getNextOption(){
        throw new IllegalArgumentException("STUB!");
    }

    public String getNextArgRequired() {
        throw new IllegalArgumentException("STUB!");
    }

    public PrintWriter getErrPrintWriter() {
        throw new IllegalArgumentException("STUB!");
    }
    public PrintWriter getOutPrintWriter() {
        throw new IllegalArgumentException("STUB!");
    }
    public InputStream getRawInputStream() {
        throw new IllegalArgumentException("STUB!");
    }
}
