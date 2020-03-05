package org.meowcat.edxposed.manager;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.json.XposedTab;
import org.meowcat.edxposed.manager.util.json.XposedZip;

import java.util.List;
import java.util.Objects;

public class BaseAdvancedInstaller extends Fragment {
    private View mClickedButton;

    static BaseAdvancedInstaller newInstance(XposedTab tab) {
        BaseAdvancedInstaller myFragment = new BaseAdvancedInstaller();

        Bundle args = new Bundle();
        args.putParcelable("tab", tab);
        myFragment.setArguments(args);

        return myFragment;
    }

    private List<XposedZip> installers() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).installers;
    }

    private List<XposedZip> uninstallers() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).uninstallers;
    }

    private String notice() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).notice;
    }

    protected String author() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).author;
    }

    private String supportUrl() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).support;
    }

    protected boolean isStable() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).stable;
    }

    private boolean isOfficial() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).official;
    }

    private String description() {
        XposedTab tab = Objects.requireNonNull(getArguments()).getParcelable("tab");
        return Objects.requireNonNull(tab).description;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.single_installer_view, container, false);

        final Spinner chooserInstallers = view.findViewById(R.id.chooserInstallers);
        final Spinner chooserUninstallers = view.findViewById(R.id.chooserUninstallers);
        final Button btnInstall = view.findViewById(R.id.btnInstall);
        final Button btnUninstall = view.findViewById(R.id.btnUninstall);
        ImageView infoInstaller = view.findViewById(R.id.infoInstaller);
        ImageView infoUninstaller = view.findViewById(R.id.infoUninstaller);
        TextView noticeTv = view.findViewById(R.id.noticeTv);
        TextView author = view.findViewById(R.id.author);
        View showOnXda = view.findViewById(R.id.show_on_xda);
        View updateDescription = view.findViewById(R.id.updateDescription);

        try {
            chooserInstallers.setAdapter(new XposedZip.MyAdapter(getContext(), installers()));
            chooserUninstallers.setAdapter(new XposedZip.MyAdapter(getContext(), uninstallers()));
        } catch (Exception ignored) {
        }
        infoInstaller.setOnClickListener(v -> {
            XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();
            String s = getString(R.string.infoInstaller,
                    selectedInstaller.name,
                    selectedInstaller.version);

            new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext())).setTitle(R.string.info)
                    .setMessage(s).setPositiveButton(android.R.string.ok, null).show();
        });
        infoUninstaller.setOnClickListener(v -> {
            XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();
            String s = getString(R.string.infoUninstaller,
                    selectedUninstaller.name,
                    selectedUninstaller.version);

            new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext())).setTitle(R.string.info)
                    .setMessage(s).setPositiveButton(android.R.string.ok, null).show();
        });

        btnInstall.setOnClickListener(v -> areYouSure(R.string.warningArchitecture,
                (dialog, which) -> {
                    XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();
                    Uri uri = Uri.parse(selectedInstaller.link);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }));

        btnUninstall.setOnClickListener(v -> areYouSure(R.string.warningArchitecture,
                (dialog, which) -> {
                    XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();
                    Uri uri = Uri.parse(selectedUninstaller.link);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }));

        noticeTv.setText(Html.fromHtml(notice()));
        author.setText(getString(R.string.download_author, author()));

        try {
            if (uninstallers().size() == 0) {
                infoUninstaller.setVisibility(View.GONE);
                chooserUninstallers.setVisibility(View.GONE);
                btnUninstall.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {
        }

        if (!isStable()) {
            view.findViewById(R.id.warning_unstable).setVisibility(View.VISIBLE);
        }

        if (!isOfficial()) {
            view.findViewById(R.id.warning_unofficial).setVisibility(View.VISIBLE);
        }

        showOnXda.setOnClickListener(v -> NavUtil.startURL((AppCompatActivity) getActivity(), supportUrl()));
        updateDescription.setOnClickListener(v -> new MaterialAlertDialogBuilder(Objects.requireNonNull(getContext()))
                .setTitle(R.string.changes)
                .setMessage(Html.fromHtml(description()))
                .setPositiveButton(android.R.string.ok, null).show());

        return view;
    }


    @SuppressWarnings("SameParameterValue")
    private void areYouSure(int contentTextId, DialogInterface.OnClickListener listener) {
        new MaterialAlertDialogBuilder(Objects.requireNonNull(getActivity())).setTitle(R.string.areyousure)
                .setMessage(contentTextId)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

}