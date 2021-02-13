/*
 * This file is part of LSPosed.
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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.lspd.service;

import android.os.Handler;
import android.os.IBinder;
import android.os.IServiceManager;
import android.os.Looper;
import android.os.ServiceManager;

import io.github.lsposed.common.KeepAll;
import io.github.lsposed.lspd.util.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ServiceProxy implements InvocationHandler, KeepAll {

    public static String CONFIG_PATH = null;
    public static String INSTALLER_PACKAGE_NAME = null;

    private static IServiceManager original;

    public synchronized static void install() throws ReflectiveOperationException {
        if (original != null) return;

        Method method = ServiceManager.class.getDeclaredMethod("getIServiceManager");
        Field field = ServiceManager.class.getDeclaredField("sServiceManager");

        method.setAccessible(true);
        field.setAccessible(true);

        original = (IServiceManager) method.invoke(null);
        field.set(null, Proxy.newProxyInstance(
                ServiceProxy.class.getClassLoader(),
                new Class[]{IServiceManager.class},
                new ServiceProxy()
        ));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "addService": {
                if (args.length > 1 && args[0] instanceof String && args[1] instanceof IBinder) {
                    final String name = (String) args[0];
                    final IBinder service = (IBinder) args[1];
                    args[1] = onAddService(name, service);
                }
                return method.invoke(original, args);
            }
//                case "getService":
//                    if(args.length == 1 && args[0] instanceof String && method.getReturnType() == IBinder.class) {
//                        final String name = (String) args[0];
//                        final IBinder service = (IBinder)method.invoke(original, args);
//                        return onGetService(name, service);
//                    }
//                    return method.invoke(original, args);
            default:
                return method.invoke(original, args);
        }
    }

    private IBinder onAddService(String name, IBinder service) {
        if ("activity".equals(name)) {
            try {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        PackageReceiver.register();
                    }
                });
            } catch (Throwable e) {
                Utils.logW("Error in registering package receiver", e);
            }
            return service;
        }
        return service;
    }

//        protected IBinder onGetService(String name, IBinder service) {
//            return service;
//        }
}
