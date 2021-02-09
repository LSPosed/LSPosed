package name.mikanoshi.customiuizer.utils;

import java.util.Calendar;

public class Helpers {

    public static Holidays currentHoliday = Holidays.NONE;

    public enum Holidays {
        NONE, NEWYEAR, LUNARNEWYEAR
    }

    public static void detectHoliday() {
        currentHoliday = Holidays.NONE;
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int monthDay = cal.get(Calendar.DAY_OF_MONTH);
        //int year = cal.get(Calendar.YEAR);

        // Lunar NY
        if ((month == 0 && monthDay > 15) || month == 1) currentHoliday = Holidays.LUNARNEWYEAR;
            // NY
        else if (month == 0 || month == 11) currentHoliday = Holidays.NEWYEAR;
    }
}