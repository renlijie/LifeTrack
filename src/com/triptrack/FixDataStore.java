package com.triptrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
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

public class FixDataStore {
    private static final String TAG = "FixDataStore";

    private static final String DATABASE_NAME = "triptrack";
    private static final String DATABASE_TABLE = "fixes";
    private static final int DATABASE_VERSION = 4;

    private final Context context;

    //TODO: add UI handler from ControlPanelActivity

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE
            + " (" + Constants.KEY_UTC + " long primary key, "
            + Constants.KEY_LAT + " double, " + Constants.KEY_LNG + " double, "
            + Constants.KEY_ACC + " single);";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void
        onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: Migrate all fixes.
            Log.w(Constants.TAG + ":" + TAG, "Upgrading database from version " + oldVersion
                    + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    public FixDataStore(Context context) {
        this.context = context;
    }

    public FixDataStore open() {
        databaseHelper = new DatabaseHelper(context);
        int waitMillis = 100;
        while (true) {
            try {
                database = databaseHelper.getWritableDatabase();
                break;
            } catch (SQLException e) {
                Log.w(Constants.TAG + ":" + TAG, "Database not available. Wait for "
                        + Integer.toString(waitMillis) + " ms.");
            }
            try {
                // Wait in case the database is not immediately available.
                Thread.sleep(waitMillis);
                if (waitMillis < 5000) {
                    waitMillis *= 2;
                } else {
                    // TODO: show Toast("Database not available. Try again later.")
                }
            } catch (InterruptedException e) {
                Log.w(Constants.TAG + ":" + TAG, e.toString());
            }
        }
        // Log.d(Constants.TAG + ":" + TAG, "Database opened successfully.");
        return this;
    }

    public void close() {
        databaseHelper.close();
    }

    public void exportToFile(File file, Handler handler) {
        Cursor cursor = fetchFixes(0);
        if (cursor.moveToFirst()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write((cursor.getCount() + "\n").getBytes());
            } catch (IOException e) {
                handler.sendMessage(Message.obtain(handler, Constants.HANDLER_TOAST, 0, 0,
                        "Opening " + file + " failed. Do you have access?"));
                Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
                return;
            }

            int size = 0;
            handler.sendMessage(Message.obtain(handler,
                    Constants.HANDLER_PROGRESSBAR_SHOWMAX, cursor.getCount(), 0, null));

            while (true) {
                String r = Long.toString(cursor.getLong((cursor.getColumnIndex(Constants.KEY_UTC)))) + ","
                        + Double.toString(cursor.getDouble(cursor.getColumnIndex(Constants.KEY_LAT))) + ","
                        + Double.toString(cursor.getDouble(cursor.getColumnIndex(Constants.KEY_LNG))) + ","
                        + Float.toString(cursor.getFloat(cursor.getColumnIndex(Constants.KEY_ACC))) + "\n";
                try {
                    fos.write(r.getBytes());
                } catch (IOException e) {
                    handler.sendMessage(Message.obtain(handler,
                            Constants.HANDLER_TOAST, 0, 0, "Writing to " + file + " failed."));
                    Log.w(Constants.TAG + ":" + TAG, "Writing " + r + " failed.");
                }
                size++;
                if (size % 1000 == 0) {
                    handler.sendMessage(Message.obtain(handler,
                            Constants.HANDLER_PROGRESSBAR_SETPROGRESS, size, 0, null));
                }
                if (cursor.isLast()) {
                    break;
                }
                cursor.moveToNext();
            }

            try {
                fos.close();
            } catch (IOException e) {
                handler.sendMessage(Message.obtain(handler,
                        Constants.HANDLER_TOAST, 0, 0, "Closing " + file + " failed."));
                Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.");
            }
            handler.sendMessage(Message.obtain(handler, Constants.HANDLER_TOAST,
                    0, 0, context.getString(R.string.export_finished)));
            Log.d(Constants.TAG + ":" + TAG,
                    context.getString(R.string.export_finished));
        } else {
            handler.sendMessage(Message.obtain(handler, Constants.HANDLER_TOAST,
                    0, 0, context.getString(R.string.export_nothing)));
        }
        cursor.close();
    }

