package org.meowcat.edxposed.manager;

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

import org.meowcat.edxposed.manager.databinding.FragmentCompileDialogBinding;
import org.meowcat.edxposed.manager.util.ToastUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class CompileDialogFragment extends AppCompatDialogFragment {

    private static final String KEY_APP_INFO = "app_info";
    private static final String KEY_MSG = "msg";
    private static final String KEY_COMMANDS = "commands";
    private ApplicationInfo appInfo;


    public CompileDialogFragment() {
    }

    public static CompileDialogFragment newInstance(ApplicationInfo appInfo,
                                                    String msg, String[] commands) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(KEY_APP_INFO, appInfo);
        arguments.putString(KEY_MSG, msg);
        arguments.putStringArray(KEY_COMMANDS, commands);
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            String[] commandPrefixes = arguments.getStringArray(KEY_COMMANDS);
            appInfo = arguments.getParcelable(KEY_APP_INFO);
            if (commandPrefixes == null || commandPrefixes.length == 0 || appInfo == null) {
                ToastUtil.showShortToast(context, R.string.empty_param);
                dismissAllowingStateLoss();
                return;
            }
            String[] commands = new String[commandPrefixes.length];
            for (int i = 0; i < commandPrefixes.length; i++) {
                commands[i] = commandPrefixes[i] + appInfo.packageName;
            }
            new CompileTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, commands);
        } else {
            dismissAllowingStateLoss();
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
                return outerRef.get().requireContext().getString(R.string.compile_failed);
            }
            // Also get STDERR
            List<String> stdout = new ArrayList<>();
            List<String> stderr = new ArrayList<>();
            Shell.Result result = Shell.su(commands).to(stdout, stderr).exec();
            List<String> ret;
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
            if (outerRef.get() == null || !outerRef.get().isAdded()) {
                return;
            }
            Context ctx = outerRef.get().requireContext();
            if (result.length() == 0) {
                ToastUtil.showLongToast(ctx, R.string.compile_failed);
            } else if (result.length() >= 5 && "Error".equals(result.substring(0, 5))) {
                ToastUtil.showLongToast(ctx, ctx.getString(R.string.compile_failed_with_info) + " " + result.substring(6));
            } else {
                ToastUtil.showLongToast(ctx, R.string.done);
            }
            outerRef.get().dismissAllowingStateLoss();
        }
    }
}
