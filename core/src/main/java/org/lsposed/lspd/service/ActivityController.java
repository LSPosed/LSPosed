package org.lsposed.lspd.service;

import static org.lsposed.lspd.service.ServiceManager.TAG;

import android.annotation.SuppressLint;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.util.Log;
import android.widget.TableRow;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ActivityController extends IActivityController.Stub {
    static final String MANAGER_INJECTED_PKG_NAME = "com.android.settings";

    private Constructor<?> myActivityControllerConstructor = null;
    private Method myActivityControllerRunner = null;
    private boolean inited = false;

    private IActivityController controller = null;
    private IActivityManager am = null;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    ActivityController(IBinder am) {
        try {
            this.am = (IActivityManager) am;
            @SuppressLint("PrivateApi") var myActivityControllerClass = Class.forName("com.android.server.am.ActivityManagerShellCommand$MyActivityController", false, am.getClass().getClassLoader());
            myActivityControllerConstructor = myActivityControllerClass.getDeclaredConstructor(IActivityManager.class, PrintWriter.class, InputStream.class,
                    String.class, boolean.class);
            myActivityControllerConstructor.setAccessible(true);
            myActivityControllerRunner = myActivityControllerClass.getDeclaredMethod("run");
            myActivityControllerRunner.setAccessible(true);
            inited = true;
        } catch (Throwable e) {
            Log.e(TAG, "Failed to init ActivityController", e);
        }
    }

    boolean replaceShellCommand(IBinder am, Parcel data) {
        if (!inited) return false;
        try {
            var in = data.readFileDescriptor();
            var out = data.readFileDescriptor();
            var err = data.readFileDescriptor();
            String[] args = data.createStringArray();
            ShellCallback shellCallback = ShellCallback.CREATOR.createFromParcel(data);
            ResultReceiver resultReceiver = ResultReceiver.CREATOR.createFromParcel(data);

            if (args.length > 0 && "monitor".equals(args[0])) {
                new ShellCommand() {
                    @Override
                    public int onCommand(String cmd) {
                        final PrintWriter pw = getOutPrintWriter();
                        String opt;
                        String gdbPort = null;
                        boolean monkey = false;
                        while ((opt = getNextOption()) != null) {
                            if (opt.equals("--gdb")) {
                                gdbPort = getNextArgRequired();
                            } else if (opt.equals("-m")) {
                                monkey = true;
                            } else {
                                getErrPrintWriter().println("Error: Unknown option: " + opt);
                                return -1;
                            }
                        }

                        return replaceMyControllerActivity(am, pw, getRawInputStream(), gdbPort, monkey);
                    }

                    @Override
                    public void onHelp() {

                    }
                }.exec((Binder) am, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args, shellCallback, resultReceiver);
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "replace shell command", e);
        } finally {
            data.setDataPosition(0);
        }
        return false;
    }

    boolean replaceActivityController(Parcel data) {
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

    private int replaceMyControllerActivity(IBinder am, PrintWriter pw, InputStream stream, String gdbPort, boolean monkey) {
        try {
            InvocationHandler handler = (proxy, method, args1) -> {
                if (method.getName().equals("setActivityController")) {
                    try {
                        args1[0] = replaceActivityController((IActivityController) args1[0]);
                    } catch (Throwable e) {
                        Log.e(TAG, "replace activity controller", e);
                    }
                }
                return method.invoke(am, args1);
            };
            var amProxy = Proxy.newProxyInstance(BridgeService.class.getClassLoader(),
                    am.getClass().getSuperclass().getInterfaces(), handler);
            var ctrl = myActivityControllerConstructor.newInstance(amProxy, pw, stream, gdbPort, monkey);
            myActivityControllerRunner.invoke(ctrl);
            return 0;
        } catch (Throwable e) {
            Log.e(TAG, "run monitor", e);
            return 1;
        }
    }

    private IActivityController replaceActivityController(IActivityController controller) {
        Log.e(TAG, "android.app.IActivityManager.setActivityController is called");
        this.controller = controller;
        return this;
    }

    @Override
    public boolean activityStarting(Intent intent, String pkg) {
        Log.d(TAG, "activity from " + pkg + " with " + intent + " with extras " + intent.getExtras() + " is starting");
        var snapshot = BridgeService.getService();
        if (snapshot != null && MANAGER_INJECTED_PKG_NAME.equals(pkg)) {
            try {
                switch (snapshot.preStartManager(pkg, intent)) {
                    case 0:
                        return true;
                    case 1: {
                        handler.post(() -> {
                            try {
                                Log.e(TAG, "force stopping for " + intent);
                                am.forceStopPackage(MANAGER_INJECTED_PKG_NAME, -1);
                            } catch (Throwable e) {
                                Log.e(TAG, "force stopping", e);
                            }
                        });
                        return true;
                    }
                    case 2:
                        return false;
                }
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
    public IBinder asBinder() {
        return this;
    }
}
