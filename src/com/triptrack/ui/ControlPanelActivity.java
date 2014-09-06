package com.triptrack.ui;

import com.triptrack.LocationRecordingSwitch;
import com.triptrack.R;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;


public final class ControlPanelActivity extends Activity {
  private static final String TAG = "ControlPanelActivity";

  /**
   * When orientation changes, re-draw the activity without deleting dialogs
   * currently is showing. (If destroy() is called automatically, dialogs will
   * disappear and become inaccessible).
   */
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Manually re-draw the activity.
    onCreate(null);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.control_panel_activity);

    // SharedPreferences reader and writer.
    final SharedPreferences settings =
        getSharedPreferences(Constants.PREFS_FILE, MODE_PRIVATE);
    final SharedPreferences.Editor editor = settings.edit();

    // ToggleButton for enabling/disabling the app.
    final ToggleButton toggleButton =
        (ToggleButton) findViewById(R.id.recording_switch);
    toggleButton.setChecked(settings.getBoolean(Constants.ENABLED, false));
    toggleButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (((ToggleButton) v).isChecked()) {
          // Write the enabling status to SharedPreferences.
          while (!editor.putBoolean(Constants.ENABLED, true).commit()) ;
          // Read the intervals from SharedPreferences.
          final int intervalMins = settings.getInt(Constants.INTERVAL_MINS,
              Constants.DEFAULT_INTERVAL_MINS);
          // Start the alarm.
          LocationRecordingSwitch.startRecording(
              ControlPanelActivity.this, intervalMins);
        } else {
          // Write the enabling status to SharedPreferences.
          while (!editor.putBoolean(Constants.ENABLED, false).commit()) ;
          // Stop the alarm.
          LocationRecordingSwitch.stopRecording(ControlPanelActivity.this);
        }
      }
    });

    // Button for setting sampling interviews in minutes.
    final Button setIntervalButton = (Button) findViewById(R.id.set_interval);
    setIntervalButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Read the intervals from SharedPreferences.
        final int intervalMins = settings.getInt(Constants.INTERVAL_MINS,
            Constants.DEFAULT_INTERVAL_MINS);

        // Set an EditText view for setting the interval.
        final EditText setIntervalET = new EditText(ControlPanelActivity.this);
        setIntervalET.setInputType(InputType.TYPE_CLASS_NUMBER);
        setIntervalET.setText(Integer.toString(intervalMins));

        // Display a dialog containing the EditText view.
        new AlertDialog.Builder(ControlPanelActivity.this)
            .setTitle(
                ControlPanelActivity.this.getString(R.string.set_interval))
            .setMessage(ControlPanelActivity.this.getString(
                R.string.set_interval_direction))
            .setView(setIntervalET)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int whichButton) {
                final int newIntervalMins;
                final Editable interval = setIntervalET.getText();
                // Make sure the input number >= 1.
                try {
                  newIntervalMins = Integer.parseInt(interval.toString());
                  if (newIntervalMins < 1) {
                    throw new NumberFormatException();
                  }
                } catch (NumberFormatException e) {
                  Toast.makeText(ControlPanelActivity.this,
                      ControlPanelActivity.this.getString(
                          R.string.set_interval_invalid),
                      Toast.LENGTH_SHORT).show();
                  return;
                }
                // If interval is not changed, return.
                if (newIntervalMins == intervalMins) {
                  Toast.makeText(ControlPanelActivity.this,
                      ControlPanelActivity.this.getString(
                          R.string.set_interval_not_changed)
                          + " " + Integer.toString(intervalMins) + " mins.",
                      Toast.LENGTH_SHORT).show();
                  return;
                }
                // Write the new interval to SharedPreferences.
                while (!editor.putInt(Constants.INTERVAL_MINS, newIntervalMins)
                    .commit());
                Toast.makeText(ControlPanelActivity.this,
                    ControlPanelActivity.this.getString(
                        R.string.set_interval_changed)
                        + " " + Integer.toString(newIntervalMins) + " mins.",
                    Toast.LENGTH_SHORT).show();
                // If the alarm is activated, restart it with the new interval.
                if (settings.getBoolean(Constants.ENABLED, false)) {
                  LocationRecordingSwitch.stopRecording(
                      ControlPanelActivity.this);
                  LocationRecordingSwitch.startRecording(
                      ControlPanelActivity.this, newIntervalMins);
                }
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
              }
            })
            .show();
      }
    });

    // ProgressDialog used in export/import.
    final ProgressDialog progressBar =
        new ProgressDialog(ControlPanelActivity.this);
    progressBar.setCancelable(false);
    progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    // Handler for showing toasts and the progress bar in the UI thread.
    final Handler uiHandler = new Handler() {
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case Constants.HANDLER_TOAST: {
            Toast.makeText(
                ControlPanelActivity.this,
                (String) msg.obj,
                Toast.LENGTH_LONG).show();
            break;
          }
          case Constants.HANDLER_PROGRESSBAR_DISMISS: {
            progressBar.dismiss();
            break;
          }
          case Constants.HANDLER_PROGRESSBAR_SHOWMAX: {
            progressBar.setMax(msg.arg1);
            //TODO: ensure setProgress happens before show().
            progressBar.setProgress(0);
            progressBar.show();
            break;
          }
          case Constants.HANDLER_PROGRESSBAR_SETPROGRESS: {
            progressBar.setProgress(msg.arg1);
            break;
          }
        }
      }
    };

    // Button for importing history.
    final Button importButton = (Button) findViewById(R.id.import_history);
    importButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // External file to read.
        File sdCard = Environment.getExternalStorageDirectory();
        final File file = new File(sdCard, Constants.HISTORY_FILE + ".csv");
        // Confirmation dialog.
        new AlertDialog.Builder(ControlPanelActivity.this)
            .setMessage(ControlPanelActivity.this.getString(R.string.import_confirmation) + "\n" + file)
            .setCancelable(true)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                progressBar.setMessage(ControlPanelActivity.this.getString(R.string.import_in_progress));
                // Create a thread for importing, which may take a long time.
                new Thread(new Runnable() {
                  public void run() {
                    final GeoFixDataStore geoFixDataStore = new GeoFixDataStore(ControlPanelActivity.this);
                    geoFixDataStore.open();
                    geoFixDataStore.importFromFile(file, uiHandler);
                    geoFixDataStore.close();
                    // Dismiss the progress bar on the UI thread.
                    uiHandler.sendMessage(Message.obtain(uiHandler,
                        Constants.HANDLER_PROGRESSBAR_DISMISS, 0, 0, null));
                  }
                }).start();
              }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(ControlPanelActivity.this,
                    ControlPanelActivity.this.getString(R.string.import_canceled),
                    Toast.LENGTH_SHORT).show();
                dialog.cancel();
              }
            }).show();
      }
    });

    // Button for exporting history.
    final Button exportButton = (Button) findViewById(R.id.export_history);
    exportButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        GeoFixDataStore geoFixDataStore =
            new GeoFixDataStore(ControlPanelActivity.this);
        geoFixDataStore.open();
        Cursor c = geoFixDataStore.getRecentGeoFixes(100);
        String history = "";
        if (!c.isAfterLast()) {
          StringBuilder res = new StringBuilder("most recent 100 fixes\n");
          c.moveToFirst();

          while (true) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(c.getLong((c.getColumnIndex(Constants.KEY_UTC))));

            res.append(String.format("%04d", calendar.get(Calendar.YEAR)))
                .append("/").append(String.format("%02d", (calendar.get(Calendar.MONTH) + 1)))
                .append("/").append(String.format("%02d", calendar.get(Calendar.DATE)))
                .append(", ").append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)))
                .append(":").append(String.format("%02d", calendar.get(Calendar.MINUTE)))
                .append(":").append(String.format("%02d", calendar.get(Calendar.SECOND)))
                .append(", ").append(c.getFloat(c.getColumnIndex(Constants.COL_ACC))).append("M\n");

            if (c.isLast()) {
              c.close();
              break;
            }
            c.moveToNext();
          }
          history = res.toString();
        }
        geoFixDataStore.close();

        Calendar calendar = new GregorianCalendar();
        String today = String.format("%04d", calendar.get(Calendar.YEAR)) + "-"
            + String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "-"
            + String.format("%02d", calendar.get(Calendar.DATE));

        // File to write.
        File sdCard = Environment.getExternalStorageDirectory();
        final File file = new File(
            sdCard, Constants.HISTORY_FILE + "-" + today + ".csv");
        // Confirmation dialog.
        new AlertDialog.Builder(ControlPanelActivity.this)
            .setMessage(ControlPanelActivity.this.getString(
                R.string.export_confirmation) + "\n" + file + "\n\n" + history)
            .setCancelable(true)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                progressBar.setMessage(ControlPanelActivity.this.getString(
                    R.string.export_in_progress));
                new Thread(new Runnable() {
                  public void run() {
                    final GeoFixDataStore geoFixDataStore =
                        new GeoFixDataStore(ControlPanelActivity.this);
                    geoFixDataStore.open();
                    geoFixDataStore.exportToFile(file, uiHandler);
                    geoFixDataStore.close();
                    // Dismiss the progress bar on the UI thread.
                    uiHandler.sendMessage(Message.obtain(uiHandler,
                        Constants.HANDLER_PROGRESSBAR_DISMISS, 0, 0, null));
                  }
                }).start();
              }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(
                    ControlPanelActivity.this,
                    ControlPanelActivity.this.getString(
                        R.string.export_canceled),
                    Toast.LENGTH_SHORT).show();
                dialog.cancel();
              }
            }).show();
      }
    });

    // Button for clearing history.
    final Button clearButton = (Button) findViewById(R.id.clear_history);
    clearButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Confirmation dialog.
        new AlertDialog.Builder(ControlPanelActivity.this)
            .setMessage(ControlPanelActivity.this.getString(R.string.clear_confirmation))
            .setCancelable(true)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                final GeoFixDataStore geoFixDataStore =
                    new GeoFixDataStore(ControlPanelActivity.this);
                // Clear the history.
                geoFixDataStore.open();
                geoFixDataStore.clearHistory();
                geoFixDataStore.close();
                Toast.makeText(ControlPanelActivity.this,
                    ControlPanelActivity.this.getString(R.string.clear_finished),
                    Toast.LENGTH_SHORT).show();
              }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(ControlPanelActivity.this,
                    ControlPanelActivity.this.getString(R.string.clear_canceled),
                    Toast.LENGTH_SHORT).show();
                dialog.cancel();
              }
            }).show();
      }
    });

    // Button for showing history on MapActivity.
    final Button showMapButton = (Button) findViewById(R.id.show_history);
    showMapButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ControlPanelActivity.this.finish();
      }
    });

    // Button for showing About information.
    final Button aboutButton = (Button) findViewById(R.id.about);
    aboutButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        new AlertDialog.Builder(ControlPanelActivity.this)
            .setMessage(Constants.ABOUT).setCancelable(true)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            }).show();
      }
    });
  }
}
