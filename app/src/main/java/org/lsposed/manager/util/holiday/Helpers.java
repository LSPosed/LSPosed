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

import java.util.Calendar;

public class Helpers {

    public static Holidays currentHoliday = Holidays.NONE;

    public enum Holidays {
        NONE, NEWYEAR, LUNARNEWYEAR, PRIDE
    }

    public static void detectHoliday() {
        currentHoliday = Holidays.NONE;
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int monthDay = cal.get(Calendar.DAY_OF_MONTH);
        //int year = cal.get(Calendar.YEAR);

        // Lunar NY
        if ((month == 0 && monthDay > 15) || month == 1) {
            currentHoliday = Holidays.LUNARNEWYEAR;
        } else if (month == 0 || month == 11) { // NY
            currentHoliday = Holidays.NEWYEAR;
        } else if (month == 5) { // Pride Month
            currentHoliday = Holidays.PRIDE;
        }
    }
}
