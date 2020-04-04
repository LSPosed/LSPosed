package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.databinding.StatusInstallerBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

@SuppressLint("StaticFieldLeak")
public class StatusInstallerFragment extends Fragment {
    private static StatusInstallerBinding binding;
    private static String updateLink;

    static void setUpdate(final String link, final String changelog, Context context) {
        updateLink = link;

        binding.updateView.setVisibility(View.VISIBLE);
        binding.clickToUpdate.setVisibility(View.VISIBLE);
        binding.clickToUpdate.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.changes)
                .setMessage(HtmlCompat.fromHtml(changelog, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(R.string.update, (dialog, which) -> update(context))
                .setNegativeButton(R.string.later, null).show());
    }

    public static boolean isEnhancementEnabled() {
        return false;
    }

    private static void update(Context context) {
        Uri uri = Uri.parse(updateLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    private static String getCompleteArch() {
        String info = "";

        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            while ((text = br.readLine()) != null) {
                if (!text.startsWith("processor")) break;
            }
            br.close();
            String[] array = text != null ? text.split(":\\s+", 2) : new String[0];
            if (array.length >= 2) {
                info += array[1] + " ";
            }
        } catch (IOException ignored) {
        }

        info += Build.SUPPORTED_ABIS[0];
        return info + " (" + getArch() + ")";
    }

    @SuppressWarnings("deprecation")
    private static String getArch() {
        if (Build.CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (Build.CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (Build.CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (Build.CPU_ABI.startsWith("x86") || Build.CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (Build.CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (Build.CPU_ABI.startsWith("armeabi-v5") || Build.CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    @SuppressLint("WorldReadableFiles")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = StatusInstallerBinding.inflate(inflater, container, false);

        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }

        String mAppVer;
        if (isEnhancementEnabled()) {
            mAppVer = String.format("v%s (%s) (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, getString(R.string.status_enhancement));
        } else {
            mAppVer = String.format("v%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        }
        binding.manager.setText(mAppVer);
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            String installedXposedVersionStr = installedXposedVersionInt + ".0";
            binding.api.setText(installedXposedVersionStr);
            binding.framework.setText(installedXposedVersion.replace(installedXposedVersionStr + "-", ""));
        }

        binding.androidVersion.setText(getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        binding.manufacturer.setText(getUIFramework());
        binding.cpu.setText(getCompleteArch());

        determineVerifiedBootState(binding);
        return binding.getRoot();
    }

    private void determineVerifiedBootState(StatusInstallerBinding binding) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class, String.class);
            m.setAccessible(true);

            String propSystemVerified = (String) m.invoke(null, "partition.system.verified", "0");
            String propState = (String) m.invoke(null, "ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = !propSystemVerified.equals("0");
            boolean detected = !propState.isEmpty() || fileDmVerityModule.exists();

            if (verified) {
                binding.dmverity.setText(R.string.verified_boot_active);
                binding.dmverity.setTextColor(ContextCompat.getColor(requireActivity(), R.color.warning));
            } else if (detected) {
                binding.dmverity.setText(R.string.verified_boot_deactivated);
                binding.dmverityExplanation.setVisibility(View.GONE);
            } else {
                binding.dmverity.setText(R.string.verified_boot_none);
                binding.dmverity.setTextColor(ContextCompat.getColor(requireActivity(), R.color.warning));
                binding.dmverityExplanation.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(XposedApp.TAG, "Could not detect Verified Boot state", e);
        }
    }

    /*
        @SuppressWarnings("SameParameterValue")
        private boolean checkAppInstalled(Context context, String pkgName) {
            if (pkgName == null || pkgName.isEmpty()) {
                return false;
            }
            final PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> info = packageManager.getInstalledPackages(0);
            if (info == null || info.isEmpty()) {
                return false;
            }
            for (int i = 0; i < info.size(); i++) {
                if (pkgName.equals(info.get(i).packageName)) {
                    return true;
                }
            }
            return false;
        }

        @SuppressLint("StringFormatInvalid")
        private void refreshKnownIssue() {
            String issueName = null;
            String issueLink = null;
            final ApplicationInfo appInfo = Objects.requireNonNull(getActivity()).getApplicationInfo();
            final File baseDir = new File(XposedApp.BASE_DIR);
            final File baseDirCanonical = getCanonicalFile(baseDir);
            final File baseDirActual = new File(Build.VERSION.SDK_INT >= 24 ? appInfo.deviceProtectedDataDir : appInfo.dataDir);
            final File baseDirActualCanonical = getCanonicalFile(baseDirActual);

            if (new File("/system/framework/core.jar.jex").exists()) {
                issueName = "Aliyun OS";
                issueLink = "https://forum.xda-developers.com/showpost.php?p=52289793&postcount=5";
    //        } else if (Build.VERSION.SDK_INT < 24 && (new File("/data/miui/DexspyInstaller.jar").exists() || checkClassExists("miui.dexspy.DexspyInstaller"))) {
    //            issueName = "MIUI/Dexspy";
    //            issueLink = "https://forum.xda-developers.com/showpost.php?p=52291098&postcount=6";
    //        } else if (Build.VERSION.SDK_INT < 24 && new File("/system/framework/twframework.jar").exists()) {
    //            issueName = "Samsung TouchWiz ROM";
    //            issueLink = "https://forum.xda-developers.com/showthread.php?t=3034811";
            } else if (!baseDirCanonical.equals(baseDirActualCanonical)) {
                Log.e(XposedApp.TAG, "Base directory: " + getPathWithCanonicalPath(baseDir, baseDirCanonical));
                Log.e(XposedApp.TAG, "Expected: " + getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
                issueName = getString(R.string.known_issue_wrong_base_directory, getPathWithCanonicalPath(baseDirActual, baseDirActualCanonical));
            } else if (!baseDir.exists()) {
                issueName = getString(R.string.known_issue_missing_base_directory);
                issueLink = "https://github.com/rovo89/XposedInstaller/issues/393";
            } else if (checkAppInstalled(getContext(), "com.solohsu.android.edxp.manager")) {
                issueName = getString(R.string.edxp_installer_installed);
                issueLink = getString(R.string.about_support);
            }

        }
    */
    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
//            case 16:
//            case 17:
//            case 18:
//                return "Jelly Bean";
//            case 19:
//                return "KitKat";
            case 21:
            case 22:
                return "Lollipop";
            case 23:
                return "Marshmallow";
            case 24:
            case 25:
                return "Nougat";
            case 26:
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
            case 29:
                return "Ten";
            case 30:
                return "R";
        }
        return "Unknown";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (new File("/system/framework/twframework.jar").exists() || new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(Mi UI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Oxygen/Hydrogen OS)";
        } else if (new File("/system/framework/com.samsung.device.jar").exists() || new File("/system/framework/sec_platform_library.jar").exists()) {
            manufacturer += "(One UI)";
        }
        /*if (manufacturer.contains("Samsung")) {
            manufacturer += new File("/system/framework/twframework.jar").exists() ||
                    new File("/system/framework/samsung-services.jar").exists()
                    ? "(TouchWiz)" : "(AOSP-based ROM)";
        } else if (manufacturer.contains("Xiaomi")) {
            manufacturer += new File("/system/framework/framework-miui-res.apk").exists() ? "(MIUI)" : "(AOSP-based ROM)";
        }*/
        return manufacturer;
    }

    /*
        private File getCanonicalFile(File file) {
            try {
                return file.getCanonicalFile();
            } catch (IOException e) {
                Log.e(XposedApp.TAG, "Failed to get canonical file for " + file.getAbsolutePath(), e);
                return file;
            }
        }

        private String getPathWithCanonicalPath(File file, File canonical) {
            if (file.equals(canonical)) {
                return file.getAbsolutePath();
            } else {
                return file.getAbsolutePath() + " \u2192 " + canonical.getAbsolutePath();
            }
        }
    */
    private int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }
}