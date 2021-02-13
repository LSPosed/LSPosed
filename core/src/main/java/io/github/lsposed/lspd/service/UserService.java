package io.github.lsposed.lspd.service;

import android.content.pm.UserInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.util.List;

public class UserService {
    private static IUserManager um = null;
    private static IBinder binder = null;

    public static IUserManager getUserManager() {
        if (binder == null && um == null) {
            binder = ServiceManager.getService("user");
            um = IUserManager.Stub.asInterface(binder);
        }
        return um;
    }

    public static int[] getUsers() throws RemoteException {
        IUserManager um = getUserManager();
        if (um == null) return new int[0];
        List<UserInfo> users;
        if(Build.VERSION.SDK_INT >= 30) {
            users = um.getUsers(true, true, true);
        } else {
            users = um.getUsers(true);
        }
        int[] userArray = new int[users.size()];
        for (int i = 0; i < users.size(); i++) {
            UserInfo uh = users.get(i);
            userArray[i] = uh.id;
        }
        return userArray;
    }
}
