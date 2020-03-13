package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.ListFragment;

import com.google.android.material.snackbar.Snackbar;

import org.meowcat.edxposed.manager.repo.Module;
import org.meowcat.edxposed.manager.repo.ModuleVersion;
import org.meowcat.edxposed.manager.repo.ReleaseType;
import org.meowcat.edxposed.manager.repo.RepoParser;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.HashUtil;
import org.meowcat.edxposed.manager.util.InstallApkUtil;
import org.meowcat.edxposed.manager.util.ModuleUtil.InstalledModule;
import org.meowcat.edxposed.manager.util.RepoLoader;
import org.meowcat.edxposed.manager.util.chrome.LinkTransformationMethod;
import org.meowcat.edxposed.manager.widget.DownloadView;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

public class DownloadDetailsVersionsFragment extends ListFragment {
    @SuppressLint("StaticFieldLeak")
    private DownloadDetailsActivity activity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (DownloadDetailsActivity) getActivity();
        if (activity == null) {
            return;
        }
        Module module = activity.getModule();
        if (module == null)
            return;

        if (module.versions.isEmpty()) {
            setEmptyText(getString(R.string.download_no_versions));
            setListShown(true);
        } else {
            RepoLoader repoLoader = RepoLoader.getInstance();
            if (!repoLoader.isVersionShown(module.versions.get(0))) {
                TextView txtHeader = new TextView(getActivity());
                txtHeader.setText(R.string.download_test_version_not_shown);
                txtHeader.setTextColor(ContextCompat.getColor(activity, R.color.warning));
                txtHeader.setOnClickListener(v -> activity.gotoPage(DownloadDetailsActivity.DOWNLOAD_SETTINGS));
                getListView().addHeaderView(txtHeader);
            }

            VersionsAdapter sAdapter = new VersionsAdapter(activity, activity.getInstalledModule(), activity.findViewById(R.id.snackbar));
            for (ModuleVersion version : module.versions) {
                if (repoLoader.isVersionShown(version))
                    sAdapter.add(version);
            }
            setListAdapter(sAdapter);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((FrameLayout) view).setClipChildren(false);
        ((FrameLayout) view).setClipToPadding(false);
        ((FrameLayout) getListView().getParent()).setClipChildren(false);
        ((FrameLayout) getListView().getParent()).setClipToPadding(false);
        getListView().setClipToPadding(false);
        getListView().setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            if (insets.getTappableElementInsets().bottom != insets.getSystemWindowInsetBottom()) {
                getListView().setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setListAdapter(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == 42) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        OutputStream os = activity.getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            FileInputStream in = new FileInputStream(new File(DownloadView.lastInfo.localFilename));
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                            os.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //Snackbar.make(findViewById(R.id.snackbar), getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    static class ViewHolder {
        TextView txtStatus;
        TextView txtVersion;
        TextView txtRelType;
        TextView txtUploadDate;
        DownloadView downloadView;
        TextView txtChangesTitle;
        TextView txtChanges;
    }

    public static class DownloadModuleCallback implements DownloadsUtil.DownloadFinishedCallback {
        private final ModuleVersion moduleVersion;
        private View snackbar;

        DownloadModuleCallback(ModuleVersion moduleVersion, View snackbar) {
            this.moduleVersion = moduleVersion;
            this.snackbar = snackbar;
        }

        @Override
        public void onDownloadFinished(Context context, DownloadsUtil.DownloadInfo info) {
            File localFile = new File(info.localFilename);
            if (!localFile.isFile())
                return;

            if (moduleVersion.md5sum != null && !moduleVersion.md5sum.isEmpty()) {
                try {
                    String actualMd5Sum = HashUtil.md5(localFile);
                    if (!moduleVersion.md5sum.equals(actualMd5Sum)) {
                        Snackbar.make(snackbar, context.getString(R.string.download_md5sum_incorrect, actualMd5Sum, moduleVersion.md5sum), Snackbar.LENGTH_LONG).show();
                        DownloadsUtil.removeById(context, info.id);
                        return;
                    }
                } catch (Exception e) {
                    Snackbar.make(snackbar, context.getString(R.string.download_could_not_read_file, e.getMessage()), Snackbar.LENGTH_LONG).show();
                    DownloadsUtil.removeById(context, info.id);
                    return;
                }
            }

            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(info.localFilename, 0);

            if (packageInfo == null) {
                Snackbar.make(snackbar, R.string.download_no_valid_apk, Snackbar.LENGTH_LONG).show();
                DownloadsUtil.removeById(context, info.id);
                return;
            }

            if (!packageInfo.packageName.equals(moduleVersion.module.packageName)) {
                Snackbar.make(snackbar, context.getString(R.string.download_incorrect_package_name, packageInfo.packageName, moduleVersion.module.packageName), Snackbar.LENGTH_LONG).show();
                DownloadsUtil.removeById(context, info.id);
                return;
            }

            new InstallApkUtil(context, info).execute();
        }
    }

    private class VersionsAdapter extends ArrayAdapter<ModuleVersion> {
        private final DateFormat dateFormatter = DateFormat
                .getDateInstance(DateFormat.SHORT);
        private final int colorRelTypeStable;
        private final int colorRelTypeOthers;
        private final int colorInstalled;
        private final int colorUpdateAvailable;
        private final String textInstalled;
        private final String textUpdateAvailable;
        private final long installedVersionCode;
        private View snackbar;

        VersionsAdapter(Context context, InstalledModule installed, View snackbar) {
            super(context, R.layout.item_version);
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            int color = ContextCompat.getColor(context, typedValue.resourceId);
            colorRelTypeStable = color;
            colorRelTypeOthers = ContextCompat.getColor(activity, R.color.warning);
            colorInstalled = color;
            colorUpdateAvailable = ContextCompat.getColor(activity, R.color.download_status_update_available);
            textInstalled = getString(R.string.download_section_installed) + ":";
            textUpdateAvailable = getString(R.string.download_section_update_available) + ":";
            installedVersionCode = (installed != null) ? installed.versionCode : -1;
            this.snackbar = snackbar;
        }

        @SuppressLint("InflateParams")
        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_version, null, true);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.txtStatus = view.findViewById(R.id.txtStatus);
                viewHolder.txtVersion = view.findViewById(R.id.txtVersion);
                viewHolder.txtRelType = view.findViewById(R.id.txtRelType);
                viewHolder.txtUploadDate = view.findViewById(R.id.txtUploadDate);
                viewHolder.downloadView = view.findViewById(R.id.downloadView);
                viewHolder.txtChangesTitle = view.findViewById(R.id.txtChangesTitle);
                viewHolder.txtChanges = view.findViewById(R.id.txtChanges);
                viewHolder.downloadView.fragment = DownloadDetailsVersionsFragment.this;
                view.setTag(viewHolder);
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            ModuleVersion item = getItem(position);
            if (item == null) {
                return view;
            }
            holder.txtVersion.setText(item.name);
            holder.txtRelType.setText(item.relType.getTitleId());
            holder.txtRelType.setTextColor(item.relType == ReleaseType.STABLE
                    ? colorRelTypeStable : colorRelTypeOthers);

            if (item.uploaded > 0) {
                holder.txtUploadDate.setText(
                        dateFormatter.format(new Date(item.uploaded)));
                holder.txtUploadDate.setVisibility(View.VISIBLE);
            } else {
                holder.txtUploadDate.setVisibility(View.GONE);
            }

            if (item.code <= 0 || installedVersionCode <= 0
                    || item.code < installedVersionCode) {
                holder.txtStatus.setVisibility(View.GONE);
            } else if (item.code == installedVersionCode) {
                holder.txtStatus.setText(textInstalled);
                holder.txtStatus.setTextColor(colorInstalled);
                holder.txtStatus.setVisibility(View.VISIBLE);
            } else { // item.code > installedVersionCode
                holder.txtStatus.setText(textUpdateAvailable);
                holder.txtStatus.setTextColor(colorUpdateAvailable);
                holder.txtStatus.setVisibility(View.VISIBLE);
            }

            holder.downloadView.setUrl(item.downloadLink);
            holder.downloadView.setTitle(activity.getModule().name);
            holder.downloadView.setDownloadFinishedCallback(new DownloadModuleCallback(item, snackbar));

            if (item.changelog != null && !item.changelog.isEmpty()) {
                holder.txtChangesTitle.setVisibility(View.VISIBLE);
                holder.txtChanges.setVisibility(View.VISIBLE);

                if (item.changelogIsHtml) {
                    holder.txtChanges.setText(RepoParser.parseSimpleHtml(getActivity(), item.changelog, holder.txtChanges));
                    holder.txtChanges.setTransformationMethod(new LinkTransformationMethod((BaseActivity) getActivity()));
                    holder.txtChanges.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    holder.txtChanges.setText(item.changelog);
                    holder.txtChanges.setMovementMethod(null);
                }

            } else {
                holder.txtChangesTitle.setVisibility(View.GONE);
                holder.txtChanges.setVisibility(View.GONE);
            }

            return view;
        }
    }
}