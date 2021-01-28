package org.meowcat.edxposed.manager.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.Constants;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.StatusInstallerBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import dalvik.system.VMRuntime;

@SuppressLint("StaticFieldLeak")
public class StatusDialogBuilder extends MaterialAlertDialogBuilder {
    private static String CPU_ABI;
    private static String CPU_ABI2;

    public StatusDialogBuilder(@NonNull Context context) {
        super(context);
        final String[] abiList;
        if (VMRuntime.getRuntime().is64Bit()) {
            abiList = Build.SUPPORTED_64_BIT_ABIS;
        } else {
            abiList = Build.SUPPORTED_32_BIT_ABIS;
        }
        CPU_ABI = abiList[0];
        if (abiList.length > 1) {
            CPU_ABI2 = abiList[1];
        } else {
            CPU_ABI2 = "";
        }
        StatusInstallerBinding binding = StatusInstallerBinding.inflate(LayoutInflater.from(context), null, false);

        String installedXposedVersion = Constants.getXposedVersion();
        String mAppVer = String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        binding.manager.setText(mAppVer);

        if (installedXposedVersion != null) {
            binding.api.setText(String.format(Locale.US, "%s.0", Constants.getXposedVariant()));
            binding.framework.setText(String.format(Locale.US, "%s (%s)", installedXposedVersion, Constants.getXposedVariant()));
        }

        binding.androidVersion.setText(context.getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        binding.manufacturer.setText(getUIFramework());
        binding.cpu.setText(getCompleteArch());

        determineVerifiedBootState(binding);
        setView(binding.getRoot());
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

    public static String getArch() {
        if (CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (CPU_ABI.startsWith("x86") || CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (CPU_ABI.startsWith("armeabi-v5") || CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    private void determineVerifiedBootState(StatusInstallerBinding binding) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class, String.class);
            m.setAccessible(true);

            String propSystemVerified = (String) m.invoke(null, "partition.system.verified", "0");
            String propState = (String) m.invoke(null, "ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = false;
            if (propSystemVerified != null) {
                verified = !propSystemVerified.equals("0");
            }
            boolean detected = false;
            if (propState != null) {
                detected = !propState.isEmpty() || fileDmVerityModule.exists();
            }

            if (verified) {
                binding.dmverity.setText(R.string.verified_boot_active);
                binding.dmverity.setTextColor(ContextCompat.getColor(getContext(), R.color.warning));
            } else if (detected) {
                binding.dmverity.setText(R.string.verified_boot_deactivated);
            } else {
                binding.dmverity.setText(R.string.verified_boot_none);
                binding.dmverity.setTextColor(ContextCompat.getColor(getContext(), R.color.warning));
            }
        } catch (Exception e) {
            Log.e(App.TAG, "Could not detect Verified Boot state", e);
        }
    }

    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 26:
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
            case 29:
                return "Q";
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
        if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(MIUI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Hydrogen/Oxygen OS)";
        } else if (new File("/system/framework/oppo-framework.jar").exists() || new File("/system/framework/oppo-framework-res.apk").exists() || new File("/system/framework/coloros-framework.jar").exists() || new File("/system/framework/coloros.services.jar").exists() || new File("/system/framework/oppo-services.jar").exists() || new File("/system/framework/coloros-support-wrapper.jar").exists()) {
            manufacturer += "(Color OS)";
        } else if (new File("/system/framework/hwEmui.jar").exists() || new File("/system/framework/hwcustEmui.jar").exists() || new File("/system/framework/hwframework.jar").exists() || new File("/system/framework/framework-res-hwext.apk").exists() || new File("/system/framework/hwServices.jar").exists() || new File("/system/framework/hwcustframework.jar").exists()) {
            manufacturer += "(EMUI)";
        } else if (new File("/system/framework/com.samsung.device.jar").exists() || new File("/system/framework/sec_platform_library.jar").exists()) {
            manufacturer += "(One UI)";
        } else if (new File("/system/priv-app/CarbonDelta/CarbonDelta.apk").exists()) {
            manufacturer += "(Carbon OS)";
        } else if (new File("/system/framework/flyme-framework.jar").exists() || new File("/system/framework/flyme-res").exists() || new File("/system/framework/flyme-telephony-common.jar").exists()) {
            manufacturer += "(Flyme)";
        } else if (new File("/system/framework/org.lineageos.platform-res.apk").exists() || new File("/system/framework/org.lineageos.platform.jar").exists()) {
            manufacturer += "(Lineage OS Based ROM)";
        } else if (new File("/system/framework/twframework.jar").exists() || new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/core.jar.jex").exists()) {
            manufacturer += "(Aliyun OS)";
        }
        return manufacturer;
    }
}