package com.triptrack;

import java.util.Calendar;
import java.util.Date;

/**
 * User: lijie
 * Date: 5/24/13
 * Time: 10:08 AM
 */
public class Span {
    Calendar startDay = Calendar.getInstance();
    Calendar endDay = Calendar.getInstance();

    public Span() {
        endDay.setTime(CalendarHelper.toBeginningOfDay(startDay).getTime());
    }

    public void setStartDay(Date day) {
        startDay.setTime(day);
        CalendarHelper.toBeginningOfDay(startDay);
        if (!endDay.after(startDay)) {
            endDay.setTime(startDay.getTime());
        }
    }

    public void setStartDay(Calendar day) {
        startDay.setTime(day.getTime());
        CalendarHelper.toBeginningOfDay(startDay);
        if (!endDay.after(startDay)) {
            endDay.setTime(startDay.getTime());
        }
    }

    public void setEndDay(Date day) {
        endDay.setTime(day);
        CalendarHelper.toBeginningOfDay(endDay);
        if (!endDay.after(startDay)) {
            startDay.setTime(endDay.getTime());
        }
    }

    public void setEndDay(Calendar day) {
        endDay.setTime(day.getTime());
        CalendarHelper.toBeginningOfDay(endDay);
        if (!endDay.after(startDay)) {
            startDay.setTime(endDay.getTime());
        }
    }

    public Calendar getStartDay() {
        return startDay;
    }

    public Calendar getEndDay() {
        return endDay;
    }

    public boolean isSingleDay() {
        return (startDay.compareTo(endDay) == 0);
    }
}
