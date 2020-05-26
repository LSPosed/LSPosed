package org.meowcat.edxposed.manager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.databinding.SingleInstallerViewBinding;
import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.json.XposedTab;
import org.meowcat.edxposed.manager.util.json.XposedZip;

import java.util.Objects;

public class BaseAdvancedInstaller extends Fragment {
    private SingleInstallerViewBinding binding;

    static BaseAdvancedInstaller newInstance(XposedTab tab) {
        BaseAdvancedInstaller myFragment = new BaseAdvancedInstaller();

        Bundle args = new Bundle();
        args.putParcelable("tab", tab);
        myFragment.setArguments(args);

        return myFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments == null) {
            return null;
        } else if (arguments.getParcelable("tab") == null) {
            return null;
        }

        XposedTab tab = arguments.getParcelable("tab");

        if (tab == null) {
            return null;
        }

        binding = SingleInstallerViewBinding.inflate(inflater, container, false);
        TooltipCompat.setTooltipText(binding.infoInstaller, getString(R.string.info));
        TooltipCompat.setTooltipText(binding.infoUninstaller, getString(R.string.info));
        try {
            binding.chooserInstallers.setAdapter(new XposedZip.MyAdapter(getContext(), tab.installers));
            binding.chooserUninstallers.setAdapter(new XposedZip.MyAdapter(getContext(), tab.uninstallers));
        } catch (Exception ignored) {
        }
        binding.infoInstaller.setOnClickListener(v -> {
            XposedZip selectedInstaller = (XposedZip) binding.chooserInstallers.getSelectedItem();
            String s = getString(R.string.infoInstaller,
                    selectedInstaller.name,
                    selectedInstaller.version);

            new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext())).setTitle(R.string.info)
                    .setMessage(s).setPositiveButton(android.R.string.ok, null).show();
        });
        binding.infoUninstaller.setOnClickListener(v -> {
            XposedZip selectedUninstaller = (XposedZip) binding.chooserUninstallers.getSelectedItem();
            String s = getString(R.string.infoUninstaller,
                    selectedUninstaller.name,
                    selectedUninstaller.version);

            new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext())).setTitle(R.string.info)
                    .setMessage(s).setPositiveButton(android.R.string.ok, null).show();
        });

        binding.btnInstall.setOnClickListener(v -> warningArchitecture(
                (dialog, which) -> {
                    XposedZip selectedInstaller = (XposedZip) binding.chooserInstallers.getSelectedItem();
                    Uri uri = Uri.parse(selectedInstaller.link);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }, tab.description));

        binding.btnUninstall.setOnClickListener(v -> warningArchitecture(
                (dialog, which) -> {
                    XposedZip selectedUninstaller = (XposedZip) binding.chooserUninstallers.getSelectedItem();
                    Uri uri = Uri.parse(selectedUninstaller.link);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }, tab.description));

        binding.noticeTv.setText(HtmlCompat.fromHtml(tab.notice, HtmlCompat.FROM_HTML_MODE_LEGACY));
        binding.author.setText(getString(R.string.download_author, tab.author));

        try {
            if (tab.uninstallers.size() == 0) {
                binding.infoUninstaller.setVisibility(View.GONE);
                binding.chooserUninstallers.setVisibility(View.GONE);
                binding.btnUninstall.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {
        }

        if (!tab.stable) {
            binding.warningUnstable.setVisibility(View.VISIBLE);
        }

        if (!tab.official) {
            binding.warningUnofficial.setVisibility(View.VISIBLE);
        }

        binding.showOnXda.setOnClickListener(v -> NavUtil.startURL((BaseActivity) getActivity(), tab.support));
        binding.updateDescription.setOnClickListener(v -> new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext()))
                .setTitle(R.string.changes)
                .setMessage(HtmlCompat.fromHtml(tab.description, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(android.R.string.ok, null).show());

        return binding.getRoot();
    }

    private void warningArchitecture(DialogInterface.OnClickListener listener, String description) {
        Activity activity = getActivity();
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.warningArchitecture));
        sb.append("\n\n");
        sb.append(getString(R.string.changes));
        sb.append("\n");
        sb.append(HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY));
        if (activity != null) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.areyousure)
                    .setMessage(sb.toString())
                    .setPositiveButton(android.R.string.yes, listener)
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }
}