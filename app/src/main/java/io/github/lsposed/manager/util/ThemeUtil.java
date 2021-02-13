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

package io.github.lsposed.manager.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StyleRes;

import java.util.HashMap;
import java.util.Map;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import rikka.core.util.ResourceUtils;

public class ThemeUtil {
    private static final Map<String, Integer> colorThemeMap = new HashMap<>();
    private static final SharedPreferences preferences;

    static {
        preferences = App.getPreferences();
        colorThemeMap.put("ThemeOverlay.colorPrimary.colorAccent", R.style.ThemeOverlay_colorPrimary_colorAccent);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_red_a200", R.style.ThemeOverlay_colorPrimary_material_red_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_pink_a200", R.style.ThemeOverlay_colorPrimary_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_purple_a200", R.style.ThemeOverlay_colorPrimary_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_deep_purple_a200", R.style.ThemeOverlay_colorPrimary_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_indigo_a200", R.style.ThemeOverlay_colorPrimary_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_blue_a200", R.style.ThemeOverlay_colorPrimary_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_light_blue_500", R.style.ThemeOverlay_colorPrimary_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_cyan_500", R.style.ThemeOverlay_colorPrimary_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_teal_500", R.style.ThemeOverlay_colorPrimary_material_teal_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_green_500", R.style.ThemeOverlay_colorPrimary_material_green_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_light_green_500", R.style.ThemeOverlay_colorPrimary_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_lime_500", R.style.ThemeOverlay_colorPrimary_material_lime_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_yellow_500", R.style.ThemeOverlay_colorPrimary_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_amber_500", R.style.ThemeOverlay_colorPrimary_material_amber_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_orange_500", R.style.ThemeOverlay_colorPrimary_material_orange_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_deep_orange_500", R.style.ThemeOverlay_colorPrimary_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_brown_500", R.style.ThemeOverlay_colorPrimary_material_brown_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_grey_500", R.style.ThemeOverlay_colorPrimary_material_grey_500);
        colorThemeMap.put("ThemeOverlay.colorPrimary.material_blue_grey_500", R.style.ThemeOverlay_colorPrimary_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.colorAccent", R.style.ThemeOverlay_material_red_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_red_a200", R.style.ThemeOverlay_material_red_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_pink_a200", R.style.ThemeOverlay_material_red_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_purple_a200", R.style.ThemeOverlay_material_red_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_deep_purple_a200", R.style.ThemeOverlay_material_red_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_indigo_a200", R.style.ThemeOverlay_material_red_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_blue_a200", R.style.ThemeOverlay_material_red_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_light_blue_500", R.style.ThemeOverlay_material_red_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_cyan_500", R.style.ThemeOverlay_material_red_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_teal_500", R.style.ThemeOverlay_material_red_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_green_500", R.style.ThemeOverlay_material_red_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_light_green_500", R.style.ThemeOverlay_material_red_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_lime_500", R.style.ThemeOverlay_material_red_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_yellow_500", R.style.ThemeOverlay_material_red_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_amber_500", R.style.ThemeOverlay_material_red_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_orange_500", R.style.ThemeOverlay_material_red_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_deep_orange_500", R.style.ThemeOverlay_material_red_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_brown_500", R.style.ThemeOverlay_material_red_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_grey_500", R.style.ThemeOverlay_material_red_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_red_500.material_blue_grey_500", R.style.ThemeOverlay_material_red_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.colorAccent", R.style.ThemeOverlay_material_pink_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_red_a200", R.style.ThemeOverlay_material_pink_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_pink_a200", R.style.ThemeOverlay_material_pink_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_purple_a200", R.style.ThemeOverlay_material_pink_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_deep_purple_a200", R.style.ThemeOverlay_material_pink_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_indigo_a200", R.style.ThemeOverlay_material_pink_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_blue_a200", R.style.ThemeOverlay_material_pink_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_light_blue_500", R.style.ThemeOverlay_material_pink_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_cyan_500", R.style.ThemeOverlay_material_pink_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_teal_500", R.style.ThemeOverlay_material_pink_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_green_500", R.style.ThemeOverlay_material_pink_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_light_green_500", R.style.ThemeOverlay_material_pink_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_lime_500", R.style.ThemeOverlay_material_pink_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_yellow_500", R.style.ThemeOverlay_material_pink_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_amber_500", R.style.ThemeOverlay_material_pink_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_orange_500", R.style.ThemeOverlay_material_pink_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_deep_orange_500", R.style.ThemeOverlay_material_pink_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_brown_500", R.style.ThemeOverlay_material_pink_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_grey_500", R.style.ThemeOverlay_material_pink_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_pink_500.material_blue_grey_500", R.style.ThemeOverlay_material_pink_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.colorAccent", R.style.ThemeOverlay_material_purple_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_red_a200", R.style.ThemeOverlay_material_purple_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_pink_a200", R.style.ThemeOverlay_material_purple_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_purple_a200", R.style.ThemeOverlay_material_purple_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_deep_purple_a200", R.style.ThemeOverlay_material_purple_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_indigo_a200", R.style.ThemeOverlay_material_purple_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_blue_a200", R.style.ThemeOverlay_material_purple_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_light_blue_500", R.style.ThemeOverlay_material_purple_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_cyan_500", R.style.ThemeOverlay_material_purple_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_teal_500", R.style.ThemeOverlay_material_purple_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_green_500", R.style.ThemeOverlay_material_purple_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_light_green_500", R.style.ThemeOverlay_material_purple_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_lime_500", R.style.ThemeOverlay_material_purple_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_yellow_500", R.style.ThemeOverlay_material_purple_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_amber_500", R.style.ThemeOverlay_material_purple_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_orange_500", R.style.ThemeOverlay_material_purple_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_deep_orange_500", R.style.ThemeOverlay_material_purple_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_brown_500", R.style.ThemeOverlay_material_purple_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_grey_500", R.style.ThemeOverlay_material_purple_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_purple_500.material_blue_grey_500", R.style.ThemeOverlay_material_purple_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.colorAccent", R.style.ThemeOverlay_material_deep_purple_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_red_a200", R.style.ThemeOverlay_material_deep_purple_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_pink_a200", R.style.ThemeOverlay_material_deep_purple_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_purple_a200", R.style.ThemeOverlay_material_deep_purple_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_deep_purple_a200", R.style.ThemeOverlay_material_deep_purple_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_indigo_a200", R.style.ThemeOverlay_material_deep_purple_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_blue_a200", R.style.ThemeOverlay_material_deep_purple_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_light_blue_500", R.style.ThemeOverlay_material_deep_purple_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_cyan_500", R.style.ThemeOverlay_material_deep_purple_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_teal_500", R.style.ThemeOverlay_material_deep_purple_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_green_500", R.style.ThemeOverlay_material_deep_purple_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_light_green_500", R.style.ThemeOverlay_material_deep_purple_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_lime_500", R.style.ThemeOverlay_material_deep_purple_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_yellow_500", R.style.ThemeOverlay_material_deep_purple_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_amber_500", R.style.ThemeOverlay_material_deep_purple_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_orange_500", R.style.ThemeOverlay_material_deep_purple_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_deep_orange_500", R.style.ThemeOverlay_material_deep_purple_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_brown_500", R.style.ThemeOverlay_material_deep_purple_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_grey_500", R.style.ThemeOverlay_material_deep_purple_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_deep_purple_500.material_blue_grey_500", R.style.ThemeOverlay_material_deep_purple_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.colorAccent", R.style.ThemeOverlay_material_indigo_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_red_a200", R.style.ThemeOverlay_material_indigo_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_pink_a200", R.style.ThemeOverlay_material_indigo_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_purple_a200", R.style.ThemeOverlay_material_indigo_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_deep_purple_a200", R.style.ThemeOverlay_material_indigo_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_indigo_a200", R.style.ThemeOverlay_material_indigo_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_blue_a200", R.style.ThemeOverlay_material_indigo_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_light_blue_500", R.style.ThemeOverlay_material_indigo_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_cyan_500", R.style.ThemeOverlay_material_indigo_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_teal_500", R.style.ThemeOverlay_material_indigo_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_green_500", R.style.ThemeOverlay_material_indigo_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_light_green_500", R.style.ThemeOverlay_material_indigo_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_lime_500", R.style.ThemeOverlay_material_indigo_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_yellow_500", R.style.ThemeOverlay_material_indigo_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_amber_500", R.style.ThemeOverlay_material_indigo_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_orange_500", R.style.ThemeOverlay_material_indigo_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_deep_orange_500", R.style.ThemeOverlay_material_indigo_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_brown_500", R.style.ThemeOverlay_material_indigo_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_grey_500", R.style.ThemeOverlay_material_indigo_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_indigo_500.material_blue_grey_500", R.style.ThemeOverlay_material_indigo_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.colorAccent", R.style.ThemeOverlay_material_blue_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_red_a200", R.style.ThemeOverlay_material_blue_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_pink_a200", R.style.ThemeOverlay_material_blue_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_purple_a200", R.style.ThemeOverlay_material_blue_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_deep_purple_a200", R.style.ThemeOverlay_material_blue_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_indigo_a200", R.style.ThemeOverlay_material_blue_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_blue_a200", R.style.ThemeOverlay_material_blue_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_light_blue_500", R.style.ThemeOverlay_material_blue_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_cyan_500", R.style.ThemeOverlay_material_blue_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_teal_500", R.style.ThemeOverlay_material_blue_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_green_500", R.style.ThemeOverlay_material_blue_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_light_green_500", R.style.ThemeOverlay_material_blue_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_lime_500", R.style.ThemeOverlay_material_blue_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_yellow_500", R.style.ThemeOverlay_material_blue_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_amber_500", R.style.ThemeOverlay_material_blue_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_orange_500", R.style.ThemeOverlay_material_blue_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_deep_orange_500", R.style.ThemeOverlay_material_blue_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_brown_500", R.style.ThemeOverlay_material_blue_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_grey_500", R.style.ThemeOverlay_material_blue_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_blue_500.material_blue_grey_500", R.style.ThemeOverlay_material_blue_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.colorAccent", R.style.ThemeOverlay_material_light_blue_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_red_a200", R.style.ThemeOverlay_material_light_blue_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_pink_a200", R.style.ThemeOverlay_material_light_blue_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_purple_a200", R.style.ThemeOverlay_material_light_blue_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_deep_purple_a200", R.style.ThemeOverlay_material_light_blue_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_indigo_a200", R.style.ThemeOverlay_material_light_blue_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_blue_a200", R.style.ThemeOverlay_material_light_blue_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_light_blue_500", R.style.ThemeOverlay_material_light_blue_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_cyan_500", R.style.ThemeOverlay_material_light_blue_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_teal_500", R.style.ThemeOverlay_material_light_blue_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_green_500", R.style.ThemeOverlay_material_light_blue_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_light_green_500", R.style.ThemeOverlay_material_light_blue_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_lime_500", R.style.ThemeOverlay_material_light_blue_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_yellow_500", R.style.ThemeOverlay_material_light_blue_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_amber_500", R.style.ThemeOverlay_material_light_blue_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_orange_500", R.style.ThemeOverlay_material_light_blue_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_deep_orange_500", R.style.ThemeOverlay_material_light_blue_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_brown_500", R.style.ThemeOverlay_material_light_blue_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_grey_500", R.style.ThemeOverlay_material_light_blue_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_light_blue_500.material_blue_grey_500", R.style.ThemeOverlay_material_light_blue_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.colorAccent", R.style.ThemeOverlay_material_cyan_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_red_a200", R.style.ThemeOverlay_material_cyan_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_pink_a200", R.style.ThemeOverlay_material_cyan_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_purple_a200", R.style.ThemeOverlay_material_cyan_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_deep_purple_a200", R.style.ThemeOverlay_material_cyan_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_indigo_a200", R.style.ThemeOverlay_material_cyan_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_blue_a200", R.style.ThemeOverlay_material_cyan_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_light_blue_500", R.style.ThemeOverlay_material_cyan_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_cyan_500", R.style.ThemeOverlay_material_cyan_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_teal_500", R.style.ThemeOverlay_material_cyan_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_green_500", R.style.ThemeOverlay_material_cyan_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_light_green_500", R.style.ThemeOverlay_material_cyan_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_lime_500", R.style.ThemeOverlay_material_cyan_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_yellow_500", R.style.ThemeOverlay_material_cyan_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_amber_500", R.style.ThemeOverlay_material_cyan_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_orange_500", R.style.ThemeOverlay_material_cyan_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_deep_orange_500", R.style.ThemeOverlay_material_cyan_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_brown_500", R.style.ThemeOverlay_material_cyan_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_grey_500", R.style.ThemeOverlay_material_cyan_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_cyan_500.material_blue_grey_500", R.style.ThemeOverlay_material_cyan_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.colorAccent", R.style.ThemeOverlay_material_teal_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_red_a200", R.style.ThemeOverlay_material_teal_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_pink_a200", R.style.ThemeOverlay_material_teal_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_purple_a200", R.style.ThemeOverlay_material_teal_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_deep_purple_a200", R.style.ThemeOverlay_material_teal_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_indigo_a200", R.style.ThemeOverlay_material_teal_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_blue_a200", R.style.ThemeOverlay_material_teal_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_light_blue_500", R.style.ThemeOverlay_material_teal_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_cyan_500", R.style.ThemeOverlay_material_teal_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_teal_500", R.style.ThemeOverlay_material_teal_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_green_500", R.style.ThemeOverlay_material_teal_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_light_green_500", R.style.ThemeOverlay_material_teal_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_lime_500", R.style.ThemeOverlay_material_teal_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_yellow_500", R.style.ThemeOverlay_material_teal_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_amber_500", R.style.ThemeOverlay_material_teal_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_orange_500", R.style.ThemeOverlay_material_teal_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_deep_orange_500", R.style.ThemeOverlay_material_teal_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_brown_500", R.style.ThemeOverlay_material_teal_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_grey_500", R.style.ThemeOverlay_material_teal_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_teal_500.material_blue_grey_500", R.style.ThemeOverlay_material_teal_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.colorAccent", R.style.ThemeOverlay_material_green_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_red_a200", R.style.ThemeOverlay_material_green_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_pink_a200", R.style.ThemeOverlay_material_green_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_purple_a200", R.style.ThemeOverlay_material_green_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_deep_purple_a200", R.style.ThemeOverlay_material_green_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_indigo_a200", R.style.ThemeOverlay_material_green_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_blue_a200", R.style.ThemeOverlay_material_green_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_light_blue_500", R.style.ThemeOverlay_material_green_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_cyan_500", R.style.ThemeOverlay_material_green_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_teal_500", R.style.ThemeOverlay_material_green_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_green_500", R.style.ThemeOverlay_material_green_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_light_green_500", R.style.ThemeOverlay_material_green_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_lime_500", R.style.ThemeOverlay_material_green_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_yellow_500", R.style.ThemeOverlay_material_green_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_amber_500", R.style.ThemeOverlay_material_green_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_orange_500", R.style.ThemeOverlay_material_green_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_deep_orange_500", R.style.ThemeOverlay_material_green_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_brown_500", R.style.ThemeOverlay_material_green_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_grey_500", R.style.ThemeOverlay_material_green_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_green_500.material_blue_grey_500", R.style.ThemeOverlay_material_green_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.colorAccent", R.style.ThemeOverlay_material_light_green_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_red_a200", R.style.ThemeOverlay_material_light_green_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_pink_a200", R.style.ThemeOverlay_material_light_green_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_purple_a200", R.style.ThemeOverlay_material_light_green_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_deep_purple_a200", R.style.ThemeOverlay_material_light_green_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_indigo_a200", R.style.ThemeOverlay_material_light_green_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_blue_a200", R.style.ThemeOverlay_material_light_green_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_light_blue_500", R.style.ThemeOverlay_material_light_green_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_cyan_500", R.style.ThemeOverlay_material_light_green_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_teal_500", R.style.ThemeOverlay_material_light_green_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_green_500", R.style.ThemeOverlay_material_light_green_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_light_green_500", R.style.ThemeOverlay_material_light_green_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_lime_500", R.style.ThemeOverlay_material_light_green_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_yellow_500", R.style.ThemeOverlay_material_light_green_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_amber_500", R.style.ThemeOverlay_material_light_green_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_orange_500", R.style.ThemeOverlay_material_light_green_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_deep_orange_500", R.style.ThemeOverlay_material_light_green_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_brown_500", R.style.ThemeOverlay_material_light_green_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_grey_500", R.style.ThemeOverlay_material_light_green_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_light_green_500.material_blue_grey_500", R.style.ThemeOverlay_material_light_green_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.colorAccent", R.style.ThemeOverlay_material_lime_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_red_a200", R.style.ThemeOverlay_material_lime_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_pink_a200", R.style.ThemeOverlay_material_lime_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_purple_a200", R.style.ThemeOverlay_material_lime_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_deep_purple_a200", R.style.ThemeOverlay_material_lime_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_indigo_a200", R.style.ThemeOverlay_material_lime_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_blue_a200", R.style.ThemeOverlay_material_lime_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_light_blue_500", R.style.ThemeOverlay_material_lime_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_cyan_500", R.style.ThemeOverlay_material_lime_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_teal_500", R.style.ThemeOverlay_material_lime_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_green_500", R.style.ThemeOverlay_material_lime_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_light_green_500", R.style.ThemeOverlay_material_lime_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_lime_500", R.style.ThemeOverlay_material_lime_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_yellow_500", R.style.ThemeOverlay_material_lime_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_amber_500", R.style.ThemeOverlay_material_lime_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_orange_500", R.style.ThemeOverlay_material_lime_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_deep_orange_500", R.style.ThemeOverlay_material_lime_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_brown_500", R.style.ThemeOverlay_material_lime_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_grey_500", R.style.ThemeOverlay_material_lime_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_lime_500.material_blue_grey_500", R.style.ThemeOverlay_material_lime_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.colorAccent", R.style.ThemeOverlay_material_yellow_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_red_a200", R.style.ThemeOverlay_material_yellow_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_pink_a200", R.style.ThemeOverlay_material_yellow_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_purple_a200", R.style.ThemeOverlay_material_yellow_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_deep_purple_a200", R.style.ThemeOverlay_material_yellow_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_indigo_a200", R.style.ThemeOverlay_material_yellow_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_blue_a200", R.style.ThemeOverlay_material_yellow_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_light_blue_500", R.style.ThemeOverlay_material_yellow_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_cyan_500", R.style.ThemeOverlay_material_yellow_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_teal_500", R.style.ThemeOverlay_material_yellow_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_green_500", R.style.ThemeOverlay_material_yellow_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_light_green_500", R.style.ThemeOverlay_material_yellow_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_lime_500", R.style.ThemeOverlay_material_yellow_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_yellow_500", R.style.ThemeOverlay_material_yellow_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_amber_500", R.style.ThemeOverlay_material_yellow_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_orange_500", R.style.ThemeOverlay_material_yellow_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_deep_orange_500", R.style.ThemeOverlay_material_yellow_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_brown_500", R.style.ThemeOverlay_material_yellow_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_grey_500", R.style.ThemeOverlay_material_yellow_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_yellow_500.material_blue_grey_500", R.style.ThemeOverlay_material_yellow_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.colorAccent", R.style.ThemeOverlay_material_amber_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_red_a200", R.style.ThemeOverlay_material_amber_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_pink_a200", R.style.ThemeOverlay_material_amber_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_purple_a200", R.style.ThemeOverlay_material_amber_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_deep_purple_a200", R.style.ThemeOverlay_material_amber_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_indigo_a200", R.style.ThemeOverlay_material_amber_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_blue_a200", R.style.ThemeOverlay_material_amber_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_light_blue_500", R.style.ThemeOverlay_material_amber_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_cyan_500", R.style.ThemeOverlay_material_amber_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_teal_500", R.style.ThemeOverlay_material_amber_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_green_500", R.style.ThemeOverlay_material_amber_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_light_green_500", R.style.ThemeOverlay_material_amber_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_lime_500", R.style.ThemeOverlay_material_amber_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_yellow_500", R.style.ThemeOverlay_material_amber_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_amber_500", R.style.ThemeOverlay_material_amber_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_orange_500", R.style.ThemeOverlay_material_amber_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_deep_orange_500", R.style.ThemeOverlay_material_amber_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_brown_500", R.style.ThemeOverlay_material_amber_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_grey_500", R.style.ThemeOverlay_material_amber_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_amber_500.material_blue_grey_500", R.style.ThemeOverlay_material_amber_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.colorAccent", R.style.ThemeOverlay_material_orange_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_red_a200", R.style.ThemeOverlay_material_orange_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_pink_a200", R.style.ThemeOverlay_material_orange_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_purple_a200", R.style.ThemeOverlay_material_orange_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_deep_purple_a200", R.style.ThemeOverlay_material_orange_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_indigo_a200", R.style.ThemeOverlay_material_orange_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_blue_a200", R.style.ThemeOverlay_material_orange_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_light_blue_500", R.style.ThemeOverlay_material_orange_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_cyan_500", R.style.ThemeOverlay_material_orange_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_teal_500", R.style.ThemeOverlay_material_orange_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_green_500", R.style.ThemeOverlay_material_orange_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_light_green_500", R.style.ThemeOverlay_material_orange_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_lime_500", R.style.ThemeOverlay_material_orange_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_yellow_500", R.style.ThemeOverlay_material_orange_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_amber_500", R.style.ThemeOverlay_material_orange_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_orange_500", R.style.ThemeOverlay_material_orange_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_deep_orange_500", R.style.ThemeOverlay_material_orange_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_brown_500", R.style.ThemeOverlay_material_orange_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_grey_500", R.style.ThemeOverlay_material_orange_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_orange_500.material_blue_grey_500", R.style.ThemeOverlay_material_orange_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.colorAccent", R.style.ThemeOverlay_material_deep_orange_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_red_a200", R.style.ThemeOverlay_material_deep_orange_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_pink_a200", R.style.ThemeOverlay_material_deep_orange_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_purple_a200", R.style.ThemeOverlay_material_deep_orange_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_deep_purple_a200", R.style.ThemeOverlay_material_deep_orange_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_indigo_a200", R.style.ThemeOverlay_material_deep_orange_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_blue_a200", R.style.ThemeOverlay_material_deep_orange_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_light_blue_500", R.style.ThemeOverlay_material_deep_orange_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_cyan_500", R.style.ThemeOverlay_material_deep_orange_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_teal_500", R.style.ThemeOverlay_material_deep_orange_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_green_500", R.style.ThemeOverlay_material_deep_orange_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_light_green_500", R.style.ThemeOverlay_material_deep_orange_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_lime_500", R.style.ThemeOverlay_material_deep_orange_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_yellow_500", R.style.ThemeOverlay_material_deep_orange_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_amber_500", R.style.ThemeOverlay_material_deep_orange_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_orange_500", R.style.ThemeOverlay_material_deep_orange_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_deep_orange_500", R.style.ThemeOverlay_material_deep_orange_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_brown_500", R.style.ThemeOverlay_material_deep_orange_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_grey_500", R.style.ThemeOverlay_material_deep_orange_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_deep_orange_500.material_blue_grey_500", R.style.ThemeOverlay_material_deep_orange_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.colorAccent", R.style.ThemeOverlay_material_brown_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_red_a200", R.style.ThemeOverlay_material_brown_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_pink_a200", R.style.ThemeOverlay_material_brown_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_purple_a200", R.style.ThemeOverlay_material_brown_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_deep_purple_a200", R.style.ThemeOverlay_material_brown_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_indigo_a200", R.style.ThemeOverlay_material_brown_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_blue_a200", R.style.ThemeOverlay_material_brown_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_light_blue_500", R.style.ThemeOverlay_material_brown_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_cyan_500", R.style.ThemeOverlay_material_brown_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_teal_500", R.style.ThemeOverlay_material_brown_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_green_500", R.style.ThemeOverlay_material_brown_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_light_green_500", R.style.ThemeOverlay_material_brown_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_lime_500", R.style.ThemeOverlay_material_brown_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_yellow_500", R.style.ThemeOverlay_material_brown_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_amber_500", R.style.ThemeOverlay_material_brown_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_orange_500", R.style.ThemeOverlay_material_brown_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_deep_orange_500", R.style.ThemeOverlay_material_brown_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_brown_500", R.style.ThemeOverlay_material_brown_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_grey_500", R.style.ThemeOverlay_material_brown_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_brown_500.material_blue_grey_500", R.style.ThemeOverlay_material_brown_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.colorAccent", R.style.ThemeOverlay_material_grey_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_red_a200", R.style.ThemeOverlay_material_grey_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_pink_a200", R.style.ThemeOverlay_material_grey_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_purple_a200", R.style.ThemeOverlay_material_grey_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_deep_purple_a200", R.style.ThemeOverlay_material_grey_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_indigo_a200", R.style.ThemeOverlay_material_grey_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_blue_a200", R.style.ThemeOverlay_material_grey_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_light_blue_500", R.style.ThemeOverlay_material_grey_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_cyan_500", R.style.ThemeOverlay_material_grey_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_teal_500", R.style.ThemeOverlay_material_grey_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_green_500", R.style.ThemeOverlay_material_grey_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_light_green_500", R.style.ThemeOverlay_material_grey_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_lime_500", R.style.ThemeOverlay_material_grey_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_yellow_500", R.style.ThemeOverlay_material_grey_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_amber_500", R.style.ThemeOverlay_material_grey_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_orange_500", R.style.ThemeOverlay_material_grey_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_deep_orange_500", R.style.ThemeOverlay_material_grey_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_brown_500", R.style.ThemeOverlay_material_grey_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_grey_500", R.style.ThemeOverlay_material_grey_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_grey_500.material_blue_grey_500", R.style.ThemeOverlay_material_grey_500_material_blue_grey_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.colorAccent", R.style.ThemeOverlay_material_blue_grey_500_colorAccent);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_red_a200", R.style.ThemeOverlay_material_blue_grey_500_material_red_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_pink_a200", R.style.ThemeOverlay_material_blue_grey_500_material_pink_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_purple_a200", R.style.ThemeOverlay_material_blue_grey_500_material_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_deep_purple_a200", R.style.ThemeOverlay_material_blue_grey_500_material_deep_purple_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_indigo_a200", R.style.ThemeOverlay_material_blue_grey_500_material_indigo_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_blue_a200", R.style.ThemeOverlay_material_blue_grey_500_material_blue_a200);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_light_blue_500", R.style.ThemeOverlay_material_blue_grey_500_material_light_blue_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_cyan_500", R.style.ThemeOverlay_material_blue_grey_500_material_cyan_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_teal_500", R.style.ThemeOverlay_material_blue_grey_500_material_teal_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_green_500", R.style.ThemeOverlay_material_blue_grey_500_material_green_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_light_green_500", R.style.ThemeOverlay_material_blue_grey_500_material_light_green_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_lime_500", R.style.ThemeOverlay_material_blue_grey_500_material_lime_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_yellow_500", R.style.ThemeOverlay_material_blue_grey_500_material_yellow_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_amber_500", R.style.ThemeOverlay_material_blue_grey_500_material_amber_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_orange_500", R.style.ThemeOverlay_material_blue_grey_500_material_orange_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_deep_orange_500", R.style.ThemeOverlay_material_blue_grey_500_material_deep_orange_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_brown_500", R.style.ThemeOverlay_material_blue_grey_500_material_brown_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_grey_500", R.style.ThemeOverlay_material_blue_grey_500_material_grey_500);
        colorThemeMap.put("ThemeOverlay.material_blue_grey_500.material_blue_grey_500", R.style.ThemeOverlay_material_blue_grey_500_material_blue_grey_500);
    }

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    private static boolean isBlackNightTheme() {
        return preferences.getBoolean("black_dark_theme", false);
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

    public static String getColorTheme(Context context) {
        String primaryColorEntryName = "colorPrimary";
        int colorPrimary = preferences.getInt("primary_color", context.getColor(R.color.colorPrimary));
        for (CustomThemeColor color : CustomThemeColors.Primary.values()) {
            if (colorPrimary == context.getColor(color.getResourceId())) {
                primaryColorEntryName = color.getResourceEntryName();
                break;
            }
        }

        String accentColorEntryName = "colorAccent";
        int colorAccent = preferences.getInt("accent_color", context.getColor(R.color.colorAccent));
        for (CustomThemeColor color : CustomThemeColors.Accent.values()) {
            if (colorAccent == context.getColor(color.getResourceId())) {
                accentColorEntryName = color.getResourceEntryName();
                break;
            }
        }
        return "ThemeOverlay." + primaryColorEntryName + "." + accentColorEntryName;
    }

    @StyleRes
    public static int getColorThemeStyleRes(Context context) {
        Integer theme = colorThemeMap.get(getColorTheme(context));
        if (theme == null) {
            return R.style.ThemeOverlay_colorPrimary_colorAccent;
        }
        return theme;
    }

}
