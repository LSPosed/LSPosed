package io.github.lsposed.manager.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityCrashReportBinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CrashReportActivity extends AppCompatActivity {
    ActivityCrashReportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.copyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            //Are there any devices without clipboard...?
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("edcrash", getAllErrorDetailsFromIntent(getIntent()));
                clipboard.setPrimaryClip(clip);
                Snackbar.make(binding.snackbar, R.string.copy_toast_msg, Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    public String getAllErrorDetailsFromIntent(@NonNull Intent intent) {
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        String buildDateAsString = getBuildDateAsString(dateFormat);

        String versionName = getVersionName();

        String errorDetails = "";

        errorDetails += "Build version: " + versionName + " \n";
        if (buildDateAsString != null) {
            errorDetails += "Build date: " + buildDateAsString + " \n";
        }
        errorDetails += "Current date: " + dateFormat.format(currentDate) + " \n";
        errorDetails += "Device: " + getDeviceModelName() + " \n \n";
        errorDetails += "Stack trace:  \n";
        errorDetails += getStackTraceFromIntent(intent);
        return errorDetails;
    }

    private String getBuildDateAsString(@NonNull DateFormat dateFormat) {
        long buildDate;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);

            ZipEntry ze = zf.getEntry("classes.dex");
            buildDate = ze.getTime();


            zf.close();
        } catch (Exception e) {
            buildDate = 0;
        }

        if (buildDate > 312764400000L) {
            return dateFormat.format(new Date(buildDate));
        } else {
            return null;
        }
    }

    private String getVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getDeviceModelName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(@Nullable String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public String getStackTraceFromIntent(@NonNull Intent intent) {
        return intent.getStringExtra(BuildConfig.APPLICATION_ID + ".EXTRA_STACK_TRACE");
    }

}