    public void importFromFile(File file, Handler handler) {
        // Open a reader.
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (IOException e) {
            handler.sendMessage(Message.obtain(handler,
                    Constants.HANDLER_TOAST, 0, 0, "Opening " + file + " failed. Does it exist?"));
            Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
            return;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fis)));

        // Read the first line as the total number of fixes.
        String str;
        int size;
        try {
            str = br.readLine();
        } catch (IOException e) {
            handler.sendMessage(Message.obtain(handler,
                    Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
            Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.");
            return;
        }
        try {
            size = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // If the first line cannot be parsed as an int, create a new reader
            // to read from the beginning of the file, in case the first line is
            // actually a fix.
            try {
                br.close();
                fis = new FileInputStream(file);
            } catch (IOException ex) {
                handler.sendMessage(Message.obtain(handler,
                        Constants.HANDLER_TOAST, 0, 0, "Opening " + file + " failed. Does it exist?"));
                Log.w(Constants.TAG + ":" + TAG, "Opening " + file + " failed.");
                return;
            }
            br = new BufferedReader(new InputStreamReader(new DataInputStream(fis)));

            // Assign a fake size.
            size = Integer.MAX_VALUE;

            // Notify the user and set the flag. Continue.
            handler.sendMessage(Message.obtain(handler, Constants.HANDLER_TOAST,
                    0, 0, "Reading size failed. But continue."));
            Log.w(Constants.TAG + ":" + TAG, "Reading size failed." + str);
        }

        // Show the progress bar.
        handler.sendMessage(Message.obtain(handler,
                Constants.HANDLER_PROGRESSBAR_SHOWMAX, size, 0, null));

        long utc;
        double lat, lng;
        float acc;

        // The index of fix currently being imported.
        size = 0;
        // The number of fixes written successfully.
        int goodSize = 0;
        // The number of illegal fixes.
        int badSize = 0;
        // The number of fixes written unsuccessfully.
        int dupSize = 0;

        // Use transaction to significantly improve efficiency.
        database.beginTransaction();

        while (true) {
            size++;
            if (size % 1000 == 0) {
                handler.sendMessage(Message.obtain(handler,
                        Constants.HANDLER_PROGRESSBAR_SETPROGRESS, size, 0, null));
            }

            // Read the current line. Should contain a fix.
            try {
                str = br.readLine();
            } catch (IOException e) {
                handler.sendMessage(Message.obtain(handler,
                        Constants.HANDLER_TOAST, 0, 0, "Reading " + file + " failed."));
                Log.w(Constants.TAG + ":" + TAG, "Reading " + file + " failed.");
                return;
            }
            if (str == null) {
                break;
            }

            // Parse the string as a fix.
            String[] entry = str.split(",");
            if (entry.length < 4) {
                badSize++;
                Log.e(Constants.TAG + ":" + TAG, str + " is not valid data!");
                continue;
            }

            // Validate the data.
            try {
                utc = Long.parseLong(entry[0]);
                lat = Double.parseDouble(entry[1]);
                lng = Double.parseDouble(entry[2]);
                acc = Float.parseFloat(entry[3]);
            } catch (NumberFormatException e) {
                badSize++;
                Log.w(Constants.TAG + ":" + TAG, "Reading fix #" + size + " failed.");
                continue;
            }
            if (utc < 0 || lat < -90 || lat > 90 || lng > 180 || lng < -180 ||
                    acc < 0) {
                badSize++;
                Log.w(Constants.TAG + ":" + TAG, "Not valid: fix #" + size + ".");
                continue;
            }

            // Add to database. If the fix is already in the database, an
            // SQLException will occur. Catch it.
            try {
                createFix(utc, lat, lng, acc);
                goodSize++;
            } catch (SQLException e) {
                dupSize++;
                Log.w(Constants.TAG + ":" + TAG, "SQLException at fix #" + size + ".");
            }
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        // Notify the user about the stats.
        String stats = "Done. Read " + Integer.toString(goodSize + dupSize + badSize) + " fixes.\n"
                + Integer.toString(goodSize) + " fixes are written successfully.\n"
                + Integer.toString(dupSize) + " timestamps already exist in the database.\n"
                + Integer.toString(badSize) + " fixes are not valid.";
        handler.sendMessage(Message.obtain(handler, Constants.HANDLER_TOAST, 0, 0, stats));
        Log.d(Constants.TAG + ":" + TAG, stats);

        // Close the reader.
        try {
            br.close();
        } catch (IOException e) {
            handler.sendMessage(Message.obtain(handler,
                    Constants.HANDLER_TOAST, 0, 0, "Closing " + file + " failed."));
            Log.w(Constants.TAG + ":" + TAG, "Closing " + file + " failed.");
        }
    }

    public void clearHistory() {
        database.execSQL("DELETE FROM " + DATABASE_TABLE);
    }

    public void deleteSingle(long utc) {
        database.delete(DATABASE_TABLE,
                Constants.KEY_UTC + "=" + Long.toString(utc), null);
    }

    public void delete(long fromUtc, long toUtc) {
        database.delete(DATABASE_TABLE, Constants.KEY_UTC + ">=" + fromUtc
                + " AND " + Constants.KEY_UTC + "<" + toUtc, null);
    }

    public void deleteDay(long utc) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(utc);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long fromUtc = cal.getTimeInMillis();
        long toUtc = fromUtc + 24L * 3600 * 1000;
        database.delete(DATABASE_TABLE, Constants.KEY_UTC + ">=" + Long.toString(fromUtc)
                + " AND " + Constants.KEY_UTC + "<" + Long.toString(toUtc), null);
    }

    // TODO: encrypt
    public long createFix(Location location) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Constants.KEY_UTC, location.getTime());
        initialValues.put(Constants.KEY_LAT, location.getLatitude());
        initialValues.put(Constants.KEY_LNG, location.getLongitude());
        initialValues.put(Constants.KEY_ACC, location.getAccuracy());
        return database.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * @param time
     * @param latitude
     * @param longitude
     * @param accuracy
     * @return
     * @throws SQLException when utc already exists. Caught in import().
     */
    public long createFix(long time, double latitude, double longitude,
                          float accuracy) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(Constants.KEY_UTC, time);
        initialValues.put(Constants.KEY_LAT, latitude);
        initialValues.put(Constants.KEY_LNG, longitude);
        initialValues.put(Constants.KEY_ACC, accuracy);
        return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    // TODO: decrypt
    public Cursor fetchFixes(int size) {
        if (size <= 0) {
            return database.query(DATABASE_TABLE,
                    new String[]{Constants.KEY_UTC, Constants.KEY_LAT,
                            Constants.KEY_LNG, Constants.KEY_ACC},
                    null, null, null, null, Constants.KEY_UTC + " DESC", null);
        }
        return database.query(DATABASE_TABLE,
                new String[]{Constants.KEY_UTC, Constants.KEY_LAT,
                        Constants.KEY_LNG, Constants.KEY_ACC},
                null, null, null, null, Constants.KEY_UTC + " DESC",
                Integer.toString(size));
    }

    public Cursor fetchFixes(Calendar startDay, Calendar endDay) {
        long startMillis, endMillis;
        if (startDay != null)
            startMillis = startDay.getTimeInMillis();
        else
            startMillis = Long.MIN_VALUE;
        if (endDay != null)
            endMillis = endDay.getTimeInMillis() + 24L * 3600 * 1000 - 1;
        else
            endMillis = Long.MAX_VALUE;

        return database.query(DATABASE_TABLE,
                new String[]{Constants.KEY_UTC, Constants.KEY_LAT,
                        Constants.KEY_LNG, Constants.KEY_ACC},
                Constants.KEY_UTC + " BETWEEN ? AND ?", // inclusive
                new String[]{Long.toString(startMillis), Long.toString(endMillis)},
                null, null, Constants.KEY_UTC + " DESC", null);
    }

    public Calendar previousRecordDay(Calendar calendar) {
        Cursor cursor =
                database.query(DATABASE_TABLE,
                        new String[]{Constants.KEY_UTC, Constants.KEY_LAT,
                                Constants.KEY_LNG, Constants.KEY_ACC},
                        Constants.KEY_UTC + " < ?",
                        new String[]{Long.toString(calendar.getTimeInMillis())},
                        null, null, Constants.KEY_UTC + " DESC", "1");
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            cursor.moveToFirst();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Constants.KEY_UTC)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cursor.close();
            return cal;
        }
    }

    public Calendar nextRecordDay(Calendar calendar) {
        Cursor cursor = database.query(DATABASE_TABLE,
                new String[]{Constants.KEY_UTC, Constants.KEY_LAT,
                        Constants.KEY_LNG, Constants.KEY_ACC},
                Constants.KEY_UTC + " >= ?",
                new String[]{Long.toString(calendar.getTimeInMillis()
                        + 24L * 3600 * 1000)},
                null, null, Constants.KEY_UTC + " ASC", "1");
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            cursor.moveToFirst();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Constants.KEY_UTC)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cursor.close();
            return cal;
        }
    }
}
