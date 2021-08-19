/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ActivityCrashReportBinding;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class CrashReportActivity extends AppCompatActivity {
    ActivityCrashReportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.copyLogs.setOnClickListener(v -> {
            var clipboard = getSystemService(ClipboardManager.class);
            //Are there any devices without clipboard...?
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("LSPManagerCrashInfo", getAllErrorDetailsFromIntent(getIntent()));
                clipboard.setPrimaryClip(clip);
                Snackbar.make(binding.snackbar, R.string.copy_toast_msg, Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    public String getAllErrorDetailsFromIntent(@NonNull Intent intent) {
        var dateFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        String versionName = String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);

        return "Build version: " + versionName + " \n" +
                "Current date: " + dateFormat.format(Instant.now()) + " \n" +
                "Device: " + getDeviceModelName() + " \n" +
                "Fingerprint: " + getFingerprint() + " \n \n" +
                "SDK: " + Build.VERSION.SDK_INT + " \n \n" +
                "Stack trace:  \n" +
                getStackTraceFromIntent(intent);
    }

    private String getFingerprint() {
        return Build.BRAND + '/' +
                Build.PRODUCT + '/' +
                Build.DEVICE + ':' +
                Build.VERSION.RELEASE + '/' +
                Build.ID + '/' +
                Build.VERSION.INCREMENTAL + ':' +
                Build.TYPE + '/' +
                Build.TAGS;
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
