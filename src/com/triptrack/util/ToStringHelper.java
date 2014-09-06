package com.triptrack.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ToStringHelper {
  private ToStringHelper() {}

  public static String utcToString(long utc) {
    Calendar c = new GregorianCalendar();
    c.setTimeInMillis(utc);
    return String.format(
        "%s %02d:%02d:%02d",
        CalendarUtils.dateToString(c),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND));
  }

  public static String latLngAccToString(double lat, double lng, float acc) {
    return String.format("%.5f, %.5f (%dm)", lat, lng, (int) acc);
  }

}
