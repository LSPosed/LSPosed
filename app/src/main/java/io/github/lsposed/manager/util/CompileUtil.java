package io.github.lsposed.manager.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;

import androidx.fragment.app.FragmentManager;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.fragment.CompileDialogFragment;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

public class CompileUtil {
    // TODO:

    public enum CompileType {
        RESET,
        SPEED,
        DEXOPT;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static CompileDialogFragment instance;

    private static final String TAG_COMPILE_DIALOG = "compile_dialog";

    public static final Singleton<IPackageManager> PACKAGE_MANAGER = new Singleton<IPackageManager>() {
        @Override
        protected IPackageManager create() {
            return IPackageManager.Stub.asInterface(new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));
        }
    };

    public static void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (instance != null) {
            instance.onRequestPermissionsResult(requestCode, grantResult);
        }
    }

    public static void reset(Context context, FragmentManager fragmentManager,
                             ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_reset_msg), CompileType.RESET);
    }

    public static void compileSpeed(Context context, FragmentManager fragmentManager,
                                    ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_speed_msg), CompileType.SPEED);
    }

    public static void compileDexopt(Context context, FragmentManager fragmentManager,
                                     ApplicationInfo info) {
        compilePackageInBg(fragmentManager, info,
                context.getString(R.string.compile_speed_msg), CompileType.DEXOPT);
    }

    private static void compilePackageInBg(FragmentManager fragmentManager,
                                           ApplicationInfo info, String msg, CompileType type) {
        instance = CompileDialogFragment.newInstance(info, msg, type);
        instance.show(fragmentManager, TAG_COMPILE_DIALOG);
    }

}
