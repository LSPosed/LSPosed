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

package name.mikanoshi.customiuizer.holidays;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.lsposed.manager.R;

import name.mikanoshi.customiuizer.utils.Helpers;

public class HolidayHelper {

    public static void setup(Activity activity) {
        Helpers.detectHoliday();

        ImageView header = activity.findViewById(R.id.holiday_header);

        if (Helpers.currentHoliday == Helpers.Holidays.NEWYEAR) {
            header.setImageResource(R.drawable.newyear_header);
            header.setVisibility(View.VISIBLE);
        } else if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
            header.setImageResource(R.drawable.lunar_newyear_header);
            header.setVisibility(View.VISIBLE);
        } else {
            ((ViewGroup) header.getParent()).removeView(header);
        }
    }
}
