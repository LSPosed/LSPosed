package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.os.Build;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;

import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.BuildConfig;

import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuSystemProperties;
import rikka.shizuku.SystemServiceHelper;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class RebootUtil {
    public enum RebootType {
        NORMAL,
        USERSPACE,
        RECOVERY,
        BOOTLOADER,
        DOWNLOAD,
        EDL;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, int grantResult) {
        RebootType mode = RebootType.values()[requestCode];
        if (grantResult == PERMISSION_GRANTED) {
            try {
                if (mode == RebootType.USERSPACE && !supportUserspaceReboot()) {
                    ShizukuSystemProperties.set("ctl.restart", "surfaceflinger");
                    ShizukuSystemProperties.set("ctl.restart", "zygote");
                } else {
                    POWER_MANAGER.get().reboot(BuildConfig.DEBUG, mode.toString(), false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                rebootWithShell(mode);
            }
        } else {
            rebootWithShell(mode);
        }
    }

    private static final Singleton<IPowerManager> POWER_MANAGER = new Singleton<IPowerManager>() {
        @Override
        protected IPowerManager create() {
            return IPowerManager.Stub.asInterface(new ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.POWER_SERVICE)));
        }
    };


    public static void reboot(RebootType mode) {
        onRequestPermissionsResult(mode.ordinal(), App.checkPermission(mode.ordinal()) ? PERMISSION_GRANTED : PERMISSION_DENIED);

    }

    private static void rebootWithShell(RebootType mode) {
        if (!Shell.rootAccess()) {
            return;
        }
        String command;
        if (mode == RebootType.USERSPACE) {
            if (supportUserspaceReboot()) {
                command = "/system/bin/svc power reboot userspace";
            } else {
                command = "setprop ctl.restart surfaceflinger; setprop ctl.restart zygote";
            }
        } else if (mode == RebootType.NORMAL) {
            command = "/system/bin/svc power reboot";
        } else if (mode == RebootType.RECOVERY) {
            Shell.su("touch /cache/recovery/boot").exec();
            command = "/system/bin/svc power recovery";
        } else {
            command = "/system/bin/svc power reboot " + mode.toString();
        }
        Shell.su(command).exec();
    }

    private static boolean supportUserspaceReboot() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && ((PowerManager) App.getInstance().getSystemService(Context.POWER_SERVICE)).isRebootingUserspaceSupported();
    }
}