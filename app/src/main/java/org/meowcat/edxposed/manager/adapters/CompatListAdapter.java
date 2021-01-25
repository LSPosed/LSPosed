package org.meowcat.edxposed.manager.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.ToastUtil;

import java.util.List;

public class CompatListAdapter extends AppAdapter {

    private List<String> checkedList;

    public CompatListAdapter(Context context) {
        super(context);
    }

    @Override
    protected List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        return checkedList = AppHelper.getCompatList();
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        boolean success = isChecked ?
                AppHelper.addCompatList(info.packageName) : AppHelper.removeCompatList(info.packageName);
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
