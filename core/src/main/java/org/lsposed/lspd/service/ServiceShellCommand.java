package org.lsposed.lspd.service;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

import java.io.FileDescriptor;

class ServiceShellCommand {
    private static final int SHELL_COMMAND_TRANSACTION = ('_' << 24) | ('C' << 16) | ('M' << 8) | 'D';
    private final IBinder binder;

    ServiceShellCommand(String name) {
        binder = android.os.ServiceManager.getService(name);
    }

    void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
                      String[] args, ResultReceiver resultReceiver) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(in);
        data.writeFileDescriptor(out);
        data.writeFileDescriptor(err);
        data.writeStringArray(args);
        data.writeStrongBinder(null);
        resultReceiver.writeToParcel(data, 0);
        try {
            binder.transact(SHELL_COMMAND_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
