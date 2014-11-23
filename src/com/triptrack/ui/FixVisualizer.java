package com.triptrack.ui;

import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack_beta.R;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;
import com.triptrack.util.Cursors;
import com.triptrack.util.ToStringHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
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
import java.util.HashMap;

public class FixVisualizer {
  private static final String TAG = "FixVisualizer";
  private static final int MAX_NUM_MARKERS = 500;
  private static final BitmapDescriptor MARKER_ICON =
      BitmapDescriptorFactory.fromResource(R.drawable.marker);

  private HistoryMapActivity mapActivity;
  private ArrayList<Fix> fixes = new ArrayList<Fix>();
  private HashMap<String, Long> markerIdToUtc = new HashMap<String, Long>();
  private Cursor rows;
  private GoogleMap map;
  private Button datePicker;
  private DateRange dateRange;
  private boolean drawMarkers;

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

    if (!rows.moveToLast()) { // move to the oldest fix.
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 3));
      Toast.makeText(
          mapActivity,
          "no location during this period of time",
          Toast.LENGTH_SHORT).show();
      datePicker.setText(CalendarUtils.dateRangeToString(dateRange)
          + "\n0 out of 0");
      return;
    }

    ArrayList<Double> lngList = new ArrayList<Double>();
    int size = rows.getCount();

    int index = 0;
    int freshness;
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    while (true) {
      double lat = Cursors.getLat(rows);
      double lng = Cursors.getLng(rows);
      float acc = Cursors.getAcc(rows);
      long utc = Cursors.getUtc(rows);

      boundsBuilder.include(new LatLng(lat, lng));

      freshness = (int) (255 * (double) index++ / size);
      fixes.add(new Fix(utc, lat, lng, acc, freshness));
      lngList.add(lng);

      if (rows.isFirst()) {
        rows.close();
        break;
      }
      rows.moveToPrevious();
    }
    map.clear();
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50));

    if (drawMarkers && fixes.size() > MAX_NUM_MARKERS) {
      drawMarkers = false;
      Toast.makeText(mapActivity, "Too many fixes (" + fixes.size() + ").\n"
          + "Markers will not be drawn.", Toast.LENGTH_SHORT).show();
    }

    PolylineOptions lineOpt = new PolylineOptions()
        .width(4).color(Color.argb(128, 0, 0, 255));
    for (Fix fix : fixes) {
      lineOpt.add(fix.getLatLng());
      CircleOptions circleOpt = new CircleOptions()
          .center(fix.getLatLng())
          .radius(fix.getAcc())
          .strokeWidth(2);
         // .strokeColor(Color.argb(128, 255 - fix.getFreshness(), fix.getFreshness(), 0));
      map.addCircle(circleOpt);

      if (drawMarkers) {
        Marker marker = map.addMarker(new MarkerOptions()
            .position(fix.getLatLng())
            .title(ToStringHelper.utcToString(fix.getUtc()))
            .snippet(ToStringHelper.latLngAccToString(
                fix.getLat(), fix.getLng(), fix.getAcc()))
            .icon(MARKER_ICON));
        markerIdToUtc.put(marker.getId(), fix.getUtc());
      }
    }
    map.addPolyline(lineOpt);

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
        FixVisualizer.this.mapActivity.updateDrawing(true);
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
