package org.meowcat.edxposed.manager.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;
import org.meowcat.edxposed.manager.databinding.ActivityBlackListBinding;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ToastUtil;

import java.util.Collection;
import java.util.List;


public class BlackListAdapter extends AppAdapter {

    private volatile boolean isWhiteListMode;
    private List<String> checkedList;

    public BlackListAdapter(Context context, boolean isWhiteListMode, ActivityBlackListBinding binding) {
        super(context, binding);
        this.isWhiteListMode = isWhiteListMode;
    }

    @Override
    public List<String> generateCheckedList() {
        if (XposedApp.getPreferences().getBoolean("hook_modules", true)) {
            Collection<ModuleUtil.InstalledModule> installedModules = ModuleUtil.getInstance().getModules().values();
            for (ModuleUtil.InstalledModule info : installedModules) {
                AppHelper.FORCE_WHITE_LIST_MODULE.add(info.packageName);
            }
        }
        AppHelper.makeSurePath();
        if (isWhiteListMode) {
            checkedList = AppHelper.getWhiteList();
        } else {
            checkedList = AppHelper.getBlackList();
        }
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        boolean success = isChecked ?
                AppHelper.addPackageName(isWhiteListMode, info.packageName) :
                AppHelper.removePackageName(isWhiteListMode, info.packageName);
        if (success) {
            if (isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
        } else {
            ToastUtil.showShortToast(context, R.string.add_package_failed);
            view.setChecked(!isChecked);
        }
    }
}
