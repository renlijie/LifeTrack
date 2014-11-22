package com.triptrack.ui;

import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack_beta.R;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;
import com.triptrack.util.Constants;
import com.triptrack.util.ToStringHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FixVisualizer {
  private static final String TAG = "FixVisualizer";
  private static final int MAX_NUM_MARKERS = 500;
  private static final BitmapDescriptor MARKER_ICON =
      BitmapDescriptorFactory.fromResource(R.drawable.marker);

  private HistoryMapActivity mapActivity;
  private ArrayList<Fix> fixes = new ArrayList<>();
  private HashMap<String, Long> markerIdToUtc = new HashMap<>();
  private Cursor rows;
  private GoogleMap map;
  private Button datePicker;
  private DateRange dateRange;
  private boolean drawMarkers;

  private double maxLat = -90;
  private double minLat = 90;
  private double rightLng = 0;
  private double leftLng = 0;

  public FixVisualizer(
      Cursor rows,
      HistoryMapActivity mapActivity,
      GoogleMap map,
      Button datePicker,
      DateRange dateRange,
      boolean drawMarkers) {
    this.rows = rows;
    this.mapActivity = mapActivity;
    this.map = map;
    this.datePicker = datePicker;
    this.dateRange = dateRange;
    this.drawMarkers = drawMarkers;
  }

  public void draw() {
    map.setOnInfoWindowClickListener(new FixDeleter());

    if (!rows.moveToLast()) {
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 3));
      Toast.makeText(
          mapActivity,
          "no location during this period of time",
          Toast.LENGTH_SHORT).show();
      datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
          + "\n0 out of 0");
      return;
    }

    ArrayList<Double> lngList = new ArrayList<>();
    int size = rows.getCount();

    int index = 0;
    int freshness;
    double preLat = 0, preLng = 0;
    float[] results = new float[1];
    boolean firstPoint = true;

    while (true) {
      double lat = rows.getDouble(rows.getColumnIndex(Constants.COL_LAT));
      double lng = rows.getDouble(rows.getColumnIndex(Constants.COL_LNG));
      float acc = rows.getFloat(rows.getColumnIndex(Constants.COL_ACC));
      long utc = rows.getLong(rows.getColumnIndex(Constants.KEY_UTC));

      if (firstPoint) {
        firstPoint = false;
      } else {
        Location.distanceBetween(lat, lng, preLat, preLng, results);
        if (results[0] <= acc) {
          // Is a valid fix, but not drawing.
          index++;
          if (rows.isFirst()) {
            break;
          }
          rows.moveToPrevious();
          continue;
        }
      }
      preLat = lat;
      preLng = lng;

      if (lat > maxLat)
        maxLat = lat;
      if (lat < minLat)
        minLat = lat;

      freshness = (int) (255 * (double) index++ / size);
      fixes.add(new Fix(utc, lat, lng, acc, freshness));
      lngList.add(lng);

      if (rows.isFirst()) {
        break;
      }
      rows.moveToPrevious();
    }

    Double[] lngs = lngList.toArray(new Double[lngList.size()]);
    Arrays.sort(lngs);

    int idx = 0;
    double diff = lngs[lngs.length - 1] - lngs[0];
    if (diff > 180)
      diff = 360 - diff;
    for (int i = 1; i < lngs.length; i++) {
      double d = lngs[i] - lngs[i - 1];
      if (d > 180)
        d = 360 - d;
      if (diff < d) {
        diff = d;
        idx = i;
      }
    }
    leftLng = lngs[idx];
    if (idx == 0) {
      rightLng = lngs[lngs.length - 1];
    } else {
      rightLng = lngs[idx - 1];
    }

    rows.close();

    if (drawMarkers && fixes.size() > MAX_NUM_MARKERS) {
      drawMarkers = false;
      Toast.makeText(mapActivity, "Too many fixes (" + fixes.size() + ").\n"
          + "Markers will not be drawn.", Toast.LENGTH_SHORT).show();
    }
    map.clear();

    PolylineOptions lineOpt = new PolylineOptions()
        .width(4).color(Color.argb(128, 0, 0, 255));
    for (Fix fix : fixes) {
      lineOpt.add(fix.latLng);
      CircleOptions circleOpt = new CircleOptions()
          .center(fix.latLng)
          .radius(fix.acc)
          .strokeWidth(2)
          .strokeColor(Color.argb(128, 255 - fix.freshness, fix.freshness, 0));
      map.addCircle(circleOpt);

      if (drawMarkers) {
        Marker marker = map.addMarker(new MarkerOptions()
            .position(fix.latLng)
            .title(ToStringHelper.utcToString(fix.utc))
            .snippet(ToStringHelper.latLngAccToString(
                fix.latLng.latitude, fix.latLng.longitude, fix.acc))
            .icon(MARKER_ICON));
        markerIdToUtc.put(marker.getId(), fix.utc);
      }
    }
    map.addPolyline(lineOpt);

    map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
        new LatLng(minLat, leftLng), new LatLng(maxLat, rightLng)), 50));

    datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
        + "\n" + fixes.size() + " out of " + rows.getCount());
  }

  class FixDeleter implements GoogleMap.OnInfoWindowClickListener {
    class RealFixDeleter implements DialogInterface.OnClickListener {
      private long utc;
      private boolean deleteAllDay;

      RealFixDeleter(long utc, boolean deleteAllDay) {
        this.utc = utc;
        this.deleteAllDay = deleteAllDay;
      }
      @Override
      public void onClick(DialogInterface dialog, int id) {
        GeoFixDataStore geoFixDataStore =
            new GeoFixDataStore(FixVisualizer.this.mapActivity);
        geoFixDataStore.open();
        if (deleteAllDay) {
          geoFixDataStore.deleteOneDay(utc);
        } else {
          geoFixDataStore.deleteGeoFix(utc);
        }
        geoFixDataStore.close();
        FixVisualizer.this.mapActivity.updateDrawing(0, true);
        Toast.makeText(
            FixVisualizer.this.mapActivity,
            "deleted!",
            Toast.LENGTH_SHORT).show();
      }
    }

    class DialogueCloser implements DialogInterface.OnClickListener {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
      final long utc = markerIdToUtc.get(marker.getId());
      new AlertDialog.Builder(FixVisualizer.this.mapActivity)
          .setTitle(marker.getTitle())
          .setMessage(marker.getSnippet() + "\n\nPress BACK to cancel.")
          .setPositiveButton(
              "Delete this fix",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  new AlertDialog.Builder(FixVisualizer.this.mapActivity)
                      .setTitle("Confirm").setMessage("DELETE?")
                      .setPositiveButton("Yes", new RealFixDeleter(utc, false))
                      .setNegativeButton("No", new DialogueCloser()).show();
                }
              })
          .setNegativeButton(
              "DELETE THIS DAY",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  new AlertDialog.Builder(FixVisualizer.this.mapActivity)
                      .setTitle("Confirm").setMessage("DELETE?")
                      .setPositiveButton("Yes", new RealFixDeleter(utc, true))
                      .setNegativeButton("No", new DialogueCloser()).show();
                }
              }).show();
    }
  }
}
