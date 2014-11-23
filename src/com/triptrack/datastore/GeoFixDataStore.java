package com.triptrack.datastore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.triptrack.DateRange;
import com.triptrack.Fix;
import com.triptrack.util.CalendarUtils;
import com.triptrack.util.Constants;
import com.triptrack.util.Cursors;
import com.triptrack_beta.R;

import java.io.*;
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
      Constants.COL_ACC,
  };
  private static final String CREATE_DATABASE =
      "create table " + DATABASE_TABLE + " ("
          + Constants.KEY_UTC + " long primary key, "
          + Constants.COL_LAT + " double, "
          + Constants.COL_LNG + " double, "
          + Constants.COL_ACC + " float);";

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
                + Integer.toString(waitInMillis) + " ms.", e);
      }
      try {
        // Wait in case the database is not immediately available.
        Thread.sleep(waitInMillis);
        if (waitInMillis < 5000) {
          waitInMillis *= 2;
        }
      } catch (InterruptedException e) {
        Log.w(Constants.TAG + ":" + TAG, e);
      }
    }
    return this;
  }

  public void close() {
    databaseHelper.close();
  }

  public void exportToFile(File file, Handler uiHandler) {
    Cursor cursor = getAllGeoFixes();

    if (cursor.moveToFirst()) { // oldest first
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(file);
        // write total number of fixes
        fos.write((cursor.getCount() + "\n").getBytes());

        int numGeoFixesExported = 0;
        uiHandler.sendMessage(Message.obtain(uiHandler,
            Constants.HANDLER_PROGRESSBAR_SHOWMAX, cursor.getCount(), 0, null));
        uiHandler.sendMessage(Message.obtain(
            uiHandler, Constants.HANDLER_PROGRESSBAR_SETPROGRESS,
            0, 0, null));

        // write all fixes
        while (true) {
          String geoFix = cursorToString(cursor);
          try {
            fos.write(geoFix.getBytes());
          } catch (IOException e) {
            uiHandler.sendMessage(Message.obtain(
                uiHandler, Constants.HANDLER_TOAST, 0, 0,
                "Writing to " + file + " failed."));
            Log.w(Constants.TAG + ":" + TAG,
                "Writing " + geoFix + " failed.", e);
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
      } catch (IOException e) {
        uiHandler.sendMessage(Message.obtain(
            uiHandler, Constants.HANDLER_TOAST, 0, 0,
            "Operating " + file + " failed. Do you have access?"));
        Log.w(Constants.TAG + ":" + TAG, "Operating " + file + " failed.", e);
        return;
      } finally {
        if (fos != null) {
          try {
            fos.close();
          } catch (IOException e) {
            uiHandler.sendMessage(Message.obtain(
                uiHandler, Constants.HANDLER_TOAST, 0, 0,
                "Closing " + file + " failed. Do you have access?"));
            Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.", e);
          }
        }
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
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      // Read the first line containing the total number of fixes.
      String line;
      int numFixes;
      try {
        line = br.readLine();
      } catch (IOException e) {
        uiHandler.sendMessage(Message.obtain(uiHandler,
            Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
        Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.", e);
        return;
      }
      try {
        numFixes = Integer.parseInt(line);
      } catch (NumberFormatException e) {
        uiHandler.sendMessage(Message.obtain(
            uiHandler, Constants.HANDLER_TOAST, 0, 0,
            "Reading size failed. The first line must contain an integer."));
        Log.w(Constants.TAG + ":" + TAG, "Reading size failed." + line, e);
        return;
      }

      // Show the progress bar.
      uiHandler.sendMessage(Message.obtain(uiHandler,
          Constants.HANDLER_PROGRESSBAR_SHOWMAX, numFixes, 0, null));
      uiHandler.sendMessage(Message.obtain(
          uiHandler, Constants.HANDLER_PROGRESSBAR_SETPROGRESS,
          0, 0, null));

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
          Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.", e);
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
              "Non-parseable fix: #" + numImportedFixes, e);
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
    } catch (IOException e) {
      uiHandler.sendMessage(Message.obtain(uiHandler, Constants.HANDLER_TOAST,
          0, 0, "Operating " + file + " failed. Does it exist?"));
      Log.w(Constants.TAG + ":" + TAG, "Operating " + file + " failed.", e);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          uiHandler.sendMessage(Message.obtain(
              uiHandler, Constants.HANDLER_TOAST, 0, 0,
              "Closing " + file + " failed."));
          Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.", e);
        }
      }
    }
  }

  public void clearHistory() {
    database.execSQL("DELETE FROM " + DATABASE_TABLE);
  }

  public void deleteGeoFix(long utc) {
    deleteByRange(utc, utc - 1);
  }

  public void deleteOneDay(long utc) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(utc);

    long fromUtc = CalendarUtils.toBeginningOfDay(cal).getTimeInMillis();
    long toUtc = fromUtc + 24L * 3600 * 1000;
    deleteByRange(fromUtc, toUtc);
  }

  /**
   * Deletes fixes in range [fromUtc, toUtc).
   */
  private void deleteByRange(long fromUtc, long toUtc) {
    database.delete(
        DATABASE_TABLE,
        Constants.KEY_UTC + ">=? AND " + Constants.KEY_UTC + "<?",
        new String[] {Long.toString(fromUtc), Long.toString(toUtc)});
  }

  /**
   * Writes the fix into datastore.
   *
   * @throws SQLException when UTC already exists in datastore.
   *
   * TODO: encrypt
   */
  public void insertGeoFixOrThrow(
      long utc, double latitude, double longitude, float accuracy) {
    ContentValues newFix = new ContentValues();
    newFix.put(Constants.KEY_UTC, utc);
    newFix.put(Constants.COL_LAT, latitude);
    newFix.put(Constants.COL_LNG, longitude);
    newFix.put(Constants.COL_ACC, accuracy);
    database.insertOrThrow(DATABASE_TABLE, null, newFix);
  }

  public Cursor getRecentGeoFixes(int size) {
    return database.query(
        DATABASE_TABLE,
        DATABASE_COLUMNS,
        null, // selection
        null, // selectinoArgs
        null, // groupBy
        null, // having
        Constants.KEY_UTC + " DESC",
        Integer.toString(size));
  }

  /**
   * Returns all fixes from the oldest to the latest.
   */
  public Cursor getAllGeoFixes() {
    return database.query(
        DATABASE_TABLE,
        DATABASE_COLUMNS,
        null, // selection
        null, // selectinoArgs
        null, // groupBy
        null, // having
        Constants.KEY_UTC + " ASC",
        null); // limit
  }

  // TODO: decrypt

  /**
   * Gets most recent to old fixes.
   */
  public Cursor getGeoFixesByDateRange(DateRange dateRange) {
    long startMillis = dateRange.getStartDay().getTimeInMillis();
    long endMillis = dateRange.getEndDay().getTimeInMillis()
        + 24L * 3600 * 1000 - 1;

    return database.query(
        DATABASE_TABLE,
        DATABASE_COLUMNS,
        Constants.KEY_UTC + " BETWEEN ? AND ?", // inclusive
        new String[] {Long.toString(startMillis), Long.toString(endMillis)},
        null, // groupBy
        null, // having
        Constants.KEY_UTC + " DESC",
        null); // limit
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

  private String cursorToString(Cursor cursor) {
    return Long.toString(Cursors.getUtc(cursor)) + ","
        + Double.toString(Cursors.getLat(cursor)) + ","
        + Double.toString(Cursors.getLng(cursor)) + ","
        + Float.toString(Cursors.getAcc(cursor)) + "\n";
  }

  /**
   * prev = true , desc = true : returns the day of the latest fix BEFORE date.
   * prev = false, desc = false: returns the day of the earliest fix AFTER date.
   * prev = true , desc = false: returns the day of the earliest fix in DB.
   * prev = false, desc = true : returns the day of the latest fix in DB.
   */
  private Calendar searchDayofGeoFix(Calendar date, boolean prev, boolean desc) {
    Cursor cursor = database.query(
        DATABASE_TABLE,
        DATABASE_COLUMNS,
        Constants.KEY_UTC + (prev ? "<?" : ">=?"),
        new String[] {Long.toString(
            date.getTimeInMillis() + (prev ? 0 : (24L * 3600 * 1000)))},
        null,
        null,
        Constants.KEY_UTC + (desc ? " DESC" : " ASC"),
        "1");
    if (cursor.getCount() == 0) {
      cursor.close();
      return null;
    } else {
      cursor.moveToFirst();
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(Cursors.getUtc(cursor));
      cursor.close();
      return CalendarUtils.toBeginningOfDay(cal);
    }
  }

  /**
   * Gets next Fix with UTC < utc.
   */
  private Fix getPreviousGeoFix(long utc) {
    Cursor cursor = database.query(DATABASE_TABLE, DATABASE_COLUMNS,
        Constants.KEY_UTC + " < ?", new String[] {Long.toString(utc)},
        null, null, Constants.KEY_UTC + " DESC", "1");
    if (cursor.getCount() == 0) {
      cursor.close();
      return null;
    } else {
      cursor.moveToFirst();
      Fix fix = new Fix(
          Cursors.getUtc(cursor),
          Cursors.getLat(cursor),
          Cursors.getLng(cursor),
          Cursors.getAcc(cursor));
      cursor.close();
      return fix;
    }
  }

  /**
   * Gets next Fix with UTC > utc.
   */
  private Fix getNextGeoFix(long utc) {
    Cursor cursor = database.query(
        DATABASE_TABLE,
        DATABASE_COLUMNS,
        Constants.KEY_UTC + " >?",
        new String[] {Long.toString(utc)},
        null, // groupBy
        null, // having
        Constants.KEY_UTC + " ASC",
        "1");
    if (cursor.getCount() == 0) {
      cursor.close();
      return null;
    } else {
      cursor.moveToFirst();
      Fix fix = new Fix(
          Cursors.getUtc(cursor),
          Cursors.getLat(cursor),
          Cursors.getLng(cursor),
          Cursors.getAcc(cursor));
      cursor.close();
      return fix;
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
