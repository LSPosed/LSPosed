package de.robv.android.xposed.services;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.io.IOException;

/** @hide */
public final class BinderService extends BaseService {
	public static final int TARGET_APP = 0;
	public static final int TARGET_SYSTEM = 1;

	/**
	 * Retrieve the binder service running in the specified context.
	 * @param target Either {@link #TARGET_APP} or {@link #TARGET_SYSTEM}.
	 * @return A reference to the service.
	 * @throws IllegalStateException In case the service doesn't exist (should never happen).
	 */
	public static BinderService getService(int target) {
		if (target < 0 || target > sServices.length) {
			throw new IllegalArgumentException("Invalid service target " + target);
		}
		synchronized (sServices) {
			if (sServices[target] == null) {
				sServices[target] = new BinderService(target);
			}
			return sServices[target];
		}
	}

	@Override
	public boolean checkFileAccess(String filename, int mode) {
		ensureAbsolutePath(filename);

		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		data.writeInterfaceToken(INTERFACE_TOKEN);
		data.writeString(filename);
		data.writeInt(mode);

		try {
			mRemote.transact(ACCESS_FILE_TRANSACTION, data, reply, 0);
		} catch (RemoteException e) {
			data.recycle();
			reply.recycle();
			return false;
		}

		reply.readException();
		int result = reply.readInt();
		reply.recycle();
		data.recycle();
		return result == 0;
	}

	@Override
	public FileResult statFile(String filename) throws IOException {
		ensureAbsolutePath(filename);

		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		data.writeInterfaceToken(INTERFACE_TOKEN);
		data.writeString(filename);

		try {
			mRemote.transact(STAT_FILE_TRANSACTION, data, reply, 0);
		} catch (RemoteException e) {
			data.recycle();
			reply.recycle();
			throw new IOException(e);
		}

		reply.readException();
		int errno = reply.readInt();
		if (errno != 0)
			throwCommonIOException(errno, null, filename, " while retrieving attributes for ");

		long size = reply.readLong();
		long time = reply.readLong();
		reply.recycle();
		data.recycle();
		return new FileResult(size, time);
	}

	@Override
	public byte[] readFile(String filename) throws IOException {
		return readFile(filename, 0, 0, 0, 0).content;
	}

	@Override
	public FileResult readFile(String filename, long previousSize, long previousTime) throws IOException {
		return readFile(filename, 0, 0, previousSize, previousTime);
	}

	@Override
	public FileResult readFile(String filename, int offset, int length,
			long previousSize, long previousTime) throws IOException {
		ensureAbsolutePath(filename);

		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		data.writeInterfaceToken(INTERFACE_TOKEN);
		data.writeString(filename);
		data.writeInt(offset);
		data.writeInt(length);
		data.writeLong(previousSize);
		data.writeLong(previousTime);

		try {
			mRemote.transact(READ_FILE_TRANSACTION, data, reply, 0);
		} catch (RemoteException e) {
			data.recycle();
			reply.recycle();
			throw new IOException(e);
		}

		reply.readException();
		int errno = reply.readInt();
		String errorMsg = reply.readString();
		long size = reply.readLong();
		long time = reply.readLong();
		byte[] content = reply.createByteArray();
		reply.recycle();
		data.recycle();

		switch (errno) {
			case 0:
				return new FileResult(content, size, time);
			case 22: // EINVAL
				if (errorMsg != null) {
					IllegalArgumentException iae = new IllegalArgumentException(errorMsg);
					if (offset == 0 && length == 0)
						throw new IOException(iae);
					else
						throw iae;
				} else {
					throw new IllegalArgumentException("Offset " + offset + " / Length " + length
							+ " is out of range for " + filename + " with size " + size);
				}
			default:
				throwCommonIOException(errno, errorMsg, filename, " while reading ");
				throw new IllegalStateException(); // not reached
		}
	}


	// ----------------------------------------------------------------------------
	private static final String INTERFACE_TOKEN = "de.robv.android.xposed.IXposedService";

	private static final int ACCESS_FILE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 2;
	private static final int STAT_FILE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 3;
	private static final int READ_FILE_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 4;

	private static final String[] SERVICE_NAMES = { "user.xposed.app", "user.xposed.system" };
	private static final BinderService[] sServices = new BinderService[2];
	private final IBinder mRemote;

	private BinderService(int target) {
		IBinder binder = ServiceManager.getService(SERVICE_NAMES[target]);
		if (binder == null)
			throw new IllegalStateException("Service " + SERVICE_NAMES[target] + " does not exist");
		this.mRemote = binder;
	}
}
