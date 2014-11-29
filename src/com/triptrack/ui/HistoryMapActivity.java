package com.triptrack.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.timessquare.CalendarPickerView;
import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.support.AsyncTaskResult;
import com.triptrack.support.MessageType;
import com.triptrack.util.CalendarUtils;
import com.triptrack_beta.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Activity the user sees upon opening the app.
 */
public class HistoryMapActivity extends FragmentActivity {
  private static final String TAG = "HistoryMapActivity";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final int MAX_ZOOM_LEVEL = 18;

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

  private UserNotifier userNotifier;
  private GeoFixDataStore geoFixDataStore;
  private DateRange dateRange = new DateRange();
  private boolean isProcessing = false;
  // TODO(renlijie): make it thread-safe.
  private List<Fix> fixes;

  private ClusterManager<Fix> clusterManager;
  private GetDataCursorTask getDataCursorTask = null;
  private ReadDataTask readDataTask = null;
  private ClusterTask clusterTask = null;

  private class UserNotifier extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MessageType.STARTED_PROCESSING:
          ensureDatePickerShowing("Clustering...");
          break;
        case MessageType.FINISHED_PROCESSING:
          fadeOutDatePicker(CalendarUtils.dateRangeToString(dateRange) + "\n"
              + msg.arg1 + " markers + " + msg.arg2 + " clusters.");
          break;
        case MessageType.UPDATE_COUNTER:
          int count = msg.arg1;
          String remark;
          if (count > 50000) {
            if (count > 100000) {
              remark = "(ಠ益ಠ) " + count + "!!";
            } else {
              remark = "└(°o°)┘ " + count + "!";
            }
          } else {
            remark = String.valueOf(count);
          }
          datePicker.setText("Fetching records from DB...\n" + remark);
          break;
        case MessageType.INIT_ALGORITHM:
          ensureDatePickerShowing("Feeding data to algorithm...");
          break;
        case MessageType.START_DRAWING:
          ensureDatePickerShowing("Drawing...");
          break;
        case MessageType.FINISHED_DRAWING:
          fadeOutDatePicker(CalendarUtils.dateRangeToString(dateRange) + "\n"
              + fixes.size() + " fixes.");
          break;
        case MessageType.ERROR:
          ensureDatePickerShowing("Internal error:\n" + msg.obj);
          break;
        default:
          throw new RuntimeException("unknown message type: " + msg.what);
      }
    }
  }

  private class GetDataCursorTask extends AsyncTask<Void, Void, AsyncTaskResult<Cursor>> {
    private boolean drawMarkers;

    GetDataCursorTask(boolean drawMarkers) {
      this.drawMarkers = drawMarkers;
    }

    @Override
    protected void onPreExecute () {
      map.clear();
    }

    @Override
    //TODO(renlijie): cancel when changing date range.
    protected AsyncTaskResult<Cursor> doInBackground(Void[] params) {
      Cursor cursor = null;
      try {
        cursor = geoFixDataStore.getGeoFixesByDateRange(dateRange);
        fixes = new ArrayList<>(cursor.getCount());
        return new AsyncTaskResult(cursor);
      } catch (Throwable e) {
        if (cursor != null) {
          cursor.close();
        }
        return new AsyncTaskResult(e);
      }
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<Cursor> result) {
      if (result.getError() != null) {
        userNotifier.sendMessage(userNotifier.obtainMessage(
            MessageType.ERROR, result.getError().getMessage()));
        return;
      }
      if (isCancelled()) {
        return;
      }
      Cursor cursor = result.getResult();
      if (!cursor.moveToFirst()) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
            new LatLng(35.662691, 139.731127), 12)); // Roppongi!
        Toast.makeText(
            HistoryMapActivity.this,
            "no location during this period of time",
            Toast.LENGTH_SHORT).show();
        fadeOutDatePicker(CalendarUtils.dateRangeToString(dateRange) + "\n0 fix.");
      } else {
        if (readDataTask != null) {
          readDataTask.cancel(true);
        }
        readDataTask = new ReadDataTask(drawMarkers);
        readDataTask.execute(cursor);
      }
    }
  }

  private class ReadDataTask extends AsyncTask<Cursor, Integer, AsyncTaskResult<LatLngBounds>> {
    private boolean drawMarkers;

    ReadDataTask(boolean drawMarkers) {
      this.drawMarkers = drawMarkers;
    }

    @Override
    //TODO(renlijie): cancel when changing date range.
    protected AsyncTaskResult<LatLngBounds> doInBackground(Cursor... params) {
      Cursor cursor = null;
      LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
      try {
        int count = 0;
        cursor = params[0];
        while (true) {
          Fix fix = Fix.fromCursor(cursor);
          fixes.add(fix);
          boundsBuilder.include(fix.getPosition());
          count += 1;
          if (count % 1000 == 0) {
            publishProgress(count);
          }
          if (!cursor.moveToNext()) {
            break;
          }
        }
        return new AsyncTaskResult(boundsBuilder.build());
      } catch (Throwable e) {
        return new AsyncTaskResult(e);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
      userNotifier.sendMessage(userNotifier.obtainMessage(
          MessageType.UPDATE_COUNTER, progress[0], 0));
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<LatLngBounds> result) {
      if (result.getError() != null) {
        userNotifier.sendMessage(userNotifier.obtainMessage(
            MessageType.ERROR, result.getError().getMessage()));
        return;
      }
      if (isCancelled()) {
        return;
      }

      Display display = getWindowManager().getDefaultDisplay();
      Point displaySize = new Point();
      display.getSize(displaySize);
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(
          result.getResult(), displaySize.x, displaySize.y, 100));
      if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
        map.moveCamera(CameraUpdateFactory.zoomTo(MAX_ZOOM_LEVEL));
      }

      if (drawMarkers) {
        cluster();
      } else {
        drawLines();
      }
    }
  }

  private class ClusterTask extends AsyncTask<Void, Void, AsyncTaskResult<Void>> {
    @Override
    protected AsyncTaskResult<Void> doInBackground(Void... params) {
      try {
        userNotifier.sendMessage(userNotifier.obtainMessage(MessageType.INIT_ALGORITHM, 0, 0));
        clusterManager.clearItems();
        clusterManager.addItems(fixes);
        return new AsyncTaskResult(null);
      } catch (Throwable e) {
        return new AsyncTaskResult(e);
      }
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<Void> result) {
      if (result.getError() != null) {
        userNotifier.sendMessage(userNotifier.obtainMessage(
            MessageType.ERROR, result.getError().getMessage()));
        return;
      }
      if (isCancelled()) {
        return;
      }
      clusterManager.cluster();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putLong(START_DATE, dateRange.getStartDay().getTimeInMillis());
    savedInstanceState.putLong(END_DATE, dateRange.getEndDay().getTimeInMillis());

    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
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

    userNotifier = new UserNotifier();
    clusterManager = new ClusterManager<>(this, map, userNotifier);

    geoFixDataStore = new GeoFixDataStore(this);
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

  public void drawSelectedRange(View v) {
    showMapPanel();
    List<Date> dates = calendarView.getSelectedDates();

    DateRange newDateRange = new DateRange();
    newDateRange.setStartDay(dates.get(0));
    newDateRange.setEndDay(dates.get(dates.size() - 1));

    if (newDateRange.getStartDay().equals(dateRange.getStartDay())
        && newDateRange.getEndDay().equals(dateRange.getEndDay())) {
      return;
    }

    dateRange = newDateRange;
    draw(markersButton.isChecked());
  }

  public void drawPreviousDay(View v) {
    if (resetToPreviousDay()) {
      draw(markersButton.isChecked());
    } else {
      Toast.makeText(this, "No earlier fix was recorded.", Toast.LENGTH_SHORT).show();
    }
  }

  public void drawNextDay(View v) {
    if (resetToNextDay()) {
      draw(markersButton.isChecked());
    } else {
      Toast.makeText(this, "No later fix was recorded.", Toast.LENGTH_SHORT).show();
    }
  }

  public void jumpToEarliestDay(View v) {
    Calendar day = geoFixDataStore.earliestRecordDay();
    calendarView.selectDate(day == null
        ? CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime()
        : day.getTime());
  }

  public void toggleMarkers(View v) {
    if(markersButton.isChecked()) {
      map.setOnCameraChangeListener(clusterManager);
      cluster();
    } else {
      map.setOnCameraChangeListener(null);
      drawLines();
    }
  }

  private void drawLines() {
    userNotifier.sendEmptyMessage(MessageType.START_DRAWING);

    PolylineOptions lineOpt = new PolylineOptions().width(4).color(Color.argb(128, 0, 0, 255));
    for (Fix fix : fixes) {
      lineOpt.add(fix.getPosition());
//      CircleOptions circleOpt = new CircleOptions()
//          .center(fix.getPosition())
//          .radius(fix.getAcc())
//          .strokeWidth(2);
         // .strokeColor(Color.argb(128, 255 - fix.getFreshness(), fix.getFreshness(), 0));
//      map.addCircle(circleOpt);
    }
    map.addPolyline(lineOpt);

    userNotifier.sendEmptyMessage(MessageType.FINISHED_DRAWING);
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

  public void jumpToToday(View v) {
    calendarView.selectDate(CalendarUtils.toBeginningOfDay(Calendar.getInstance()).getTime());
  }

  private boolean resetToNextDay() {
    Calendar nextDay;
    nextDay = geoFixDataStore.nextRecordDay(dateRange.getEndDay());
    if (nextDay != null) {
      dateRange.setStartDay(nextDay);
      return true;
    }
    return false;
  }

  private boolean resetToPreviousDay() {
    Calendar prevDay;
    prevDay = geoFixDataStore.prevRecordDay(dateRange.getStartDay());
    if (prevDay != null) {
      dateRange.setEndDay(prevDay);
      return true;
    }
    return false;
  }

  private void fadeOutButtons() {
    Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
    fadeOutAnimation.setStartOffset(2000);
    fadeOutAnimation.setDuration(2000);
    fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationEnd(Animation a) {
        if (!isProcessing) {
          datePicker.setVisibility(View.INVISIBLE);
        }
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

  private void setMapFragmentVisibility(int visibility) {
    getSupportFragmentManager().findFragmentById(R.id.map).getView().setVisibility(visibility);
  }

  private void draw(boolean drawMarkers) {
    ensureDatePickerShowing("Fetching records from DB...");

    if (getDataCursorTask != null) {
      getDataCursorTask.cancel(true);
    }
    getDataCursorTask = new GetDataCursorTask(drawMarkers);
    getDataCursorTask.execute();
  }

  private void cluster() {
    if (clusterTask != null) {
      clusterTask.cancel(true);
    }
    clusterTask = new ClusterTask();
    clusterTask.execute();
  }

  private void fadeOutDatePicker(String text) {
    datePicker.setText(text);
    isProcessing = false;
    fadeOutButtons();
  }

  private void ensureDatePickerShowing(String text) {
    isProcessing = true;
    datePicker.setText(text);
    datePicker.clearAnimation();
    datePicker.setVisibility(View.VISIBLE);
  }
}
