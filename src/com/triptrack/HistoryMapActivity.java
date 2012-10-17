package com.triptrack;

import java.util.Calendar;
import java.util.List;

import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class HistoryMapActivity extends MapActivity {
  private static final String TAG = "HistoryMap";
  private static final String[] strDays = new String[] { "Sun.", "Mon.", "Tue.", "Wed.", "Thu.", "Fri.", "Sat." };

  private MapView mapView;
  private TextView dateView;
  private List<Overlay> mapOverlays;
  private Calendar calendar;
  private boolean allDays;

  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  static String printDate(Calendar calendar) {
    if (calendar == null) {
      return "ALL DAYS";
    }

    return calendar.get(Calendar.YEAR) + "/"
      + String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "/"
      + String.format("%02d", calendar.get(Calendar.DATE)) + ", "
      + strDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
  }

  void prepareRows(int direction, final boolean firstTime, boolean move) {
    FixDataStore fixDataStore = new FixDataStore(this);
    fixDataStore.open();

    if (direction == 1) {
      Calendar cal;
      cal = fixDataStore.nextRecordDay(calendar);
      if (cal != null) {
        calendar = cal;
      }
    } else if (direction == -1) {
      Calendar cal;
      cal = fixDataStore.previousRecordDay(calendar);
      if (cal != null) {
        calendar = cal;
      }
    }

    Cursor c;
    if (allDays) {
      c = fixDataStore.fetchFixes(null);
    } else {
      c = fixDataStore.fetchFixes(calendar);
    }

    if (!firstTime) {
      FixOverlay fixOverlay = (FixOverlay) (mapOverlays.get(mapOverlays.size() - 1));
      fixOverlay.close(); // only close once
      mapOverlays.remove(mapOverlays.size() - 1);
    } else {
      mapOverlays = mapView.getOverlays();
    }
    FixOverlay f;
    if(allDays) {
      f = new FixOverlay(this, c, null, this);
      mapOverlays.add(f);
    } else {
      f = new FixOverlay(this, c, calendar, this);
      mapOverlays.add(f);
    }

    if (allDays) {
      dateView.setText(" " + printDate(null) + ", " + c.getCount() + " fixes. \n "
        + f.size() + " fixes drawn. \n " + "Older   fixes:    red. \n Newer fixes:    green. ");
    } else {
      dateView.setText(" " + printDate(calendar) + ", " + c.getCount() + " fixes. \n "
        + f.size() + " fixes drawn. \n " + "0am -> 12pm:    red. \n 12pm -> 0am:    green. ");
    }
    fixDataStore.close();

    if(move) {
      double lat, lng;
      if (c.moveToFirst()) {
        lat = c.getDouble(c.getColumnIndex(Constants.KEY_LAT));
        lng = c.getDouble(c.getColumnIndex(Constants.KEY_LNG));
      } else {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownLocation == null) {
          lat = 0;
          lng = 0;
        } else {
          lat = lastKnownLocation.getLatitude();
          lng = lastKnownLocation.getLongitude();
        }
      }
      double latSpanE6 = f.getLatSpan() * 1E6;
      double lngSpanE6 = f.getLngSpan() * 1E6;
      double cenLatE6 = f.getCenLat() * 1E6;
      double cenLngE6 = f.getCenLng() * 1E6;
      mapView.getController().zoomToSpan((int)(latSpanE6 * 1.5), (int)(lngSpanE6 * 1.1));
      GeoPoint gFix = new GeoPoint((int)(cenLatE6), (int)(cenLngE6));
      mapView.getController().animateTo(gFix);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.map);

    mapView = (MapView) findViewById(R.id.mapview);
    mapView.setBuiltInZoomControls(true);

    dateView = (TextView) findViewById(R.id.date);
    dateView.setTextColor(Color.BLACK);
    dateView.setBackgroundColor(Color.GRAY);

    calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    allDays = false;
    prepareRows(0, true, true);

    Button previousDayButton = (Button) findViewById(R.id.previous_day);
    previousDayButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        allDays = false;
        prepareRows(-1, false, true);
      }
    });

    Button nextDayButton = (Button) findViewById(R.id.next_day);
    nextDayButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        allDays = false;
        prepareRows(1, false, true);
      }
    });

    Button allDaysButton = (Button) findViewById(R.id.all_days);
    allDaysButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        allDays = true;
        prepareRows(0, false, true);
      }
    });
  }
}
