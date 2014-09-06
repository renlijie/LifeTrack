package com.triptrack.ui;

import com.squareup.timessquare.CalendarPickerView;
import com.triptrack.DateRange;
import com.triptrack.R;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity the user sees upon opening the app.
 *
 * TODO: handle orientation changes by NOT re-drawing everything.
 */
public class HistoryMapActivity extends Activity {
  private static final String TAG = "HistoryMapActivity";

  // Map Panel
  private GoogleMap map;
  private Button datePicker;
  private Button settingsButton;
  private Button previousDayButton;
  private Button nextDayButton;

  // Calendar Panel
  private CalendarPickerView calendarView;
  private ToggleButton markersButton;
  private Button drawButton;
  private Button earliestDayButton;
  private Button todayButton;

  // datastore and internal states
  private GeoFixDataStore geoFixDataStore = new GeoFixDataStore(this);
  private DateRange dateRange = new DateRange();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(com.triptrack.R.layout.history_map_activity);
    geoFixDataStore.open();

    map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
        .getMap();
    if (map == null) {
      // TODO: show notification
      return;
    }
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

    map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
      @Override
      public void onMapLoaded() {
        updateDrawing(0, markersButton.isChecked());
      }
    });

    fadeOutButtons();

    datePicker.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showCalendarPanel();
      }
    });

    settingsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        HistoryMapActivity.this.startActivity(
            new Intent(HistoryMapActivity.this, ControlPanelActivity.class));
      }
    });

    markersButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateDrawing(0, markersButton.isChecked());
        fadeOutButtons();
      }
    });

    previousDayButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateDrawing(-1, markersButton.isChecked());
        fadeOutButtons();
      }
    });

    nextDayButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateDrawing(1, markersButton.isChecked());
        fadeOutButtons();
      }
    });

    drawButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showMapPanel();
        List<Date> dates = calendarView.getSelectedDates();
        dateRange.setStartDay(dates.get(0));
        dateRange.setEndDay(dates.get(dates.size() - 1));
        drawFixes(dateRange, markersButton.isChecked());
      }
    });

    earliestDayButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Calendar day = geoFixDataStore.earliestRecordDay();
        calendarView.selectDate(day == null ?
            CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime() : day.getTime());
      }
    });

    todayButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        calendarView.selectDate(CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime());
      }
    });
  }

  // Override BACK key's behavior.
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && (calendarView.getVisibility() == View.VISIBLE)) {
      showMapPanel();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onDestroy() {
    geoFixDataStore.close();
    super.onDestroy();
  }

  void updateDrawing(int direction, boolean drawMarkers) {
    if (direction == 1) {
      geoFixDataStore.plusOneDay(dateRange);
    } else if (direction == -1) {
      geoFixDataStore.minusOneDay(dateRange);
    }
    drawFixes(dateRange, drawMarkers);
  }

  private void drawFixes(DateRange dateRange, boolean drawMarkers) {
    Cursor c = geoFixDataStore.getGeoFixesByDateRange(dateRange);
    FixVisualizer fixVisualizer = new FixVisualizer(c, this, map, drawMarkers);
    fixVisualizer.draw();

    datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
        + "\n" + fixVisualizer.numFarAwayFixes() + " out of "
        + c.getCount());
    if (fixVisualizer.numFarAwayFixes() == 0) {
      Toast.makeText(
          this,
          "no location during this period of time",
          Toast.LENGTH_SHORT).show();
    }
  }

  private void fadeOutButtons() {
    Animation buttonFadeOut = new AlphaAnimation(0.6f, 0.0f);
    buttonFadeOut.setStartOffset(2000);
    buttonFadeOut.setDuration(2000);
    buttonFadeOut.setAnimationListener(new AnimationListener() {
      @Override
      public void onAnimationEnd(Animation a) {
        settingsButton.setVisibility(View.INVISIBLE);
        previousDayButton.setVisibility(View.INVISIBLE);
        nextDayButton.setVisibility(View.INVISIBLE);
        markersButton.setVisibility(View.INVISIBLE);
        datePicker.setVisibility(View.GONE);
      }

      @Override
      public void onAnimationRepeat(Animation a) {}

      @Override
      public void onAnimationStart(Animation a) {}
    });

    settingsButton.startAnimation(buttonFadeOut);
    previousDayButton.startAnimation(buttonFadeOut);
    nextDayButton.startAnimation(buttonFadeOut);
    markersButton.startAnimation(buttonFadeOut);
    datePicker.startAnimation(buttonFadeOut);
  }

  private void showMapPanel() {
    calendarView.setVisibility(View.GONE);
    markersButton.setVisibility(View.GONE);
    drawButton.setVisibility(View.GONE);
    earliestDayButton.setVisibility(View.GONE);
    todayButton.setVisibility(View.GONE);

    setMapFragmentVisibility(View.VISIBLE);
    fadeOutButtons();
  }

  private void showCalendarPanel() {
    datePicker.clearAnimation();
    settingsButton.clearAnimation();
    previousDayButton.clearAnimation();
    nextDayButton.clearAnimation();
    markersButton.clearAnimation();

    setMapFragmentVisibility(View.INVISIBLE);
    settingsButton.setVisibility(View.GONE);

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
    getFragmentManager().findFragmentById(R.id.map).getView()
        .setVisibility(visibility);
  }
}
