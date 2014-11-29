package com.triptrack.ui;

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
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.triptrack.LocationRecordingSwitch;
import com.triptrack.datastore.GeoFixDataStore;
import com.triptrack.util.Constants;
import com.triptrack.util.Cursors;
import com.triptrack_beta.R;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;


public final class ControlPanelActivity extends Activity {
  private static final String TAG = "ControlPanelActivity";

  private final Handler uiHandler = new Handler() {
    public void handleMessage(Message message) {
      switch (message.what) {
        case Constants.HANDLER_TOAST: {
          Toast.makeText(
              ControlPanelActivity.this,
              (String) message.obj,
              Toast.LENGTH_LONG).show();
          break;
        }
        case Constants.HANDLER_PROGRESSBAR_DISMISS: {
          progressBar.dismiss();
          break;
        }
        case Constants.HANDLER_PROGRESSBAR_SHOWMAX: {
          progressBar.setMax(message.arg1);
          progressBar.show();
          break;
        }
        case Constants.HANDLER_PROGRESSBAR_SETPROGRESS: {
          progressBar.setProgress(message.arg1);
          break;
        }
      }
    }
  };

  private ProgressDialog progressBar;
  private SharedPreferences settings;
  private SharedPreferences.Editor editor;

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

    settings = getSharedPreferences(Constants.PREFS_FILE, MODE_PRIVATE);
    editor = settings.edit();
    progressBar = new ProgressDialog(this);
    progressBar.setCancelable(false);
    progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

