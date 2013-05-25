package com.triptrack;

import java.util.Calendar;

public class CalendarHelper {
    private static final String[] strDays = new String[]{"Sun.", "Mon.", "Tue.", "Wed.", "Thu.", "Fri.", "Sat."};

    public static String prettyDate(Calendar date) {
        return date.get(Calendar.YEAR) + "/"
                + String.format("%02d", (date.get(Calendar.MONTH) + 1)) + "/"
                + String.format("%02d", date.get(Calendar.DATE)) + ", "
                + strDays[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String prettyInterval(Calendar firstDay, Calendar lastDay) {
        return prettyDate(firstDay) + " ~ " + prettyDate(lastDay);
    }

    public static String prettyInterval(Span span) {
        return prettyInterval(span.getStartDay(), span.getEndDay());
    }

    public static void toBeginningOfDay(Calendar day) {
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
    }
}
