package com.triptrack.util;

import com.triptrack.DateRange;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarUtils {
  private CalendarUtils() {}

  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy/MM/dd, EEE");

  public static String dateToString(Calendar date) {
    return DATE_FORMAT.format(date.getTime());
  }

  public static String dateRangeToString(DateRange dateRange) {
    return dateToString(dateRange.getStartDay()) + " ~ "
        + dateToString(dateRange.getEndDay());
  }

  public static Calendar toBeginningOfDay(Calendar day) {
    day.set(Calendar.HOUR_OF_DAY, 0);
    day.set(Calendar.MINUTE, 0);
    day.set(Calendar.SECOND, 0);
    day.set(Calendar.MILLISECOND, 0);
    return day;
  }
}
