package org.meowcat.edxposed.manager.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.ui.fragment.CompileDialogFragment;

import java.util.List;

import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuSystemProperties;
import rikka.shizuku.SystemServiceHelper;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

@SuppressWarnings("deprecation")
public class CompileUtil {

    public enum CompileType {
        RESET,
        SPEED,
        DEXOPT,
        SPEED_ALL,
        DEXOPT_ALL;

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

    @SuppressLint("StaticFieldLeak")
    private static Activity sActivity;

    public static void onRequestPermissionsResult(int requestCode, int grantResult) {
        CompileUtil.CompileType mode = CompileUtil.CompileType.values()[(requestCode - 1) / 10];
        if (mode == CompileType.DEXOPT_ALL || mode == CompileType.SPEED_ALL) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(sActivity)
                    .setTitle(R.string.speed_now)
                    .setMessage(R.string.this_may_take_a_while)
                    .setCancelable(false)
                    .show();
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                if (grantResult == PERMISSION_GRANTED) {
                    try {
                        IPackageManager packageManager = PACKAGE_MANAGER.get();
                        if (mode == CompileType.DEXOPT_ALL) {
                            packageManager.runBackgroundDexoptJob(null);
                        } else {
                            boolean checkProfiles = ShizukuSystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
                            List<String> list = packageManager.getAllPackages();
                            for (String packageName : list) {
                                CompileUtil.PACKAGE_MANAGER.get().performDexOptMode(packageName, checkProfiles, "speed", true, true, null);
                            }
                        }
                        dialog.dismiss();
                        App.runOnUiThread(() -> Toast.makeText(sActivity, R.string.done, Toast.LENGTH_LONG).show());
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (!Shell.rootAccess()) {
                    dialog.dismiss();
                    App.runOnUiThread(() -> Toast.makeText(sActivity, R.string.root_failed, Toast.LENGTH_LONG).show());
                    return;
                }

                if (mode == CompileType.SPEED_ALL) {
                    Shell.su("cmd package compile -m speed -a").exec();
                } else {
                    Shell.su("cmd package bg-dexopt-job").exec();
                }

                dialog.dismiss();
                App.runOnUiThread(() -> Toast.makeText(sActivity, R.string.done, Toast.LENGTH_LONG).show());
            });
        } else if (instance != null) {
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

    public static void compileAllDexopt(Activity activity) {
        sActivity = activity;
        int type = CompileType.DEXOPT_ALL.ordinal() * 10 + 1;
        onRequestPermissionsResult(type, App.checkPermission(type) ? PERMISSION_GRANTED : PERMISSION_DENIED);
    }

    public static void compileAllSpeed(Activity activity) {
        sActivity = activity;
        int type = CompileType.SPEED_ALL.ordinal() * 10 + 1;
        onRequestPermissionsResult(type, App.checkPermission(type) ? PERMISSION_GRANTED : PERMISSION_DENIED);
    }

    private static void compilePackageInBg(FragmentManager fragmentManager,
                                           ApplicationInfo info, String msg, CompileType type) {
        instance = CompileDialogFragment.newInstance(info, msg, type);
        instance.show(fragmentManager, TAG_COMPILE_DIALOG);
    }

}
