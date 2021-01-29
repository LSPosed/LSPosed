package io.github.lsposed.manager.adapters;

import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.AppListActivity;


public class WhiteListAdapter extends AppAdapter {

    private List<String> checkedList;

    public WhiteListAdapter(AppListActivity activity) {
        super(activity);
    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        return checkedList = AppHelper.getAppList();
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, String packageName) {
        boolean success = AppHelper.setPackageAppList(packageName, isChecked);
        if (success) {
            if (isChecked) {
                checkedList.add(packageName);
            } else {
                checkedList.remove(packageName);
            }
        } else {
            activity.makeSnackBar(R.string.add_package_failed, Snackbar.LENGTH_SHORT);
            view.setChecked(!isChecked);
        }
    }
}
