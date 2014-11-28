package com.triptrack.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.timessquare.CalendarPickerView;
import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;
import com.triptrack.util.Cursors;
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
 // private FixVisualizer fixVisualizer;
  private boolean isProcessing = false;



  private ClusterManager<Fix> clusterManager;

  private class UserNotifier extends Handler {
    @Override
    public void handleMessage(Message msg) {
      if (msg.what == ClusterManager.STARTED_PROCESSING) {
        datePicker.setVisibility(View.VISIBLE);
        datePicker.setText("Processing...");
        isProcessing = true;
      } else if (msg.what == ClusterManager.FINISHED_PROCESSING) {
        datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
            + "\n" + msg.arg1 + " markers + " + msg.arg2 + " clusters.");
        fadeOutButtons();
      }
    }
  }

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
    clusterManager = new ClusterManager<>(this, map, new UserNotifier());
    map.setOnCameraChangeListener(clusterManager);


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

    //fixVisualizer = new FixVisualizer(this, map, datePicker);
    draw(markersButton.isChecked());



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

    draw(markersButton.isChecked());
  }

  public void showNextDay(View v) {
    increaseNextDay();

    draw(markersButton.isChecked());
  }

  public void drawFixes(View v) {
    showMapPanel();
    List<Date> dates = calendarView.getSelectedDates();
    dateRange.setStartDay(dates.get(0));
    dateRange.setEndDay(dates.get(dates.size() - 1));

    draw(markersButton.isChecked());
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
      public void onAnimationRepeat(Animation a) {}

      @Override
      public void onAnimationStart(Animation a) {
        datePicker.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
        previousDayButton.setVisibility(View.VISIBLE);
        nextDayButton.setVisibility(View.VISIBLE);
        markersButton.setVisibility(View.VISIBLE);
      }
    });

    if (!isProcessing) {
      datePicker.startAnimation(fadeOutAnimation);
    }
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
        .inMode(CalendarPickerView.SelectionMode.SELECTED_PERIOD);
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




  private void draw(boolean drawMarkers) {
    Cursor rows = geoFixDataStore.getGeoFixesByDateRange(dateRange);
 //   ArrayList<Fix> fixes = new ArrayList<>();

    if (!rows.moveToLast()) { // move to the oldest fix.
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 3));
      Toast.makeText(
          this,
          "no location during this period of time",
          Toast.LENGTH_SHORT).show();
      datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
          + "\n0 markers + 0 clusters.");
      return;
    }

 //   LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    clusterManager.clearItems();


    while (true) {
      double lat = Cursors.getLat(rows);
      double lng = Cursors.getLng(rows);
      float acc = Cursors.getAcc(rows);
      long utc = Cursors.getUtc(rows);

  //    boundsBuilder.include(new LatLng(lat, lng));

 //     fixes.add(new Fix(utc, lat, lng, acc, 50));
      clusterManager.addItem(new Fix(utc, lat, lng, acc));
      if (rows.isFirst()) {
        rows.close();
        break;
      }
      rows.moveToPrevious();
    }
    map.clear();



//    Display display = mapActivity.getWindowManager().getDefaultDisplay();
//    Point displaySize = new Point();
//    display.getSize(displaySize);
//    map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), displaySize.x, displaySize.y, 100));
//    if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
//      map.moveCamera(CameraUpdateFactory.zoomTo(MAX_ZOOM_LEVEL));
//    }
    clusterManager.cluster();

//    PolylineOptions lineOpt = new PolylineOptions()
//        .width(4).color(Color.argb(128, 0, 0, 255));
//    for (Fix fix : fixes) {
//      lineOpt.add(fix.getPosition());
//      CircleOptions circleOpt = new CircleOptions()
//          .center(fix.getPosition())
//          .radius(fix.getAcc())
//          .strokeWidth(2);
//         // .strokeColor(Color.argb(128, 255 - fix.getFreshness(), fix.getFreshness(), 0));
//      map.addCircle(circleOpt);
//
//      if (drawMarkers) {
//        Marker marker = map.addMarker(new MarkerOptions()
//            .position(fix.getPosition())
//            .title(ToStringHelper.utcToString(fix.getUtc()))
//            .snippet(ToStringHelper.latLngAccToString(
//                fix.getLat(), fix.getLng(), fix.getAcc()))
//            .icon(MARKER_ICON));
//        markerIdToUtc.put(marker.getId(), fix.getUtc());
//      }
//    }
//    map.addPolyline(lineOpt);
  }
}
