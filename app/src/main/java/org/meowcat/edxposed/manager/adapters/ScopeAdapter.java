package org.meowcat.edxposed.manager.adapters;

import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.ui.activity.AppListActivity;
import org.meowcat.edxposed.manager.ui.widget.MasterSwitch;

import java.util.ArrayList;
import java.util.List;

public class ScopeAdapter extends AppAdapter {

    protected final String modulePackageName;
    protected boolean enabled = true;
    private List<String> checkedList;
    private final MasterSwitch masterSwitch;

    public ScopeAdapter(AppListActivity activity, String modulePackageName, MasterSwitch masterSwitch) {
        super(activity);
        this.modulePackageName = modulePackageName;
        this.masterSwitch = masterSwitch;
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
        activity.runOnUiThread(() -> masterSwitch.setChecked(enabled));
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
            activity.makeSnackBar(R.string.add_package_failed, Snackbar.LENGTH_SHORT);
            if (!isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
            view.setChecked(!isChecked);
        }
    }
}
