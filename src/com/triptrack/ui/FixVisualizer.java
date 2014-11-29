package com.triptrack.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;
import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.CalendarUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class FixVisualizer {
  private static final String TAG = "FixVisualizer";
  private static final int MAX_ZOOM_LEVEL = 18;

  private ClusterManager<Fix> clusterManager;
  private HistoryMapActivity mapActivity;
  private ArrayList<Fix> fixes = new ArrayList<Fix>();
  private HashMap<String, Long> markerIdToUtc = new HashMap<>();
  private GoogleMap map;
  private Button datePicker;
  private DateRange dateRange;

  public FixVisualizer(
      HistoryMapActivity mapActivity,
      GoogleMap map,
      Button datePicker) {
    this.mapActivity = mapActivity;
    this.map = map;
    this.datePicker = datePicker;
//    this.clusterManager = new ClusterManager<>(mapActivity, map, new UserNotifier());
    map.setOnCameraChangeListener(clusterManager);
  }

  public void draw(GeoFixDataStore dataStore, DateRange dateRange, boolean drawMarkers) {
    this.dateRange = dateRange;

    class MyTask extends AsyncTask {
      @Override
      protected Object doInBackground(Object[] params) {
        return null;
      }
    }

    Cursor rows = dataStore.getGeoFixesByDateRange(dateRange);
    fixes.clear();

    //map.setOnInfoWindowClickListener(new FixDeleter());

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

    // ArrayList<Double> lngList = new ArrayList<Double>();
    // int size = rows.getCount();
    // int index = 0;
    // int freshness;
//    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
//
//    while (true) {
//      double lat = Cursors.getLat(rows);
//      double lng = Cursors.getLng(rows);
//      float acc = Cursors.getAcc(rows);
//      long utc = Cursors.getUtc(rows);
//
//      boundsBuilder.include(new LatLng(lat, lng));
//
//      // freshness = (int) (255 * (double) index++ / size);
//      // lngList.add(lng);
//      fixes.add(new Fix(utc, lat, lng, acc, 50));
//
//      if (rows.isFirst()) {
//        rows.close();
//        break;
//      }
//      rows.moveToPrevious();
//    }
//    map.clear();

//    clusterManager.clearItems();
//    clusterManager.addItems(fixes);

//    Display display = mapActivity.getWindowManager().getDefaultDisplay();
//    Point displaySize = new Point();
//    display.getSize(displaySize);
//    map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), displaySize.x, displaySize.y, 100));
//    if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
//      map.moveCamera(CameraUpdateFactory.zoomTo(MAX_ZOOM_LEVEL));
//    }
//    clusterManager.cluster();

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
        Context context = FixVisualizer.this.mapActivity;

        GeoFixDataStore geoFixDataStore =
            new GeoFixDataStore(context);
        geoFixDataStore.open();
        if (deleteAllDay) {
          geoFixDataStore.deleteOneDay(utc);
        } else {
          geoFixDataStore.deleteGeoFix(utc);
        }
        geoFixDataStore.close();

        draw(geoFixDataStore, dateRange, true);
        Toast.makeText(
            context,
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
      final Context context = FixVisualizer.this.mapActivity;
      new AlertDialog.Builder(context)
          .setTitle(marker.getTitle())
          .setMessage(marker.getSnippet() + "\n\nPress BACK to cancel.")
          .setPositiveButton(
              "Delete this fix",
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  new AlertDialog.Builder(context)
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
                  new AlertDialog.Builder(context)
                      .setTitle("Confirm").setMessage("DELETE?")
                      .setPositiveButton("Yes", new RealFixDeleter(utc, true))
                      .setNegativeButton("No", new DialogueCloser()).show();
                }
              }).show();
    }
  }
}
