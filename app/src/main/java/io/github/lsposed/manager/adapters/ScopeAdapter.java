package io.github.lsposed.manager.adapters;

import android.widget.CompoundButton;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.AppListActivity;
import io.github.lsposed.manager.ui.widget.MasterSwitch;
import io.github.lsposed.manager.util.ModuleUtil;

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
                ModuleUtil.getInstance().setModuleEnabled(modulePackageName, enabled);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public List<String> generateCheckedList() {
        checkedList = AppHelper.getScopeList(modulePackageName);
        enabled = ModuleUtil.getInstance().isModuleEnabled(modulePackageName);
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
