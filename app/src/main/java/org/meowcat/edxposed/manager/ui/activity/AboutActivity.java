package org.meowcat.edxposed.manager.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.ActivityAboutBinding;
import org.meowcat.edxposed.manager.util.GlideHelper;
import org.meowcat.edxposed.manager.util.NavUtil;

public class AboutActivity extends BaseActivity {
    ActivityAboutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupWindowInsets(binding.snackbar, null);

        String packageName = getPackageName();
        String translator = getResources().getString(R.string.translator);

        SharedPreferences prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);

        final String changes = prefs.getString("changelog", null);

        if (changes == null) {
            binding.changelogView.setVisibility(View.GONE);
        } else {
            binding.changelogView.setOnClickListener(v1 -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.changes)
                    .setMessage(HtmlCompat.fromHtml(changes, HtmlCompat.FROM_HTML_MODE_LEGACY))
                    .setPositiveButton(android.R.string.ok, null).show());
        }
        binding.appVersion.setText(BuildConfig.VERSION_NAME);

        binding.tabSupportModuleDescription.setText(getString(R.string.support_modules_description,
                getString(R.string.module_support)));

        setupView(binding.installerSupportView, R.string.support_material_xda);
        setupView(binding.faqView, R.string.support_faq_url);
        setupView(binding.tgGroupView, R.string.group_telegram_link);
        setupView(binding.qqGroupView, R.string.group_qq_link);
        setupView(binding.donateView, R.string.support_donate_url);
        setupView(binding.sourceCodeView, R.string.about_source);
        setupView(binding.tgChannelView, R.string.group_telegram_channel_link);

        if (translator.isEmpty()) {
            binding.translatorsView.setVisibility(View.GONE);
        }

        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(getApplicationInfo()))
                .into(binding.appIcon);
    }

    void setupView(View v, final int url) {
        v.setOnClickListener(v1 -> NavUtil.startURL(this, getString(url)));
    }

    public void openLink(View view) {
        NavUtil.startURL(this, view.getTag().toString());
    }
}
