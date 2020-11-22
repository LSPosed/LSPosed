package org.meowcat.edxposed.manager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.ToastUtil;
import org.meowcat.edxposed.manager.widget.MasterSwitch;

import java.util.ArrayList;
import java.util.List;

public class ScopeAdapter extends AppAdapter {

    protected final String modulePackageName;
    protected boolean enabled = true;
    private List<String> checkedList;
    private final MasterSwitch masterSwitch;

    public ScopeAdapter(Context context, String modulePackageName, MasterSwitch masterSwitch) {
        super(context);
        this.modulePackageName = modulePackageName;
        this.masterSwitch = masterSwitch;
        masterSwitch.setTitle(context.getString(R.string.enable_scope));
        masterSwitch.setOnCheckedChangedListener(new MasterSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean checked) {
                enabled = checked;
                AppHelper.saveScopeList(modulePackageName, enabled ? checkedList : new ArrayList<>());
                notifyDataSetChanged();
            }
        });
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
        enabled = checkedList.size() != 0;
        ((Activity) context).runOnUiThread(() -> masterSwitch.setChecked(enabled));
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
