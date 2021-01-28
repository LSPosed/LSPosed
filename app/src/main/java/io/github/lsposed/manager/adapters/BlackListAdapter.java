package io.github.lsposed.manager.adapters;

import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.AppListActivity;

import java.util.List;


public class BlackListAdapter extends AppAdapter {

    private List<String> checkedList;

    public BlackListAdapter(AppListActivity activity) {
        super(activity);
    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        return checkedList = AppHelper.getAppList(AppHelper.isWhiteListMode());
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        boolean success = AppHelper.setPackageAppList(info.packageName, isChecked);
        if (success) {
            if (isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
        } else {
            activity.makeSnackBar(R.string.add_package_failed, Snackbar.LENGTH_SHORT);
            view.setChecked(!isChecked);
        }
    }
}
