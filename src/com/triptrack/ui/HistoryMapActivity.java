package com.triptrack.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ToggleButton;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.timessquare.CalendarPickerView;
import com.triptrack.DateRange;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;
import com.triptrack_beta.R;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity the user sees upon opening the app.
 *
 * TODO: handle orientation changes by NOT re-drawing everything.
 */
public class HistoryMapActivity extends FragmentActivity {
  private static final String TAG = "HistoryMapActivity";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";

  // Map Panel
  private GoogleMap map;
  private Button datePicker;
  private Button settingsButton;
  private Button previousDayButton;
  private Button nextDayButton;
  private ToggleButton markersButton;

  // Calendar Panel
  private CalendarPickerView calendarView;
  private Button drawButton;
  private Button earliestDayButton;
  private Button todayButton;

  private GeoFixDataStore geoFixDataStore = new GeoFixDataStore(this);
  private DateRange dateRange = new DateRange();
  private FixVisualizer fixVisualizer;

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putLong(START_DATE, dateRange.getStartDay().getTimeInMillis());
    savedInstanceState.putLong(END_DATE, dateRange.getEndDay().getTimeInMillis());

    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      dateRange.setStartDay(savedInstanceState.getLong(START_DATE));
      dateRange.setEndDay(savedInstanceState.getLong(END_DATE));
    }

    setContentView(R.layout.history_map_activity);

    map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
    if (map == null) {
      // TODO: show notification
      return;
    }

    geoFixDataStore.open();

    map.setMyLocationEnabled(true);
    map.setTrafficEnabled(false);
    map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
      @Override
      public void onMapClick(LatLng point) {
        fadeOutButtons();
      }
    });

    datePicker = (Button) findViewById(R.id.date_picker);
    settingsButton = (Button) findViewById(R.id.settings);
    previousDayButton = (Button) findViewById(R.id.previous_day);
    nextDayButton = (Button) findViewById(R.id.next_day);
    markersButton = (ToggleButton) findViewById(R.id.draw_markers);

    calendarView = (CalendarPickerView) findViewById(R.id.calendar);
    calendarView.setFastScrollEnabled(true);

    markersButton.setChecked(false);
    drawButton = (Button) findViewById(R.id.draw);
    earliestDayButton = (Button) findViewById(R.id.earliest);
    todayButton = (Button) findViewById(R.id.today);

    fixVisualizer = new FixVisualizer(this, map, datePicker);

    drawFixes(markersButton.isChecked());
  }

  // Override BACK key's behavior.
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && (calendarView.getVisibility() == View.VISIBLE)) {
      showMapPanel();
      fadeOutButtons();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onDestroy() {
    geoFixDataStore.close();
    super.onDestroy();
  }

  public void showPreviousDay(View v) {
    decreasePreviousDay();
    drawFixes(markersButton.isChecked());
  }

  public void showNextDay(View v) {
    increaseNextDay();
    drawFixes(markersButton.isChecked());
  }

  public void drawFixes(View v) {
    showMapPanel();
    List<Date> dates = calendarView.getSelectedDates();
    dateRange.setStartDay(dates.get(0));
    dateRange.setEndDay(dates.get(dates.size() - 1));
    drawFixes(markersButton.isChecked());
  }

  public void toEarliestDay(View v) {
    Calendar day = geoFixDataStore.earliestRecordDay();
    calendarView.selectDate(day == null ?
        CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime() : day.getTime());
  }

  public void toToday(View v) {
    calendarView.selectDate(CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime());
  }

  public void increaseNextDay() {
    Calendar nextDay;
    nextDay = geoFixDataStore.nextRecordDay(dateRange.getEndDay());
    if (nextDay != null) {
      dateRange.setStartDay(nextDay);
    }
  }

  public void decreasePreviousDay() {
    Calendar prevDay;
    prevDay = geoFixDataStore.prevRecordDay(dateRange.getStartDay());
    if (prevDay != null) {
      dateRange.setEndDay(prevDay);
    }
  }

  public void drawFixes(boolean drawMarkers) {
    Cursor c = geoFixDataStore.getGeoFixesByDateRange(dateRange);
    fixVisualizer.draw(c, dateRange, drawMarkers);
  }

  void fadeOutButtons() {
    Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
    fadeOutAnimation.setStartOffset(2000);
    fadeOutAnimation.setDuration(2000);
    fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationEnd(Animation a) {
        datePicker.setVisibility(View.INVISIBLE);
        settingsButton.setVisibility(View.INVISIBLE);
        previousDayButton.setVisibility(View.INVISIBLE);
        nextDayButton.setVisibility(View.INVISIBLE);
        markersButton.setVisibility(View.INVISIBLE);
      }

      @Override
      public void onAnimationRepeat(Animation a) {
      }

      @Override
      public void onAnimationStart(Animation a) {
        datePicker.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
        previousDayButton.setVisibility(View.VISIBLE);
        nextDayButton.setVisibility(View.VISIBLE);
        markersButton.setVisibility(View.VISIBLE);
      }
    });

    datePicker.startAnimation(fadeOutAnimation);
    settingsButton.startAnimation(fadeOutAnimation);
    previousDayButton.startAnimation(fadeOutAnimation);
    nextDayButton.startAnimation(fadeOutAnimation);
    markersButton.startAnimation(fadeOutAnimation);
  }

  private void showMapPanel() {
    calendarView.setVisibility(View.GONE);
    drawButton.setVisibility(View.GONE);
    earliestDayButton.setVisibility(View.GONE);
    todayButton.setVisibility(View.GONE);

    setMapFragmentVisibility(View.VISIBLE);
  }

  public void toggleMarkers(View v) {
    // updateDrawing(markersButton.isChecked());
  }

  public void showSettings(View v) {
    startActivity(new Intent(this, ControlPanelActivity.class));
  }

  public void showCalendarPanel(View view) {
    datePicker.clearAnimation();
    settingsButton.clearAnimation();
    previousDayButton.clearAnimation();
    nextDayButton.clearAnimation();
    markersButton.clearAnimation();

    setMapFragmentVisibility(View.INVISIBLE);

    Calendar tomorrow = Calendar.getInstance();
    tomorrow.add(Calendar.DATE, 1);
    Calendar earliestDay = geoFixDataStore.earliestRecordDay();
    calendarView.init(
        earliestDay == null
            ? CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime()
            : earliestDay.getTime(),
        tomorrow.getTime())
        .inMode(CalendarPickerView.SelectionMode.RANGE);
    calendarView.selectDate(dateRange.getStartDay().getTime());
    calendarView.selectDate(dateRange.getEndDay().getTime());

    calendarView.setVisibility(View.VISIBLE);
    drawButton.setVisibility(View.VISIBLE);
    earliestDayButton.setVisibility(View.VISIBLE);
    todayButton.setVisibility(View.VISIBLE);
  }

  private void setMapFragmentVisibility(int visibility) {
    getSupportFragmentManager().findFragmentById(R.id.map).getView().setVisibility(visibility);
  }
}
