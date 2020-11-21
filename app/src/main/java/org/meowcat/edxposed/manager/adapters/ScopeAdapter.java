package org.meowcat.edxposed.manager.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

public class ScopeAdapter extends AppAdapter {

    protected final String modulePackageName;
    private List<String> checkedList;

    public ScopeAdapter(Context context, String modulePackageName) {
        super(context);
        this.modulePackageName = modulePackageName;
    }

    @Override
    public List<String> generateCheckedList() {
        AppHelper.makeSurePath();
        List<String> scopeList = AppHelper.getScopeList(modulePackageName);
        List<String> list = new ArrayList<>();
        for (ApplicationInfo info : fullList) {
            list.add(info.packageName);
        }
        scopeList.retainAll(list);
        checkedList = scopeList;
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        if (isChecked) {
            checkedList.add(info.packageName);
        } else {
            checkedList.remove(info.packageName);
        }
        if (!AppHelper.saveScopeList(modulePackageName, checkedList)) {
            ToastUtil.showShortToast(context, R.string.add_package_failed);
            if (!isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
            view.setChecked(!isChecked);
        }
    }
}
