package com.example.tammy.mapviewer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by tammy on 12/6/2017.
 */

public class DBHandler extends SQLiteOpenHelper {
    private static DBHandler mInstance = null;

    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "mapsInfo";
    // Contacts table name
    private static final String TABLE_MAPS = "maps";
    // Shops Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";
    private static final String KEY_BOUNDS = "bounds";
    private static final String KEY_MEDIABOX = "mediabox";
    private static final String KEY_VIEWPORT = "viewport";
    private static final String KEY_THUMBNAIL = "thumbnail";
    private static final String KEY_NAME = "name";
    //private Context context;

    public static DBHandler getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DBHandler(context); // throws null pointer context.getApplicationContext()
        }
        return mInstance;
    }
    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MAPS_TABLE = "CREATE TABLE " + TABLE_MAPS + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PATH + " TEXT,"
            + KEY_BOUNDS + " TEXT," + KEY_MEDIABOX + " TEXT,"
            + KEY_VIEWPORT + " TEXT, "+KEY_THUMBNAIL + " BLOB," + KEY_NAME + " TEXT"+")";
        db.execSQL(CREATE_MAPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For new version do new stuff here
    }

    public void deleteTable(Context c){
        SQLiteDatabase db = this.getWritableDatabase();
        // delete maps table
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_MAPS);
        // Create maps table again
        onCreate(db);
        Toast.makeText(c, "All imported maps were deleted.", Toast.LENGTH_LONG).show();
    }

    // Adding new PDF Map
    public int addMap(PDFMap map) throws SQLException {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_PATH, map.getPath()); // Path and file name of map
            values.put(KEY_BOUNDS, map.getBounds()); // Lat/Long Bounds of the map
            values.put(KEY_MEDIABOX, map.getMediabox()); // Pixel Bounds of the map
            values.put(KEY_VIEWPORT, map.getViewport()); // Margins
            values.put(KEY_THUMBNAIL, map.getThumbnail()); // Thumbnail image
            values.put(KEY_NAME, map.getName()); // Map name without path
            // Inserting Row
            db.insert(TABLE_MAPS, null, values);
            //db.close(); // Closing database connection
        }
        catch (SQLException e){
            return -1;
        }
        return 0;
    }

    // Getting one PDF Map
    public PDFMap getMap(int id) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MAPS, new String[]{KEY_ID,
            KEY_PATH, KEY_BOUNDS, KEY_MEDIABOX, KEY_VIEWPORT, KEY_THUMBNAIL, KEY_NAME}, KEY_ID + "=?",
            new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        PDFMap map = new PDFMap(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4),
                cursor.getBlob(5), cursor.getString(6));
       cursor.close();
        // return geo pdf map
        return map;
    }

    // Getting All PDF Maps
    public ArrayList<PDFMap> getAllMaps() throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<PDFMap> mapList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MAPS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                PDFMap map = new PDFMap();
                map.setId(Integer.parseInt(cursor.getString(0)));
                map.setPath(cursor.getString(1));
                map.setBounds(cursor.getString(2));
                map.setMediabox(cursor.getString(3));
                map.setViewport(cursor.getString(4));
                map.setThumbnail(cursor.getBlob(5));
                map.setName(cursor.getString(6));
                // Adding map to list
                mapList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // return map list
        return mapList;
    }

    // Updating a PDF Map
    public int updateMap(PDFMap map) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, map.getPath());
        values.put(KEY_BOUNDS, map.getBounds());
        values.put(KEY_MEDIABOX, map.getMediabox());
        values.put(KEY_VIEWPORT, map.getViewport());
        values.put(KEY_THUMBNAIL, map.getThumbnail());
        values.put(KEY_NAME, map.getName());

        // updating row
        int err = db.update(TABLE_MAPS, values, KEY_ID + " = ?",
            new String[]{ String.valueOf(map.getId()) });
        return err;
    }

    // Deleting a PDF Map
    public void deleteMap(PDFMap map) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MAPS, KEY_ID + " = ?",
            new String[] { String.valueOf(map.getId()) });
        //db.close();
    }
}