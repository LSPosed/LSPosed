/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lsposed.lspd.utils;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lsposed.lspd.managerservice.BuildConfig;

/**
 * Transfer a large list of Parcelable objects across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * Caveat: for efficiency and security, all elements must be the same concrete type.
 * In order to avoid writing the class name of each object, we must ensure that
 * each object is the same type, or else unparceling then reparceling the data may yield
 * a different result if the class name encoded in the Parcelable is a Base type.
 * See b/17671747.
 *
 */
abstract class BaseParceledListSlice<T> implements Parcelable {
    private static String TAG = "ParceledListSlice";
    private static boolean DEBUG = BuildConfig.DEBUG;
    /*
     * TODO get this number from somewhere else. For now set it to a quarter of
     * the 1MB limit.
     */
    private static final int MAX_IPC_SIZE = 64 * 1024;
    private final List<T> mList;
    private int mInlineCountLimit = Integer.MAX_VALUE;

    public BaseParceledListSlice(List<T> list) {
        if(list == null) {
            mList = Collections.emptyList();
        } else {
            mList = list;
        }
    }

    @SuppressWarnings("unchecked")
    BaseParceledListSlice(Parcel p, ClassLoader loader) {
        final int N = p.readInt();
        mList = new ArrayList<T>(N);
        if (DEBUG) Log.d(TAG, "Retrieving " + N + " items");
        if (N <= 0) {
            return;
        }
        Parcelable.Creator<?> creator = readParcelableCreator(p, loader);
        Class<?> listElementClass = null;
        int i = 0;
        while (i < N) {
            if (p.readInt() == 0) {
                break;
            }
            final T parcelable = readCreator(creator, p, loader);
            if (listElementClass == null) {
                listElementClass = parcelable.getClass();
            } else {
                verifySameType(listElementClass, parcelable.getClass());
            }
            mList.add(parcelable);
            if (DEBUG) Log.d(TAG, "Read inline #" + i + ": " + mList.get(mList.size()-1));
            i++;
        }
        if (i >= N) {
            return;
        }
        final IBinder retriever = p.readStrongBinder();
        while (i < N) {
            if (DEBUG) Log.d(TAG, "Reading more @" + i + " of " + N + ": retriever=" + retriever);
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(i);
            try {
                retriever.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
            } catch (RemoteException e) {
                Log.w(TAG, "Failure retrieving array; only received " + i + " of " + N, e);
                return;
            }
            while (i < N && reply.readInt() != 0) {
                final T parcelable = readCreator(creator, reply, loader);
                verifySameType(listElementClass, parcelable.getClass());
                mList.add(parcelable);
                if (DEBUG) Log.d(TAG, "Read extra #" + i + ": " + mList.get(mList.size()-1));
                i++;
            }
            reply.recycle();
            data.recycle();
        }
    }
    private T readCreator(Parcelable.Creator<?> creator, Parcel p, ClassLoader loader) {
        if (creator instanceof Parcelable.ClassLoaderCreator<?>) {
            Parcelable.ClassLoaderCreator<?> classLoaderCreator =
                    (Parcelable.ClassLoaderCreator<?>) creator;
            return (T) classLoaderCreator.createFromParcel(p, loader);
        }
        return (T) creator.createFromParcel(p);
    }
    private static void verifySameType(final Class<?> expected, final Class<?> actual) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Can't unparcel type "
                    + (actual == null ? null : actual.getName()) + " in list of type "
                    + (expected == null ? null : expected.getName()));
        }
    }
    public List<T> getList() {
        return mList;
    }
    /**
     * Set a limit on the maximum number of entries in the array that will be included
     * inline in the initial parcelling of this object.
     */
    public void setInlineCountLimit(int maxCount) {
        mInlineCountLimit = maxCount;
    }
    /**
     * Write this to another Parcel. Note that this discards the internal Parcel
     * and should not be used anymore. This is so we can pass this to a Binder
     * where we won't have a chance to call recycle on this.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int N = mList.size();
        final int callFlags = flags;
        dest.writeInt(N);
        if (DEBUG) Log.d(TAG, "Writing " + N + " items");
        if (N > 0) {
            final Class<?> listElementClass = mList.get(0).getClass();
            writeParcelableCreator(mList.get(0), dest);
            int i = 0;
            while (i < N && i < mInlineCountLimit && dest.dataSize() < MAX_IPC_SIZE) {
                dest.writeInt(1);
                final T parcelable = mList.get(i);
                verifySameType(listElementClass, parcelable.getClass());
                writeElement(parcelable, dest, callFlags);
                if (DEBUG) Log.d(TAG, "Wrote inline #" + i + ": " + mList.get(i));
                i++;
            }
            if (i < N) {
                dest.writeInt(0);
                Binder retriever = new Binder() {
                    @Override
                    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                            throws RemoteException {
                        if (code != FIRST_CALL_TRANSACTION) {
                            return super.onTransact(code, data, reply, flags);
                        }
                        int i = data.readInt();
                        if (DEBUG) Log.d(TAG, "Writing more @" + i + " of " + N);
                        while (i < N && reply.dataSize() < MAX_IPC_SIZE) {
                            reply.writeInt(1);
                            final T parcelable = mList.get(i);
                            verifySameType(listElementClass, parcelable.getClass());
                            writeElement(parcelable, reply, callFlags);
                            if (DEBUG) Log.d(TAG, "Wrote extra #" + i + ": " + mList.get(i));
                            i++;
                        }
                        if (i < N) {
                            if (DEBUG) Log.d(TAG, "Breaking @" + i + " of " + N);
                            reply.writeInt(0);
                        }
                        return true;
                    }
                };
                if (DEBUG) Log.d(TAG, "Breaking @" + i + " of " + N + ": retriever=" + retriever);
                dest.writeStrongBinder(retriever);
            }
        }
    }
    protected abstract void writeElement(T parcelable, Parcel reply, int callFlags);
    protected abstract void writeParcelableCreator(T parcelable, Parcel dest);
    protected abstract Parcelable.Creator<?> readParcelableCreator(Parcel from, ClassLoader loader);
}
