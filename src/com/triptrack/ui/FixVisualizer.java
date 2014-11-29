package com.triptrack.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.triptrack.datastore.GeoFixDataStore;

import java.util.HashMap;

public class FixVisualizer {
  private HistoryMapActivity mapActivity;
  private HashMap<String, Long> markerIdToUtc = new HashMap<>();

  public FixVisualizer(HistoryMapActivity mapActivity) {
    this.mapActivity = mapActivity;
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

        //draw(geoFixDataStore, dateRange, true);
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
