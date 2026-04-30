package com.dnrcpw.cpwmobilepdf.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dnrcpw.cpwmobilepdf.model.Track;
import com.dnrcpw.cpwmobilepdf.model.TrackSegment;
import com.dnrcpw.cpwmobilepdf.model.Tracks;
import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;

public class DBTrackHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "tracksInfo";
    // Contacts table name
    private static final String TABLE_TRACKS = "tracks";
    // wayPts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_MAPNAME = "mapname";
    private static final String KEY_DESC = "descrption";
    private static final String KEY_LINE_SEGMENTS = "linesegments"; // comma delimited x,y pairs
    private static final String KEY_COLOR = "color";
    private static final String KEY_TIME = "time";

    public DBTrackHandler(Context context) throws SQLException {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db1) throws SQLException {
        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_MAPNAME + " TEXT, "
                + KEY_DESC + " TEXT, " + KEY_LINE_SEGMENTS + " TEXT, "
                + KEY_COLOR + " TEXT, " + KEY_TIME + " TEXT)";
        db1.execSQL(CREATE_TRACKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db1, int oldVersion, int newVersion) throws SQLException {
        // For new version do new stuff here
    }

    public void addTrack(Track track) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, track.getMapName()); // Name of map
        values.put(KEY_DESC, track.getDesc()); // Waypoint description
        values.put(KEY_LINE_SEGMENTS, track.getLineSegments()); // String of x1,y1,x2,y2,x2,y2,x3,y3... line segments in lat, long
        values.put(KEY_COLOR, track.getColorName()); // Color name of pushpin image
        values.put(KEY_TIME, track.getTime()); // Date and time of creation of waypoint
        // Inserting Row
        db.insert(TABLE_TRACKS, null, values);
    }

    public void updateTrack(int id, TrackSegment trackSegment){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String strSegment = trackSegment.getSegment(); // get line segment as comma-delimited string x1,y1,x2,y2
        values.put(KEY_LINE_SEGMENTS, strSegment); // Name of map
    }
    public int updateTrack(Track track){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAPNAME, track.getMapName()); // Name of map
        values.put(KEY_DESC, track.getDesc()); // Waypoint description
        values.put(KEY_LINE_SEGMENTS, track.getLineSegments()); // String of x1,y1,x2,y2,x2,y2,x3,y3... line segments in lat, long
        values.put(KEY_COLOR, track.getColorName()); // Color name of pushpin image
        values.put(KEY_TIME, track.getTime()); // Date and time of creation of waypoint
        // updating row
        return db.update(TABLE_TRACKS, values, KEY_ID + " = ?",
                new String[]{ String.valueOf(track.getId()) });
    }

    // Delete all Tracks for a given PDF map
    public void deleteTracks(String mapName) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_TRACKS, "mapName=?", new String[]{mapName});
    }
    // Deleting a Waypoint
    public void deleteTrack(Track track) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] { String.valueOf(track.getId()) });
    }

    public Tracks getTracks(String mapName) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        Tracks trackList = new Tracks(mapName);
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_TRACKS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding waypoint to list if matches name
                if (mapName.equals(cursor.getString(1)))
                    trackList.add(Integer.parseInt(cursor.getString(0)),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getString(5));

            } while (cursor.moveToNext());
        }
        cursor.close();
        // return waypoints list
        return trackList;
    }
}
