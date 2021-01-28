package org.meowcat.edxposed.manager.adapters;

import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.ui.activity.AppListActivity;
import org.meowcat.edxposed.manager.util.ModuleUtil;

import java.util.Collection;
import java.util.List;


public class BlackListAdapter extends AppAdapter {

    private List<String> checkedList;

    public BlackListAdapter(AppListActivity activity) {
        super(activity);
    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        checkedList = AppHelper.getAppList(AppHelper.isWhiteListMode());
        return checkedList;
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
