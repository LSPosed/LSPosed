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
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.FragmentCompileDialogBinding;
import io.github.lsposed.manager.util.ToastUtil;

@SuppressWarnings("deprecation")
public class CompileDialogFragment extends AppCompatDialogFragment {

    // TODO:
    private static final String[] COMPILE_RESET_COMMAND = new String[]{"cmd", "package", "compile", "-f", "-m", "speed", ""};

    private static final String KEY_APP_INFO = "app_info";
    private static final String KEY_MSG = "msg";
    private ApplicationInfo appInfo;

    public CompileDialogFragment() {
    }

    public static void speed(Context context, FragmentManager fragmentManager, ApplicationInfo info) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(KEY_APP_INFO, info);
        arguments.putString(KEY_MSG, context.getString(R.string.compile_speed_msg));
        CompileDialogFragment fragment = new CompileDialogFragment();
        fragment.setArguments(arguments);
        fragment.setCancelable(false);
        fragment.show(fragmentManager, "compile_dialog");
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            appInfo = arguments.getParcelable(KEY_APP_INFO);
            String[] command = COMPILE_RESET_COMMAND;
            command[6] = appInfo.packageName;
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
                return "";
            }
            try {
                Process process = Runtime.getRuntime().exec(commands);
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuilder err = new StringBuilder();
                while ((read = errorReader.read(buffer)) > 0) {
                    err.append(buffer, 0, read);
                }
                StringBuilder input = new StringBuilder();
                while ((read = inputReader.read(buffer)) > 0) {
                    input.append(buffer, 0, read);
                }
                errorReader.close();
                inputReader.close();
                process.waitFor();
                String result = "";
                if (process.exitValue() != 0) {
                    result = "Error ";
                }
                if (TextUtils.isEmpty(err)) {
                    return result + input.toString();
                } else {
                    return result + err.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error " + e.getCause();
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
