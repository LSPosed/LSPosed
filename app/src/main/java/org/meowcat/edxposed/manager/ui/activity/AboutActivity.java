package org.meowcat.edxposed.manager.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;

import com.bumptech.glide.Glide;

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

        binding.appVersion.setText(BuildConfig.VERSION_NAME);

        setupView(binding.installerSupportView, R.string.group_telegram_channel_link);
        setupView(binding.sourceCodeView, R.string.about_source);
        setupView(binding.tgChannelView, R.string.group_telegram_channel_link);

        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(getApplicationInfo()))
                .into(binding.appIcon);
    }

    void setupView(View v, final int url) {
        v.setOnClickListener(v1 -> NavUtil.startURL(this, getString(url)));
    }

    //public void openLink(View view) {
    //    NavUtil.startURL(this, view.getTag().toString());
    //}
}
