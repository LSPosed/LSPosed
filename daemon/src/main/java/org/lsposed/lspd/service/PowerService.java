/*
 * <!--This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023 LSPosed Contributors-->
 */

package org.lsposed.lspd.service;

import static android.content.Context.POWER_SERVICE;
import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class PowerService {
    private static IPowerManager pm = null;
    private static IBinder binder = null;
    private static final IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.w(TAG, "PowerManager is dead");
            binder.unlinkToDeath(this, 0);
            binder = null;
            pm = null;
        }
    };

    private static IPowerManager getPowerManager() {
        if (binder == null || pm == null) {
            binder = ServiceManager.getService(POWER_SERVICE);
            if (binder == null) return null;
            try {
                binder.linkToDeath(recipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            pm = IPowerManager.Stub.asInterface(binder);
        }
        return pm;
    }

    public static void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
        IPowerManager pm = getPowerManager();
        if (pm == null) return;
        pm.reboot(confirm, reason, wait);
    }
}
