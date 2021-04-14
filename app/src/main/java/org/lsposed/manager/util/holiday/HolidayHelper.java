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

package org.lsposed.manager.util.holiday;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.lsposed.manager.R;

public class HolidayHelper {

    public static void setup(Activity activity) {
        Helpers.detectHoliday();

        ImageView header = activity.findViewById(R.id.holiday_header);

        switch (Helpers.currentHoliday) {
            case NEWYEAR:
                header.setLayoutParams(
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                header.setImageResource(R.drawable.newyear_header);
                header.setVisibility(View.VISIBLE);
                break;
            case LUNARNEWYEAR:
                header.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                header.setImageResource(R.drawable.lunar_newyear_header);
                header.setVisibility(View.VISIBLE);
                ((FrameLayout) header.getParent()).requestLayout();
                break;
            case PRIDE:
                header.setForeground(new ColorDrawable(0x15000000));
                header.setImageResource(R.drawable.pride_flag);
                header.setVisibility(View.VISIBLE);
                break;
            case NONE:
            default:
                ((ViewGroup) header.getParent()).removeView(header);
        }
    }

    public static CardColors getHolidayColors() {
        switch (Helpers.currentHoliday) {
            case LUNARNEWYEAR:
                return new CardColors(0xfff05654, 0);
            case NEWYEAR:
                return new CardColors(0xff677a89, 0);
            case PRIDE:
                return new CardColors(0, 0xffffffff);
            case NONE:
            default:
                return new CardColors(0, 0);
        }
    }

    public static class CardColors {
        public int backgroundColor;
        public int textColor;

        public CardColors(int background, int text) {
            backgroundColor = background;
            textColor = text;
        }
    }
}
