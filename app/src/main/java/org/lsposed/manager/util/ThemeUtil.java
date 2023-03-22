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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

import java.util.HashMap;
import java.util.Map;

import rikka.core.util.ResourceUtils;

public class ThemeUtil {
    private static final Map<String, Integer> colorThemeMap = new HashMap<>();
    private static final SharedPreferences preferences;

    public static final String MODE_NIGHT_FOLLOW_SYSTEM = "MODE_NIGHT_FOLLOW_SYSTEM";
    public static final String MODE_NIGHT_NO = "MODE_NIGHT_NO";
    public static final String MODE_NIGHT_YES = "MODE_NIGHT_YES";

    static {
        preferences = App.getPreferences();
        colorThemeMap.put("SAKURA", R.style.ThemeOverlay_MaterialSakura);
        colorThemeMap.put("MATERIAL_RED", R.style.ThemeOverlay_MaterialRed);
        colorThemeMap.put("MATERIAL_PINK", R.style.ThemeOverlay_MaterialPink);
        colorThemeMap.put("MATERIAL_PURPLE", R.style.ThemeOverlay_MaterialPurple);
        colorThemeMap.put("MATERIAL_DEEP_PURPLE", R.style.ThemeOverlay_MaterialDeepPurple);
        colorThemeMap.put("MATERIAL_INDIGO", R.style.ThemeOverlay_MaterialIndigo);
        colorThemeMap.put("MATERIAL_BLUE", R.style.ThemeOverlay_MaterialBlue);
        colorThemeMap.put("MATERIAL_LIGHT_BLUE", R.style.ThemeOverlay_MaterialLightBlue);
        colorThemeMap.put("MATERIAL_CYAN", R.style.ThemeOverlay_MaterialCyan);
        colorThemeMap.put("MATERIAL_TEAL", R.style.ThemeOverlay_MaterialTeal);
        colorThemeMap.put("MATERIAL_GREEN", R.style.ThemeOverlay_MaterialGreen);
        colorThemeMap.put("MATERIAL_LIGHT_GREEN", R.style.ThemeOverlay_MaterialLightGreen);
        colorThemeMap.put("MATERIAL_LIME", R.style.ThemeOverlay_MaterialLime);
        colorThemeMap.put("MATERIAL_YELLOW", R.style.ThemeOverlay_MaterialYellow);
        colorThemeMap.put("MATERIAL_AMBER", R.style.ThemeOverlay_MaterialAmber);
        colorThemeMap.put("MATERIAL_ORANGE", R.style.ThemeOverlay_MaterialOrange);
        colorThemeMap.put("MATERIAL_DEEP_ORANGE", R.style.ThemeOverlay_MaterialDeepOrange);
        colorThemeMap.put("MATERIAL_BROWN", R.style.ThemeOverlay_MaterialBrown);
        colorThemeMap.put("MATERIAL_BLUE_GREY", R.style.ThemeOverlay_MaterialBlueGrey);
    }

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    private static boolean isBlackNightTheme() {
        return preferences.getBoolean("black_dark_theme", false);
    }

    public static boolean isSystemAccent() {
        return DynamicColors.isDynamicColorAvailable() && preferences.getBoolean("follow_system_accent", true);
    }

    public static String getNightTheme(Context context) {
        if (isBlackNightTheme()
                && ResourceUtils.isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return THEME_DEFAULT;
    }

    @StyleRes
    public static int getNightThemeStyleRes(Context context) {
        switch (getNightTheme(context)) {
            case THEME_BLACK:
                return R.style.ThemeOverlay_Black;
            case THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay;
        }
    }

    public static String getColorTheme() {
        if (isSystemAccent()) {
            return "SYSTEM";
        }
        return preferences.getString("theme_color", "COLOR_BLUE");
    }

    @StyleRes
    public static int getColorThemeStyleRes() {
        Integer theme = colorThemeMap.get(getColorTheme());
        if (theme == null) {
            return R.style.ThemeOverlay_MaterialBlue;
        }
        return theme;
    }

    public static int getDarkTheme(String mode) {
        switch (mode) {
            case MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case MODE_NIGHT_YES:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case MODE_NIGHT_NO:
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    public static int getDarkTheme() {
        return getDarkTheme(preferences.getString("dark_theme", MODE_NIGHT_FOLLOW_SYSTEM));
    }
}
