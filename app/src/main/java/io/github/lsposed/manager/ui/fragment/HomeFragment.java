package io.github.lsposed.manager.ui.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import io.github.lsposed.manager.ConfigManager;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.DialogAboutBinding;
import io.github.lsposed.manager.databinding.FragmentHomeBinding;
import io.github.lsposed.manager.ui.activity.LogsActivity;
import io.github.lsposed.manager.ui.activity.ModulesActivity;
import io.github.lsposed.manager.ui.activity.RepoActivity;
import io.github.lsposed.manager.ui.activity.SettingsActivity;
import io.github.lsposed.manager.util.GlideHelper;
import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.chrome.LinkTransformationMethod;
import name.mikanoshi.customiuizer.holidays.HolidayHelper;
import name.mikanoshi.customiuizer.utils.Helpers;
import rikka.core.res.ResourcesKt;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HolidayHelper.setup(requireActivity());
        binding.status.setOnClickListener(v -> {
            if (ConfigManager.getXposedApiVersion() != -1) {
                new StatusDialogBuilder(requireContext())
                        .setTitle(R.string.info)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                NavUtil.startURL(requireActivity(), getString(R.string.about_source));
            }
        });
        binding.modules.setOnClickListener(new StartActivityListener(ModulesActivity.class, true));
        binding.download.setOnClickListener(new StartActivityListener(RepoActivity.class, false));
        binding.logs.setOnClickListener(new StartActivityListener(LogsActivity.class, true));
        binding.settings.setOnClickListener(new StartActivityListener(SettingsActivity.class, false));
        binding.about.setOnClickListener(v -> {
            DialogAboutBinding binding = DialogAboutBinding.inflate(LayoutInflater.from(requireContext()), null, false);
            binding.sourceCode.setMovementMethod(LinkMovementMethod.getInstance());
            binding.sourceCode.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
            binding.sourceCode.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/LSPosed/LSPosed\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/LSPosed\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            new AlertDialog.Builder(requireContext())
                    .setView(binding.getRoot())
                    .show();
        });
        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(requireContext().getApplicationInfo()))
                .into(binding.appIcon);
        String installXposedVersion = ConfigManager.getXposedVersionName();
        int cardBackgroundColor;
        if (installXposedVersion != null) {
            binding.statusTitle.setText(String.format(Locale.US, "%s %s", getString(R.string.Activated), ConfigManager.getVariantString()));
            if (!ConfigManager.isPermissive()) {
                if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
                    cardBackgroundColor = 0xfff05654;
                } else {
                    cardBackgroundColor = ResourcesKt.resolveColor(requireContext().getTheme(), R.attr.colorNormal);
                }
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installXposedVersion, ConfigManager.getXposedVersionCode()));
            } else {
                cardBackgroundColor = ResourcesKt.resolveColor(requireContext().getTheme(), R.attr.colorError);
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_permissive_summary);
            }
        } else {
            cardBackgroundColor = ResourcesKt.resolveColor(requireContext().getTheme(), R.attr.colorInstall);
            binding.statusTitle.setText(R.string.Install);
            binding.statusSummary.setText(R.string.InstallDetail);
            binding.statusIcon.setImageResource(R.drawable.ic_error);
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
        }
        binding.status.setCardBackgroundColor(cardBackgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.status.setOutlineSpotShadowColor(cardBackgroundColor);
            binding.status.setOutlineAmbientShadowColor(cardBackgroundColor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.modulesSummary.setText(String.format(getString(R.string.ModulesDetail), ModuleUtil.getInstance().getEnabledModules().size()));
        HolidayHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        HolidayHelper.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HolidayHelper.onDestroy();
    }

    private class StartActivityListener implements View.OnClickListener {
        boolean requireInstalled;
        Class<?> clazz;

        StartActivityListener(Class<?> clazz, boolean requireInstalled) {
            this.clazz = clazz;
            this.requireInstalled = requireInstalled;
        }

        @Override
        public void onClick(View v) {
            if (requireInstalled && ConfigManager.getXposedVersionName() == null) {
                Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent();
                intent.setClass(requireActivity(), clazz);
                startActivity(intent);
            }
        }
    }
}
