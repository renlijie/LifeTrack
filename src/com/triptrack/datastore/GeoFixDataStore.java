package com.triptrack.datastore;

import com.triptrack.DateRange;
import com.triptrack.R;
import com.triptrack.util.CalendarUtils;
import com.triptrack.util.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class GeoFixDataStore {
  private static final String TAG = "GeoFixDataStore";
  private static final String DATABASE_NAME = "triptrack";
  private static final String DATABASE_TABLE = "geofix";
  private static final int DATABASE_VERSION = 1;
  private static final String[] DATABASE_COLUMNS = new String[] {
      Constants.KEY_UTC,
      Constants.COL_LAT,
      Constants.COL_LNG,
      Constants.COL_ACC
  };
  private static final String CREATE_DATABASE =
      "create table " + DATABASE_TABLE + " ("
          + Constants.KEY_UTC + " long primary key, "
          + Constants.COL_LAT + " double, "
          + Constants.COL_LNG + " double, "
          + Constants.COL_ACC + " single);";

  private final Context context;

  private DatabaseHelper databaseHelper;
  private SQLiteDatabase database;

  public GeoFixDataStore(Context context) {
    this.context = context;
  }

  public GeoFixDataStore open() {
    databaseHelper = new DatabaseHelper(context);
    int waitInMillis = 100;
    while (true) {
      try {
        database = databaseHelper.getWritableDatabase();
        break;
      } catch (SQLException e) {
        Log.w(Constants.TAG + ":" + TAG,
            "Database is not available. Will try again in "
                + Integer.toString(waitInMillis) + " ms.");
      }
      try {
        // Wait in case the database is not immediately available.
        Thread.sleep(waitInMillis);
        if (waitInMillis < 5000) {
          waitInMillis *= 2;
        }
      } catch (InterruptedException e) {
        Log.w(Constants.TAG + ":" + TAG, e.toString());
      }
    }
    return this;
  }

  public void close() {
    databaseHelper.close();
  }

  public void exportToFile(File file, Handler uiHandler) {
    // get all fixes
    Cursor cursor = getRecentGeoFixes(0);

    if (cursor.moveToFirst()) {
      FileOutputStream fos;
      try {
        fos = new FileOutputStream(file);
        // write total number of fixes
        fos.write((cursor.getCount() + "\n").getBytes());
      } catch (IOException e) {
        uiHandler.sendMessage(Message.obtain(
            uiHandler, Constants.HANDLER_TOAST, 0, 0,
            "Opening " + file + " failed. Do you have access?"));
        Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
        return;
      }

      int numGeoFixesExported = 0;
      uiHandler.sendMessage(Message.obtain(uiHandler,
          Constants.HANDLER_PROGRESSBAR_SHOWMAX, cursor.getCount(), 0, null));

      // write all fixes
      while (true) {
        String geoFix = getStringFromEntry(cursor);
        try {
          fos.write(geoFix.getBytes());
        } catch (IOException e) {
          uiHandler.sendMessage(Message.obtain(
              uiHandler, Constants.HANDLER_TOAST, 0, 0,
              "Writing to " + file + " failed."));
          Log.w(Constants.TAG + ":" + TAG, "Writing " + geoFix + " failed.");
        }

        numGeoFixesExported++;
        if (numGeoFixesExported % 1000 == 0) {
          uiHandler.sendMessage(Message.obtain(
              uiHandler, Constants.HANDLER_PROGRESSBAR_SETPROGRESS,
              numGeoFixesExported, 0, null));
        }

        if (cursor.isLast()) {
          break;
        }
        cursor.moveToNext();
      }

      // close file
      try {
        fos.close();
      } catch (IOException e) {
        uiHandler.sendMessage(Message.obtain(uiHandler,
            Constants.HANDLER_TOAST, 0, 0, "Closing " + file + " failed."));
        Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.");
      }

      uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
          0, 0, context.getString(R.string.export_finished)));
      Log.d(Constants.TAG + ":" + TAG,
          context.getString(R.string.export_finished));
    } else {
      uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
          0, 0, context.getString(R.string.export_nothing)));
    }
    cursor.close();
  }

  public void importFromFile(File file, Handler uiHandler) {
    // Open a reader.
    FileInputStream fis;
    try {
      fis = new FileInputStream(file);
    } catch (IOException e) {
      uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
          0, 0, "Opening " + file + " failed. Does it exist?"));
      Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
      return;
    }
    BufferedReader br = new BufferedReader(
        new InputStreamReader(new DataInputStream(fis)));

    // Read the first line containing the total number of fixes.
    String line;
    int numFixes;
    try {
      line = br.readLine();
    } catch (IOException e) {
      uiHandler.sendMessage(Message.obtain(uiHandler,
          Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
      Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.");
      return;
    }
    try {
      numFixes = Integer.parseInt(line);
    } catch (NumberFormatException e) {
      // If the first line cannot be parsed as an int, create a new reader
      // to read from the beginning of the file, in case the first line is
      // actually a fix.
      try {
        br.close();
        fis = new FileInputStream(file);
      } catch (IOException ex) {
        uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
            0, 0, "Opening " + file + " failed. Does it exist?"));
        Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
        return;
      }
      br = new BufferedReader(new InputStreamReader(new DataInputStream(fis)));

      // Assign a fake size.
      numFixes = Integer.MAX_VALUE;

      // Notify the user and set the flag. Continue.
      uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
          0, 0, "Reading size failed. But continue."));
      Log.w(Constants.TAG + ":" + TAG, "Reading size failed." + line);
    }

    // Show the progress bar.
    uiHandler.sendMessage(Message.obtain(uiHandler,
        Constants.HANDLER_PROGRESSBAR_SHOWMAX, numFixes, 0, null));

    long utc;
    double lat, lng;
    float acc;

    // The index of the fix currently being imported.
    int numImportedFixes = 0;
    // The number of fixes written successfully.
    int numGoodFixes = 0;
    // The number of illegal fixes.
    int numBadFixes = 0;
    // The number of fixes written unsuccessfully.
    int numDuplicatedFixes = 0;

    // Use transaction to significantly improve efficiency.
    database.beginTransaction();

    while (true) {
      numImportedFixes++;
      if (numImportedFixes % 1000 == 0) {
        uiHandler.sendMessage(Message.obtain(
            uiHandler, Constants.HANDLER_PROGRESSBAR_SETPROGRESS,
            numImportedFixes, 0, null));
      }
      // Read the current line. Should contain a fix.
      try {
        line = br.readLine();
      } catch (IOException e) {
        uiHandler.sendMessage(Message.obtain(uiHandler,
            Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
        Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.");
        return;
      }
      if (line == null) {
        break;
      }
      // Parse the line as a fix.
      String[] entry = line.split(",");
      if (entry.length < 4) {
        numBadFixes++;
        Log.e(Constants.TAG + ":" + TAG, "Malformed fix: #" + numImportedFixes);
        continue;
      }
      // Validate the data.
      try {
        utc = Long.parseLong(entry[0]);
        lat = Double.parseDouble(entry[1]);
        lng = Double.parseDouble(entry[2]);
        acc = Float.parseFloat(entry[3]);
      } catch (NumberFormatException e) {
        numBadFixes++;
        Log.w(Constants.TAG + ":" + TAG,
            "Non-parseable fix: #" + numImportedFixes);
        continue;
      }
      if (utc < 0 || lat < -90 || lat > 90 || lng > 180 || lng < -180 ||
          acc < 0) {
        numBadFixes++;
        Log.w(Constants.TAG + ":" + TAG,
            "Out of bound fix: #" + numImportedFixes);
        continue;
      }

      // Insert the fix to DB. If it is already in there, an SQLException would
      // be caught.
      try {
        insertGeoFixOrThrow(utc, lat, lng, acc);
        numGoodFixes++;
      } catch (SQLException e) {
        numDuplicatedFixes++;
        Log.w(Constants.TAG + ":" + TAG, "Duplicate fix: #" + numImportedFixes);
      }
    }

    database.setTransactionSuccessful();
    database.endTransaction();

    // Notify the user about the stats.
    String stats = "Done. Read "
        + Integer.toString(numGoodFixes + numDuplicatedFixes + numBadFixes)
        + " geo-fixes.\n" + Integer.toString(numGoodFixes)
        + " are written successfully.\n" + Integer.toString(numDuplicatedFixes)
        + " are already in the database.\n" + Integer.toString(numBadFixes)
        + " are not valid.";
    uiHandler.sendMessage(Message.obtain(
        uiHandler, Constants.HANDLER_TOAST, 0, 0, stats));
    Log.d(Constants.TAG + ":" + TAG, stats);

    // Close the reader.
    try {
      br.close();
    } catch (IOException e) {
      uiHandler.sendMessage(Message.obtain(uiHandler,
          Constants.HANDLER_TOAST, 0, 0, "Closing " + file + " failed."));
      Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.");
    }
  }

  public void clearHistory() {
    database.execSQL("DELETE FROM " + DATABASE_TABLE);
  }

  public void deleteGeoFix(long utc) {
    database.delete(DATABASE_TABLE,
        Constants.KEY_UTC + "=" + Long.toString(utc), null);
  }

  public void deleteOneDay(long utc) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(utc);

    long fromUtc = CalendarUtils.toBeginningOfDay(cal).getTimeInMillis();
    long toUtc = fromUtc + 24L * 3600 * 1000;
    deleteByRange(fromUtc, toUtc);
  }

  /**
   * Write the fix into datastore.
   *
   * @throws SQLException when UTC already exists in datastore.
   */
  // TODO: encrypt
  public long insertGeoFixOrThrow(
      long time, double latitude, double longitude, float accuracy) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(Constants.KEY_UTC, time);
    initialValues.put(Constants.COL_LAT, latitude);
    initialValues.put(Constants.COL_LNG, longitude);
    initialValues.put(Constants.COL_ACC, accuracy);
    return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
  }

  // TODO: decrypt
  public Cursor getRecentGeoFixes(int size) {
    if (size <= 0) {
      return database.query(DATABASE_TABLE, DATABASE_COLUMNS, null, null, null,
          null, Constants.KEY_UTC + " DESC", null);
    }
    return database.query(DATABASE_TABLE, DATABASE_COLUMNS, null, null, null,
        null, Constants.KEY_UTC + " DESC", Integer.toString(size));
  }

  // TODO: decrypt
  public Cursor getGeoFixesByDateRange(DateRange dateRange) {
    long startMillis = dateRange.getStartDay().getTimeInMillis();
    long endMillis = dateRange.getEndDay().getTimeInMillis()
        + 24L * 3600 * 1000 - 1;

    return database.query(DATABASE_TABLE, DATABASE_COLUMNS,
        Constants.KEY_UTC + " BETWEEN ? AND ?", // inclusive
        new String[] { Long.toString(startMillis), Long.toString(endMillis) },
        null, null, Constants.KEY_UTC + " DESC", null);
  }

  public Calendar prevRecordDay(Calendar calendar) {
    return searchDayofGeoFix(calendar, true, true);
  }

  public Calendar nextRecordDay(Calendar calendar) {
    return searchDayofGeoFix(calendar, false, false);
  }

  public Calendar earliestRecordDay() {
    return searchDayofGeoFix(Calendar.getInstance(), true, false);
  }

  public void plusOneDay(DateRange dateRange) {
    Calendar nextDay;
    nextDay = nextRecordDay(dateRange.getEndDay());
    if (nextDay != null) {
      if (dateRange.isSingleDay())
        dateRange.setStartDay(nextDay);
      dateRange.setEndDay(nextDay);
    }
  }

  public void minusOneDay(DateRange dateRange) {
    Calendar prevDay;
    prevDay = prevRecordDay(dateRange.getStartDay());
    if (prevDay != null) {
      if (dateRange.isSingleDay())
        dateRange.setEndDay(prevDay);
      dateRange.setStartDay(prevDay);
    }
  }

  private void deleteByRange(long fromUtc, long toUtc) {
    database.delete(DATABASE_TABLE, Constants.KEY_UTC + ">=" + fromUtc
        + " AND " + Constants.KEY_UTC + "<" + toUtc, null);
  }

  private String getStringFromEntry(Cursor cursor) {
    return Long.toString(cursor.getLong(
        cursor.getColumnIndex(Constants.KEY_UTC))) + ","
        + Double.toString(cursor.getDouble(
        cursor.getColumnIndex(Constants.COL_LAT))) + ","
        + Double.toString(cursor.getDouble(
        cursor.getColumnIndex(Constants.COL_LNG))) + ","
        + Float.toString(cursor.getFloat(
        cursor.getColumnIndex(Constants.COL_ACC))) + "\n";
  }

  /**
   * prev = true , desc = true : return day of the latest fix BEFORE date
   * prev = false, desc = false: return day of the earliest fix AFTER date
   * prev = true , desc = false: return day of the earliest fix in DB
   * prev = false, desc = true : return day of the latest fix in DB
   */
  private Calendar searchDayofGeoFix(Calendar date, boolean prev, boolean desc) {
    Cursor cursor = database.query(DATABASE_TABLE, DATABASE_COLUMNS,
        Constants.KEY_UTC + (prev ? " < ?" : " >= ?"),
        new String[] {Long.toString(date.getTimeInMillis() + (prev ? 0 : (24L * 3600 * 1000)))},
        null, null, Constants.KEY_UTC + (desc ? " DESC" : " ASC"), "1");
    if (cursor.getCount() == 0) {
      cursor.close();
      return null;
    } else {
      cursor.moveToFirst();
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Constants.KEY_UTC)));
      cursor.close();
      return CalendarUtils.toBeginningOfDay(cal);
    }
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
      db.execSQL(CREATE_DATABASE);
    }

    @Override
    public void
    onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // TODO: Migrate all fixes.
      Log.w(Constants.TAG + ":" + TAG, "Upgrading database from version "
          + oldVersion + " to " + newVersion
          + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
      onCreate(db);
    }
  }
}