    ((ToggleButton) findViewById(R.id.recording_switch))
        .setChecked(settings.getBoolean(Constants.ENABLED, false));
  }

  /**
   * Executed when clicking on the recording switch toggle button.
   */
  public void toggleRecordingState(View v) {
    if (((ToggleButton) v).isChecked()) {
      // Write the enabling status to SharedPreferences.
      while (!editor.putBoolean(Constants.ENABLED, true).commit()) ;

      // Read the intervals from SharedPreferences.
      int intervalMins = settings.getInt(Constants.INTERVAL_MINS, Constants.DEFAULT_INTERVAL_MINS);

      // Start the alarm.
      LocationRecordingSwitch.startRecording(this, intervalMins);
    } else {
      // Write the enabling status to SharedPreferences.
      while (!editor.putBoolean(Constants.ENABLED, false).commit());

      // Stop the alarm.
      LocationRecordingSwitch.stopRecording(this);
    }
  }

  /**
   * Executed when clicking on the set interval button.
   */
  public void setInterval(View v) {
    final int intervalMins = settings.getInt(
        Constants.INTERVAL_MINS, Constants.DEFAULT_INTERVAL_MINS);
    final EditText setIntervalEditor = new EditText(this);
    setIntervalEditor.setInputType(InputType.TYPE_CLASS_NUMBER);
    setIntervalEditor.setText(Integer.toString(intervalMins));

    DialogInterface.OnClickListener onOkListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int whichButton) {
        int newIntervalMins;
        // Make sure the input number is valid and greater than 0.
        try {
          newIntervalMins = Integer.parseInt(setIntervalEditor.getText().toString());
        } catch (NumberFormatException e) {
          newIntervalMins = 0;
        }
        if (newIntervalMins < 1) {
          Toast.makeText(
              ControlPanelActivity.this,
              getString(R.string.set_interval_invalid),
              Toast.LENGTH_SHORT)
                  .show();
          return;
        }

        // If interval is not changed, return.
        if (newIntervalMins == intervalMins) {
          Toast.makeText(
              ControlPanelActivity.this,
              getString(R.string.set_interval_not_changed)
                  + " " + Integer.toString(intervalMins) + " mins.",
              Toast.LENGTH_SHORT)
                  .show();
          return;
        }

        // Write the new interval to SharedPreferences.
        while (!editor.putInt(Constants.INTERVAL_MINS, newIntervalMins).commit());
        Toast.makeText(
            ControlPanelActivity.this,
            getString(R.string.set_interval_changed)
                + " " + Integer.toString(newIntervalMins) + " mins.",
            Toast.LENGTH_SHORT)
                .show();

        // If the alarm is activated, restart it with the new interval.
        if (settings.getBoolean(Constants.ENABLED, false)) {
          LocationRecordingSwitch.stopRecording(ControlPanelActivity.this);
          LocationRecordingSwitch.startRecording(ControlPanelActivity.this, newIntervalMins);
        }
      }
    };

    // Display a dialog containing the editor.
    new AlertDialog.Builder(this)
        .setTitle(getString(R.string.set_interval))
        .setMessage(getString(R.string.set_interval_direction))
        .setView(setIntervalEditor)
        .setPositiveButton("Ok", onOkListener)
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            dialog.cancel();
          }
        })
        .show();
  }

  public void importFromFile(View v) {
    // External file to read.
    File sdCard = Environment.getExternalStorageDirectory();
    final File file = new File(sdCard, Constants.HISTORY_FILE + ".dat");

    // Confirmation dialog.
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.import_confirmation) + "\n" + file)
        .setCancelable(true)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            progressBar.setMessage(getString(R.string.import_in_progress));

            // Create a thread for importing, which may take a long time.
            new Thread(new Runnable() {
              public void run() {
                GeoFixDataStore geoFixDataStore = new GeoFixDataStore(ControlPanelActivity.this);
                geoFixDataStore.open();
                geoFixDataStore.importFromFile(file, uiHandler);
                geoFixDataStore.close();

                // Dismiss the progress bar on the UI thread.
                uiHandler.sendMessage(Message.obtain(
                    uiHandler, Constants.HANDLER_PROGRESSBAR_DISMISS, 0, 0, null));
              }
            }).start();
          }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            Toast.makeText(
                ControlPanelActivity.this,
                getString(R.string.import_canceled),
                Toast.LENGTH_SHORT)
                    .show();
            dialog.cancel();
          }
        })
        .show();
  }

  public void exportToFile(View v) {
    String history = "";
    GeoFixDataStore geoFixDataStore = new GeoFixDataStore(this);
    geoFixDataStore.open();
    Cursor c = geoFixDataStore.getRecentGeoFixes(100);

    if (!c.isAfterLast()) {
      StringBuilder res = new StringBuilder("most recent 100 fixes\n");
      c.moveToFirst();

      while (true) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(Cursors.getUtc(c));

        res.append(String.format("%04d", calendar.get(Calendar.YEAR)))
            .append("/").append(String.format("%02d", (calendar.get(Calendar.MONTH) + 1)))
            .append("/").append(String.format("%02d", calendar.get(Calendar.DATE)))
            .append(", ").append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)))
            .append(":").append(String.format("%02d", calendar.get(Calendar.MINUTE)))
            .append(":").append(String.format("%02d", calendar.get(Calendar.SECOND)))
            .append(", ").append(Cursors.getAcc(c)).append("M\n");

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

    // File to write to.
    File sdCard = Environment.getExternalStorageDirectory();
    final File file = new File(sdCard, Constants.HISTORY_FILE + "-" + today + ".dat");

    // Confirmation dialog.
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.export_confirmation) + "\n" + file + "\n\n" + history)
        .setCancelable(true)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            progressBar.setMessage(getString(R.string.export_in_progress));
            new Thread(new Runnable() {
              public void run() {
                GeoFixDataStore geoFixDataStore = new GeoFixDataStore(ControlPanelActivity.this);
                geoFixDataStore.open();
                geoFixDataStore.exportToFile(file, uiHandler);
                geoFixDataStore.close();

                // Dismiss the progress bar on the UI thread.
                uiHandler.sendMessage(Message.obtain(
                    uiHandler, Constants.HANDLER_PROGRESSBAR_DISMISS, 0, 0, null));
              }
            }).start();
          }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            Toast.makeText(
                ControlPanelActivity.this,
                getString(R.string.export_canceled),
                Toast.LENGTH_SHORT).show();
            dialog.cancel();
          }
        })
        .show();
  }

  public void clearHistory(View v) {
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.clear_confirmation))
        .setCancelable(true)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            GeoFixDataStore geoFixDataStore = new GeoFixDataStore(ControlPanelActivity.this);
            // Clear the history.
            geoFixDataStore.open();
            geoFixDataStore.clearHistory();
            geoFixDataStore.close();
            Toast.makeText(
                ControlPanelActivity.this,
                getString(R.string.clear_finished),
                Toast.LENGTH_SHORT)
                    .show();
          }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            Toast.makeText(
                ControlPanelActivity.this,
                getString(R.string.clear_canceled),
                Toast.LENGTH_SHORT)
                    .show();
            dialog.cancel();
          }
        })
        .show();
  }

  public void showInfo(View v) {
    new AlertDialog.Builder(this)
        .setMessage(Constants.ABOUT)
        .setCancelable(true)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        })
        .show();
  }
}
