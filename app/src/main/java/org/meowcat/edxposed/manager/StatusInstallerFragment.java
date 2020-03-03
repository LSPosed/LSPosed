package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@SuppressLint("StaticFieldLeak")
public class StatusInstallerFragment extends Fragment {

    public static final File DISABLE_FILE = new File(XposedApp.BASE_DIR + "conf/disabled");
    private static AppCompatActivity sActivity;
    private static String mUpdateLink;
    private static View mUpdateView;
    private static View mUpdateButton;

    static void setUpdate(final String link, final String changelog, Context mContext) {
        mUpdateLink = link;

        mUpdateView.setVisibility(View.VISIBLE);
        mUpdateButton.setVisibility(View.VISIBLE);
        mUpdateButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(sActivity)
                .setTitle(R.string.changes)
                .setMessage(Html.fromHtml(changelog))
                .setPositiveButton(R.string.update, (dialog, which) -> update(mContext))
                .setNegativeButton(R.string.later, null).show());
    }

    private static void update(Context mContext) {
        Uri uri = Uri.parse(mUpdateLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        mContext.startActivity(intent);
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

    @SuppressWarnings("SameParameterValue")
    @SuppressLint({"WorldReadableFiles", "WorldWriteableFiles"})
    private static void setFilePermissionsFromMode(String name, int mode) {
        int perms = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                | FileUtils.S_IRGRP | FileUtils.S_IWGRP;
        if ((mode & Context.MODE_WORLD_READABLE) != 0) {
            perms |= FileUtils.S_IROTH;
        }
        if ((mode & Context.MODE_WORLD_WRITEABLE) != 0) {
            perms |= FileUtils.S_IWOTH;
        }
        FileUtils.setPermissions(name, perms, -1, -1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = (AppCompatActivity) getActivity();
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.status_installer, container, false);

        mUpdateView = v.findViewById(R.id.updateView);
        mUpdateButton = v.findViewById(R.id.click_to_update);


        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }

        TextView api = v.findViewById(R.id.api);
        TextView framework = v.findViewById(R.id.framework);
        TextView manager = v.findViewById(R.id.manager);
        TextView androidSdk = v.findViewById(R.id.android_version);
        TextView manufacturer = v.findViewById(R.id.ic_manufacturer);
        TextView cpu = v.findViewById(R.id.cpu);

        String mAppVer = "v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
        manager.setText(mAppVer);
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            String installedXposedVersionStr = installedXposedVersionInt + ".0";
            api.setText(installedXposedVersionStr);
            framework.setText(installedXposedVersion.replace(installedXposedVersionStr + "-", ""));
        }

        androidSdk.setText(getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        manufacturer.setText(getUIFramework());
        cpu.setText(getCompleteArch());

        determineVerifiedBootState(v);

        refreshKnownIssue();
        return v;
    }

    private void determineVerifiedBootState(View v) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class, String.class);
            m.setAccessible(true);

            String propSystemVerified = (String) m.invoke(null, "partition.system.verified", "0");
            String propState = (String) m.invoke(null, "ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = !propSystemVerified.equals("0");
            boolean detected = !propState.isEmpty() || fileDmVerityModule.exists();

            TextView tv = v.findViewById(R.id.dmverity);
            if (verified) {
                tv.setText(R.string.verified_boot_active);
                tv.setTextColor(getResources().getColor(R.color.warning));
            } else if (detected) {
                tv.setText(R.string.verified_boot_deactivated);
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            } else {
                tv.setText(R.string.verified_boot_none);
                tv.setTextColor(getResources().getColor(R.color.warning));
                v.findViewById(R.id.dmverity_explanation).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(XposedApp.TAG, "Could not detect Verified Boot state", e);
        }
    }

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