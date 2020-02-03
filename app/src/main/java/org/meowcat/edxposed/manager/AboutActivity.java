package org.meowcat.edxposed.manager;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.util.NavUtil;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class AboutActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        View changelogView = findViewById(R.id.changelogView);
        View licensesView = findViewById(R.id.licensesView);
        View translatorsView = findViewById(R.id.translatorsView);
        View sourceCodeView = findViewById(R.id.sourceCodeView);
        View tgChannelView = findViewById(R.id.tgChannelView);
        View installerSupportView = findViewById(R.id.installerSupportView);
        View faqView = findViewById(R.id.faqView);
        View donateView = findViewById(R.id.donateView);
        TextView txtModuleSupport = findViewById(R.id.tab_support_module_description);
        View qqGroupView = findViewById(R.id.qqGroupView);
        View tgGroupView = findViewById(R.id.tgGroupView);

        String packageName = getPackageName();
        String translator = getResources().getString(R.string.translator);

        SharedPreferences prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);

        final String changes = prefs.getString("changelog", null);

        if (changes == null) {
            changelogView.setVisibility(View.GONE);
        } else {
            changelogView.setOnClickListener(v1 -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.changes)
                    .setMessage(Html.fromHtml(changes))
                    .setPositiveButton(android.R.string.ok, null).show());
        }

        try {
            String version = getPackageManager().getPackageInfo(packageName, 0).versionName;
            ((TextView) findViewById(R.id.app_version)).setText(version);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        licensesView.setOnClickListener(v12 -> createLicenseDialog());

        txtModuleSupport.setText(getString(R.string.support_modules_description,
                getString(R.string.module_support)));

        setupView(installerSupportView, R.string.support_material_xda);
        setupView(faqView, R.string.support_faq_url);
        setupView(tgGroupView, R.string.group_telegram_link);
        setupView(qqGroupView, R.string.group_qq_link);
        setupView(donateView, R.string.support_donate_url);
        setupView(sourceCodeView, R.string.about_source);
        setupView(tgChannelView, R.string.group_telegram_channel_link);

        if (translator.isEmpty()) {
            translatorsView.setVisibility(View.GONE);
        }
    }

    void setupView(View v, final int url) {
        v.setOnClickListener(v1 -> NavUtil.startURL(this, getString(url)));
    }

    private void createLicenseDialog() {
        Notices notices = new Notices();
        notices.addNotice(new Notice("material-dialogs", "https://github.com/afollestad/material-dialogs", "Copyright (c) 2014-2016 Aidan Michael Follestad", new MITLicense()));
        notices.addNotice(new Notice("StickyListHeaders", "https://github.com/emilsjolander/StickyListHeaders", "Emil Sjölander", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("PreferenceFragment-Compat", "https://github.com/Machinarius/PreferenceFragment-Compat", "machinarius", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("libsuperuser", "https://github.com/Chainfire/libsuperuser", "Copyright (C) 2012-2015 Jorrit \"Chainfire\" Jongma", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("picasso", "https://github.com/square/picasso", "Copyright 2013 Square, Inc.", new ApacheSoftwareLicense20()));

        new LicensesDialog.Builder(this)
                .setNotices(notices)
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }

    public void openLink(View view) {
        NavUtil.startURL(this, view.getTag().toString());
    }

}
