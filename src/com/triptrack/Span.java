package com.triptrack;

import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: lijie
 * Date: 5/24/13
 * Time: 10:08 AM
 */
public class Span {
    Calendar startDay = Calendar.getInstance();
    Calendar endDay = Calendar.getInstance();

    public Span() {
        startDay.set(Calendar.MINUTE, 0);
        startDay.set(Calendar.SECOND, 0);
        startDay.set(Calendar.MILLISECOND, 0);
        endDay.setTime(startDay.getTime());
    }

    public void setStartDay(Date day) {
        startDay.setTime(day);
        startDay.set(Calendar.MINUTE, 0);
        startDay.set(Calendar.SECOND, 0);
        startDay.set(Calendar.MILLISECOND, 0);
        if (!endDay.after(startDay)) {
            endDay.setTime(startDay.getTime());
        }
    }

    public void setStartDay(Calendar day) {
        startDay.setTime(day.getTime());
        startDay.set(Calendar.MINUTE, 0);
        startDay.set(Calendar.SECOND, 0);
        startDay.set(Calendar.MILLISECOND, 0);
        if (!endDay.after(startDay)) {
            endDay.setTime(startDay.getTime());
        }
    }

    public void setEndDay(Date day) {
        endDay.setTime(day);
        endDay.set(Calendar.MINUTE, 0);
        endDay.set(Calendar.SECOND, 0);
        endDay.set(Calendar.MILLISECOND, 0);
        if (!endDay.after(startDay)) {
            startDay.setTime(endDay.getTime());
        }
    }

    public void setEndDay(Calendar day) {
        endDay.setTime(day.getTime());
        endDay.set(Calendar.MINUTE, 0);
        endDay.set(Calendar.SECOND, 0);
        endDay.set(Calendar.MILLISECOND, 0);
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

    public boolean isMultiDay() {
        return (startDay.compareTo(endDay) == 0);
    }

    public void inc() {
        endDay.add(Calendar.DATE, 1);
        if (!isMultiDay()) {
            startDay.add(Calendar.DATE, 1);
        }
    }

    public void dec() {
        endDay.add(Calendar.DATE, -1);
        if (!isMultiDay()) {
            startDay.add(Calendar.DATE, -1);
        }
    }
}
