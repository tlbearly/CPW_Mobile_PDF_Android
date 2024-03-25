package com.dnrcpw.cpwmobilepdf.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;

public class DBWayPtHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "wayPtsInfo";
    // Contacts table name
    private static final String TABLE_WAYPTS = "wayPts";
    // wayPts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_MAPNAME = "mapname";
    private static final String KEY_DESC = "descrption";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_COLOR = "color";
    private static final String KEY_TIME = "time";
    private static final String KEY_LOCATION = "location";

    public DBWayPtHandler(Context context) throws SQLException {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db1) throws SQLException {
        String CREATE_WAYPTS_TABLE = "CREATE TABLE " + TABLE_WAYPTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_MAPNAME + " TEXT, "
                + KEY_DESC + " TEXT, " + KEY_X + " FLOAT, "
                + KEY_Y + " FLOAT, " +KEY_COLOR + " TEXT, " + KEY_TIME + " TEXT, "
                + KEY_LOCATION + " TEXT"+")";
        db1.execSQL(CREATE_WAYPTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db1, int oldVersion, int newVersion) throws SQLException {
        // For new version do new stuff here
    }

    /*public void deleteTable(Context c){
        SQLiteDatabase db = this.getWritableDatabase();
        // delete maps table
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_WAYPTS);
        // Create maps table again
        onCreate(db);
        Toast.makeText(c, "All imported waypoints were deleted.", Toast.LENGTH_LONG).show();
    }*/

    // Adding waypoint
    public void addWayPt(WayPt wayPt) throws SQLException {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_MAPNAME, wayPt.getName()); // Name of map
            values.put(KEY_DESC, wayPt.getDesc()); // Waypoint description
            values.put(KEY_X, wayPt.getX()); // x screen coordinate
            values.put(KEY_Y, wayPt.getY()); // y screen coordinate
            values.put(KEY_COLOR, wayPt.getColorName()); // Color name of push pin image
            values.put(KEY_TIME, wayPt.getTime()); // Date and time of creation of waypoint
            values.put(KEY_LOCATION, wayPt.getLocation()); // Lat, Long
            // Inserting Row
            db.insert(TABLE_WAYPTS, null, values);
    }

    // Delete all WayPts for a given PDF map
    public void deleteWayPts(String mapName) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_WAYPTS, "mapName=?", new String[]{mapName});
    }

    // Getting one WayPt
    /*public WayPt getWayPt(int id) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WAYPTS, new String[]{KEY_ID,
                        KEY_MAPNAME, KEY_DESC, KEY_X, KEY_Y, KEY_COLOR, KEY_TIME, KEY_LOCATION}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        WayPt wayPt;
        try {
            wayPt = new WayPt(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getFloat(3), cursor.getFloat(4),
                    cursor.getString(5), cursor.getString(6), cursor.getString(7));
        } catch (NullPointerException e) {
            Toast.makeText(c, "Error reading database.", Toast.LENGTH_LONG).show();
            cursor.close();
            return null;
        }
        cursor.close();
        return wayPt;
    }*/

    // Getting All Waypoints from one PDF map
    public WayPts getWayPts(String mapName) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        WayPts wayPtsList = new WayPts(mapName);
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_WAYPTS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding waypoint to list if matches name
                if (mapName.equals(cursor.getString(1)))
                    wayPtsList.add(Integer.parseInt(cursor.getString(0)),cursor.getString(1),cursor.getString(2),cursor.getFloat(3),cursor.getFloat(4),cursor.getString(5),cursor.getString(6),cursor.getString(7));

            } while (cursor.moveToNext());
        }
        cursor.close();
        // return waypoints list
        return wayPtsList;
    }

    // Updating a Waypoint
    public int updateWayPt(WayPt wayPt) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, wayPt.getName());
        values.put(KEY_DESC, wayPt.getDesc());
        values.put(KEY_X, wayPt.getX());
        values.put(KEY_Y, wayPt.getY());
        values.put(KEY_COLOR, wayPt.getColorName());
        values.put(KEY_TIME, wayPt.getTime());
        values.put(KEY_LOCATION, wayPt.getLocation());

        // updating row
        return db.update(TABLE_WAYPTS, values, KEY_ID + " = ?",
                new String[]{ String.valueOf(wayPt.getId()) });
    }

    // Deleting a Waypoint
    public void deleteWayPt(WayPt wayPt) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WAYPTS, KEY_ID + " = ?",
                new String[] { String.valueOf(wayPt.getId()) });
    }

    // Deleting a Waypoint
    public void deleteWayPt(String mapName) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WAYPTS, KEY_MAPNAME + " = ?",
                new String[]{mapName});
    }
}