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

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import io.github.lsposed.manager.R;

public class CustomThemeColors {

    private CustomThemeColors() {
    }

    public enum Primary implements CustomThemeColor {

        COLORPRIMARY(R.color.colorPrimary, "colorPrimary"),
        MATERIAL_RED_500(R.color.material_red_500, "material_red_500"),
        MATERIAL_PINK_500(R.color.material_pink_500, "material_pink_500"),
        MATERIAL_PURPLE_500(R.color.material_purple_500, "material_purple_500"),
        MATERIAL_DEEP_PURPLE_500(R.color.material_deep_purple_500, "material_deep_purple_500"),
        MATERIAL_INDIGO_500(R.color.material_indigo_500, "material_indigo_500"),
        MATERIAL_BLUE_500(R.color.material_blue_500, "material_blue_500"),
        MATERIAL_LIGHT_BLUE_500(R.color.material_light_blue_500, "material_light_blue_500"),
        MATERIAL_CYAN_500(R.color.material_cyan_500, "material_cyan_500"),
        MATERIAL_TEAL_500(R.color.material_teal_500, "material_teal_500"),
        MATERIAL_GREEN_500(R.color.material_green_500, "material_green_500"),
        MATERIAL_LIGHT_GREEN_500(R.color.material_light_green_500, "material_light_green_500"),
        MATERIAL_LIME_500(R.color.material_lime_500, "material_lime_500"),
        MATERIAL_YELLOW_500(R.color.material_yellow_500, "material_yellow_500"),
        MATERIAL_AMBER_500(R.color.material_amber_500, "material_amber_500"),
        MATERIAL_ORANGE_500(R.color.material_orange_500, "material_orange_500"),
        MATERIAL_DEEP_ORANGE_500(R.color.material_deep_orange_500, "material_deep_orange_500"),
        MATERIAL_BROWN_500(R.color.material_brown_500, "material_brown_500"),
        MATERIAL_GREY_500(R.color.material_grey_500, "material_grey_500"),
        MATERIAL_BLUE_GREY_500(R.color.material_blue_grey_500, "material_blue_grey_500");

        @ColorRes
        private final int mResourceId;
        @NonNull
        private final String mResourceEntryName;

        Primary(@ColorRes int resourceId, @NonNull String resourceEntryName) {
            mResourceId = resourceId;
            mResourceEntryName = resourceEntryName;
        }

        @ColorRes
        @Override
        public int getResourceId() {
            return mResourceId;
        }

        @NonNull
        @Override
        public String getResourceEntryName() {
            return mResourceEntryName;
        }
    }

    public enum Accent implements CustomThemeColor {

        COLORACCENT(R.color.colorAccent, "colorAccent"),
        MATERIAL_RED_A200(R.color.material_red_a200, "material_red_a200"),
        MATERIAL_PINK_A200(R.color.material_pink_a200, "material_pink_a200"),
        MATERIAL_PURPLE_A200(R.color.material_purple_a200, "material_purple_a200"),
        MATERIAL_DEEP_PURPLE_A200(R.color.material_deep_purple_a200, "material_deep_purple_a200"),
        MATERIAL_INDIGO_A200(R.color.material_indigo_a200, "material_indigo_a200"),
        MATERIAL_BLUE_A200(R.color.material_blue_a200, "material_blue_a200"),
        MATERIAL_LIGHT_BLUE_500(R.color.material_light_blue_500, "material_light_blue_500"),
        MATERIAL_CYAN_500(R.color.material_cyan_500, "material_cyan_500"),
        MATERIAL_TEAL_500(R.color.material_teal_500, "material_teal_500"),
        MATERIAL_GREEN_500(R.color.material_green_500, "material_green_500"),
        MATERIAL_LIGHT_GREEN_500(R.color.material_light_green_500, "material_light_green_500"),
        MATERIAL_LIME_500(R.color.material_lime_500, "material_lime_500"),
        MATERIAL_YELLOW_500(R.color.material_yellow_500, "material_yellow_500"),
        MATERIAL_AMBER_500(R.color.material_amber_500, "material_amber_500"),
        MATERIAL_ORANGE_500(R.color.material_orange_500, "material_orange_500"),
        MATERIAL_DEEP_ORANGE_500(R.color.material_deep_orange_500, "material_deep_orange_500"),
        MATERIAL_BROWN_500(R.color.material_brown_500, "material_brown_500"),
        MATERIAL_GREY_500(R.color.material_grey_500, "material_grey_500"),
        MATERIAL_BLUE_GREY_500(R.color.material_blue_grey_500, "material_blue_grey_500");

        @ColorRes
        private final int mResourceId;
        @NonNull
        private final String mResourceEntryName;

        Accent(@ColorRes int resourceId, @NonNull String resourceEntryName) {
            mResourceId = resourceId;
            mResourceEntryName = resourceEntryName;
        }

        @ColorRes
        @Override
        public int getResourceId() {
            return mResourceId;
        }

        @NonNull
        @Override
        public String getResourceEntryName() {
            return mResourceEntryName;
        }
    }
}
