package io.github.lsposed.manager.adapters;

import android.content.pm.PackageInfo;
import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.AppListActivity;
import io.github.lsposed.manager.ui.widget.MasterSwitch;

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
        for (PackageInfo info : fullList) {
            list.add(info.packageName);
        }
        scopeList.retainAll(list);
        checkedList = scopeList;
        enabled = checkedList.size() != 0;
        activity.runOnUiThread(() -> masterSwitch.setChecked(enabled));
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, String packageName) {
        if (isChecked) {
            checkedList.add(packageName);
        } else {
            checkedList.remove(packageName);
        }
        if (!AppHelper.saveScopeList(modulePackageName, checkedList)) {
            activity.makeSnackBar(R.string.add_package_failed, Snackbar.LENGTH_SHORT);
            if (!isChecked) {
                checkedList.add(packageName);
            } else {
                checkedList.remove(packageName);
            }
            view.setChecked(!isChecked);
        }
    }
}
