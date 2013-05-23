package com.triptrack;

import java.util.Calendar;

public class CalendarHelper {
    private static final String[] strDays = new String[]{"Sun.", "Mon.", "Tue.", "Wed.", "Thu.", "Fri.", "Sat."};

    static String prettyDate(Calendar date) {
        return date.get(Calendar.YEAR) + "/"
                + String.format("%02d", (date.get(Calendar.MONTH) + 1)) + "/"
                + String.format("%02d", date.get(Calendar.DATE)) + ", "
                + strDays[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    static String prettyInterval(Calendar firstDay, Calendar lastDay) {
        return prettyDate(firstDay) + " ~ " + prettyDate(lastDay);
    }
}
