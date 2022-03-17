package android.system;

import android.util.MutableInt;

import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;

public class Os {
    public static int ioctlInt(FileDescriptor fd, int cmd, MutableInt arg) throws ErrnoException {
        throw new ErrnoException();
    }

    @RequiresApi(27)
    public static int ioctlInt(FileDescriptor fd, int cmd, Int32Ref arg) throws ErrnoException {
        throw new ErrnoException();
    }

    @RequiresApi(31)
    public static int ioctlInt(FileDescriptor fd, int cmd) throws ErrnoException {
        throw new ErrnoException();
    }
}
