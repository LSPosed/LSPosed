package io.github.lsposed.manager.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.FragmentCompileDialogBinding;
import io.github.lsposed.manager.util.CompileUtil;
import io.github.lsposed.manager.util.ToastUtil;
import rikka.shizuku.ShizukuSystemProperties;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

@SuppressWarnings("deprecation")
public class CompileDialogFragment extends AppCompatDialogFragment {

    // TODO:
    private static final String COMPILE_COMMAND_PREFIX = "cmd package ";
    private static final String COMPILE_RESET_COMMAND = COMPILE_COMMAND_PREFIX + "compile --reset ";

    private static final String KEY_APP_INFO = "app_info";
    private static final String KEY_MSG = "msg";
    private static final String KEY_TYPE = "type";
    private ApplicationInfo appInfo;


    public CompileDialogFragment() {
    }

    public static CompileDialogFragment newInstance(ApplicationInfo appInfo,
                                                    String msg, CompileUtil.CompileType type) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(KEY_APP_INFO, appInfo);
        arguments.putString(KEY_MSG, msg);
        arguments.putInt(KEY_TYPE, type.ordinal());
        CompileDialogFragment fragment = new CompileDialogFragment();
        fragment.setArguments(arguments);
        fragment.setCancelable(false);
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("arguments should not be null.");
        }
        appInfo = arguments.getParcelable(KEY_APP_INFO);
        if (appInfo == null) {
            throw new IllegalStateException("appInfo should not be null.");
        }
        String msg = arguments.getString(KEY_MSG, getString(R.string.compile_speed_msg));
        final PackageManager pm = requireContext().getPackageManager();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(appInfo.loadIcon(pm))
                .setTitle(appInfo.loadLabel(pm))
                .setCancelable(false);
        FragmentCompileDialogBinding binding = FragmentCompileDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false);
        builder.setView(binding.getRoot());
        binding.message.setText(msg);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    public void onRequestPermissionsResult(int requestCode, int grantResult) {
        CompileUtil.CompileType mode = CompileUtil.CompileType.values()[requestCode];
        if (grantResult == PERMISSION_GRANTED) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                try {
                    boolean checkProfiles = ShizukuSystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
                    if (mode == CompileUtil.CompileType.RESET) {
                        CompileUtil.PACKAGE_MANAGER.get().clearApplicationProfileData(appInfo.packageName);
                        String filter = ShizukuSystemProperties.get("pm.dexopt.install");
                        CompileUtil.PACKAGE_MANAGER.get().performDexOptMode(appInfo.packageName, checkProfiles, filter, true, true, null);
                    }
                    App.runOnUiThread(() -> {
                        ToastUtil.showLongToast(App.getInstance(), R.string.done);
                        try {
                            dismissAllowingStateLoss();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    compileWithShell(mode);
                }
            });
        } else {
            compileWithShell(mode);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            int type = arguments.getInt(KEY_TYPE);
            appInfo = arguments.getParcelable(KEY_APP_INFO);
            int result = App.checkPermission(type);
            switch (result) {
                case 0:
                    onRequestPermissionsResult(type, PERMISSION_GRANTED);
                    break;
                case -2:
                    onRequestPermissionsResult(type, PERMISSION_DENIED);
                    break;
            }
        } else {
            try {
                dismissAllowingStateLoss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void compileWithShell(CompileUtil.CompileType type) {
        String command = null;
        if (type == CompileUtil.CompileType.RESET) {
            command = COMPILE_RESET_COMMAND + appInfo.packageName;
        }
        if (command != null) {
            new CompileTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, command);
        } else {
            try {
                dismissAllowingStateLoss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class CompileTask extends AsyncTask<String, Void, String> {

        WeakReference<CompileDialogFragment> outerRef;

        CompileTask(CompileDialogFragment fragment) {
            outerRef = new WeakReference<>(fragment);
        }

        @Override
        protected String doInBackground(String... commands) {
            if (outerRef.get() == null) {
                return App.getInstance().getString(R.string.compile_failed);
            }
            // Also get STDERR
            List<String> stdout = new ArrayList<>();
            List<String> stderr = new ArrayList<>();
            Shell.Result result = Shell.su(commands).to(stdout, stderr).exec();
            if (stderr.size() > 0) {
                return "Error: " + TextUtils.join("\n", stderr);
            } else if (!result.isSuccess()) { // they might don't write to stderr
                return "Error: " + TextUtils.join("\n", stdout);
            } else {
                return TextUtils.join("\n", stdout);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                outerRef.get().dismissAllowingStateLoss();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Context context = App.getInstance();
            if (result.length() == 0) {
                ToastUtil.showLongToast(context, R.string.compile_failed);
            } else if (result.length() >= 5 && "Error".equals(result.substring(0, 5))) {
                ToastUtil.showLongToast(context, context.getString(R.string.compile_failed_with_info) + " " + result.substring(6));
            } else {
                ToastUtil.showLongToast(context, R.string.done);
            }
        }
    }
}
