package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.BridgeService.TAG;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.util.Log;

import androidx.annotation.NonNull;

import org.lsposed.lspd.BuildConfig;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ActivityController extends IActivityController.Stub {
    private static Constructor<?> myActivityControllerConstructor = null;
    private static Method myActivityControllerRunner = null;
    private static boolean inited = false;
    private static int fdSize = -1;

    private static IActivityController controller = null;

    static private ActivityController instance;

    static {
        try {
            Context ctx = ActivityThread.currentActivityThread().getSystemContext();
            var systemClassLoader = ctx.getClassLoader();
            @SuppressLint("PrivateApi") var myActivityControllerClass = Class.forName("com.android.server.am.ActivityManagerShellCommand$MyActivityController", false, systemClassLoader);
            try {
                myActivityControllerConstructor = myActivityControllerClass.getDeclaredConstructor(IActivityManager.class, PrintWriter.class, InputStream.class,
                        String.class, boolean.class);
            } catch (NoSuchMethodException e1) {
                try {
                    myActivityControllerConstructor = myActivityControllerClass.getDeclaredConstructor(IActivityManager.class, PrintWriter.class, InputStream.class,
                            String.class, boolean.class, boolean.class, String.class, boolean.class);
                } catch (NoSuchMethodException e2) {
                    myActivityControllerConstructor = myActivityControllerClass.getDeclaredConstructor(IActivityManager.class, PrintWriter.class, InputStream.class,
                            String.class, boolean.class, boolean.class, String.class, boolean.class, boolean.class);
                }
            }
            myActivityControllerConstructor.setAccessible(true);
            myActivityControllerRunner = myActivityControllerClass.getDeclaredMethod("run");
            myActivityControllerRunner.setAccessible(true);
            var tmp = Parcel.obtain();
            tmp.writeFileDescriptor(FileDescriptor.in);
            fdSize = tmp.dataPosition();
            tmp.recycle();
            inited = true;
        } catch (Throwable e) {
            Log.e(TAG, "Failed to init ActivityController", e);
        }
    }

    private ActivityController() {
        instance = this;
    }

    static private @NonNull
    ActivityController getInstance() {
        if (instance == null) new ActivityController();
        return instance;
    }

    static boolean replaceShellCommand(IBinder am, Parcel data, Parcel reply) {
        if (!inited) return false;
        try {
            data.setDataPosition(fdSize * 3);
            String[] args = data.createStringArray();

            if (args.length > 0 && "monitor".equals(args[0])) {
                data.setDataPosition(0);
                try (var in = data.readFileDescriptor();
                     var out = data.readFileDescriptor();
                     var err = data.readFileDescriptor()) {
                    data.createStringArray();
                    ShellCallback shellCallback = ShellCallback.CREATOR.createFromParcel(data);
                    ResultReceiver resultReceiver = ResultReceiver.CREATOR.createFromParcel(data);
                    new ShellCommand() {
                        @Override
                        public int onCommand(String cmd) {
                            final PrintWriter pw = getOutPrintWriter();
                            String opt;
                            String gdbPort = null;
                            boolean monkey = false;
                            boolean simpleMode = false;
                            String target = null;
                            boolean alwaysContinue = false;
                            boolean alwaysKill = false;
                            while ((opt = getNextOption()) != null) {
                                if (opt.equals("--gdb")) {
                                    gdbPort = getNextArgRequired();
                                } else if (opt.equals("-m")) {
                                    monkey = true;
                                } else if (myActivityControllerConstructor.getParameterCount() == 8) {
                                    switch (opt) {
                                        case "-p":
                                            target = getNextArgRequired();
                                            break;
                                        case "-s":
                                            simpleMode = true;
                                            break;
                                        case "-c":
                                            alwaysContinue = true;
                                            break;
                                    }
                                } else if (myActivityControllerConstructor.getParameterCount() > 8) {
                                    switch (opt) {
                                        case "-k":
                                            alwaysKill = true;
                                            break;
                                    }
                                } else {
                                    getErrPrintWriter().println("Error: Unknown option: " + opt);
                                    return -1;
                                }
                            }

                            return replaceMyControllerActivity(pw, getRawInputStream(), gdbPort, monkey, simpleMode, target, alwaysContinue, alwaysKill);
                        }

                        @Override
                        public void onHelp() {

                        }
                    }.exec((Binder) am, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args, shellCallback, resultReceiver);
                } catch (Throwable e) {
                    Log.e(TAG, "replace shell command", e);
                } finally {
                    if (reply != null) reply.writeNoException();
                }
                return true;
            }
        } finally {
            data.setDataPosition(0);
        }
        return false;
    }

    static boolean replaceActivityController(Parcel data) {
        if (!inited) return false;
        try {
            var position = data.dataPosition();
            var controller = replaceActivityController(IActivityController.Stub.asInterface(data.readStrongBinder()));
            var b = data.readInt();
            data.setDataSize(position);
            data.setDataPosition(position);
            data.writeStrongInterface(controller);
            data.writeInt(b);
        } catch (Throwable e) {
            Log.e(TAG, "replace activity controller", e);
        } finally {
            data.setDataPosition(0);
        }
        return false;
    }

    static private int replaceMyControllerActivity(PrintWriter pw, InputStream stream, String gdbPort, boolean monkey, boolean simpleMode, String target, boolean alwaysContinue, boolean alwaysKill) {
        try {
            InvocationHandler handler = (proxy, method, args1) -> {
                if (method.getName().equals("setActivityController")) {
                    try {
                        args1[0] = replaceActivityController((IActivityController) args1[0]);
                    } catch (Throwable e) {
                        Log.e(TAG, "replace activity controller", e);
                    }
                }
                return method.invoke(ServiceManager.getService("activity"), args1);
            };
            var amProxy = Proxy.newProxyInstance(BridgeService.class.getClassLoader(),
                    new Class[]{myActivityControllerConstructor.getParameterTypes()[0]}, handler);
            Object ctrl;
            if (myActivityControllerConstructor.getParameterCount() == 5) {
                ctrl = myActivityControllerConstructor.newInstance(amProxy, pw, stream, gdbPort, monkey);
            } else if (myActivityControllerConstructor.getParameterCount() == 8){
                ctrl = myActivityControllerConstructor.newInstance(amProxy, pw, stream, gdbPort, monkey, simpleMode, target, alwaysContinue);
            } else {
                ctrl = myActivityControllerConstructor.newInstance(amProxy, pw, stream, gdbPort, monkey, simpleMode, target, alwaysContinue, alwaysKill);
            }
            myActivityControllerRunner.invoke(ctrl);
            return 0;
        } catch (Throwable e) {
            Log.e(TAG, "run monitor", e);
            return 1;
        }
    }

    static private IActivityController replaceActivityController(IActivityController controller) {
        Log.d(TAG, "android.app.IActivityManager.setActivityController is called");
        ActivityController.controller = controller;
        return getInstance();
    }

    @Override
    public boolean activityStarting(Intent intent, String pkg) {
        Log.d(TAG, "activity from " + pkg + " with " + intent + " with extras " + intent.getExtras() + " is starting");
        var snapshot = BridgeService.getService();
        if (snapshot != null && BuildConfig.MANAGER_INJECTED_PKG_NAME.equals(pkg)) {
            try {
                return snapshot.preStartManager(pkg, intent);
            } catch (Throwable e) {
                Log.e(TAG, "request manager", e);
            }
        }
        return controller == null || controller.activityStarting(intent, pkg);
    }

    @Override
    public boolean activityResuming(String pkg) {
        return controller == null || controller.activityResuming(pkg);
    }

    @Override
    public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
        return controller == null || controller.appCrashed(processName, pid, shortMsg, longMsg, timeMillis, stackTrace);
    }

    @Override
    public int appEarlyNotResponding(String processName, int pid, String annotation) {
        return controller == null ? 0 : controller.appNotResponding(processName, pid, annotation);
    }

    @Override
    public int appNotResponding(String processName, int pid, String processStats) {
        return controller == null ? 0 : controller.appNotResponding(processName, pid, processStats);
    }

    @Override
    public int systemNotResponding(String msg) {
        return controller == null ? -1 : controller.systemNotResponding(msg);
    }

    @Override
    public boolean moveTaskToFront(String pkg, int task, int flags, Bundle options) {
        return controller == null || controller.moveTaskToFront(pkg, task, flags, options);
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
