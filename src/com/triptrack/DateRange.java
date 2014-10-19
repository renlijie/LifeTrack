package com.triptrack;

import com.triptrack.util.CalendarUtils;

import java.util.Calendar;
import java.util.Date;


public class DateRange {
  Calendar startDay = Calendar.getInstance();
  Calendar endDay = Calendar.getInstance();

  public DateRange() {
    endDay.setTime(CalendarUtils.toBeginningOfDay(startDay).getTime());
  }

  public void setStartDay(Date day) {
    startDay.setTime(day);
    CalendarUtils.toBeginningOfDay(startDay);
    if (!endDay.after(startDay)) {
      endDay.setTime(startDay.getTime());
    }
  }

  public void setEndDay(Date day) {
    endDay.setTime(day);
    CalendarUtils.toBeginningOfDay(endDay);
    if (!endDay.after(startDay)) {
      startDay.setTime(endDay.getTime());
    }
  }

  public void setStartDay(long millis) {
    startDay.setTimeInMillis(millis);
    CalendarUtils.toBeginningOfDay(startDay);
    if (!endDay.after(startDay)) {
      endDay.setTime(startDay.getTime());
    }
  }

  public void setEndDay(long millis) {
    endDay.setTimeInMillis(millis);
    CalendarUtils.toBeginningOfDay(endDay);
    if (!endDay.after(startDay)) {
      startDay.setTime(endDay.getTime());
    }
  }

  public Calendar getStartDay() {
    return startDay;
  }

  public void setStartDay(Calendar day) {
    startDay.setTime(day.getTime());
    CalendarUtils.toBeginningOfDay(startDay);
    if (!endDay.after(startDay)) {
      endDay.setTime(startDay.getTime());
    }
  }

  public Calendar getEndDay() {
    return endDay;
  }

  public void setEndDay(Calendar day) {
    endDay.setTime(day.getTime());
    CalendarUtils.toBeginningOfDay(endDay);
    if (!endDay.after(startDay)) {
      startDay.setTime(endDay.getTime());
    }
  }
}
