package com.example.tammy.pocketmaps.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.tammy.pocketmaps.Model.PDFMap;
import com.example.tammy.pocketmaps.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by tammy on 12/6/2017.
 */

public class DBHandler extends SQLiteOpenHelper {
    //private static DBHandler mInstance = null;
    //private final Context c;
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "mapsInfo";
    // Maps table name
    private static final String TABLE_MAPS = "maps";
    // Maps Table Column Names
    private static final String KEY_ID = "id";
    private static final String KEY_PATH = "path";
    private static final String KEY_BOUNDS = "bounds";
    private static final String KEY_MEDIABOX = "mediabox";
    private static final String KEY_VIEWPORT = "viewport";
    private static final String KEY_THUMBNAIL = "thumbnail";
    private static final String KEY_NAME = "name";
    private static final String KEY_FILESIZE = "filesize";
    private static final String KEY_DISTTOMAP = "disttomap";


    //private Context context;
    // Settings table name. Store user preferred settings here
    private static final String TABLE_SETTINGS = "settings";
    // Settings table column names
    private static final String KEY_SETTINGS_ID = "id";
    private static final String KEY_MAP_SORT = "map_sort"; // Imported maps sort order. Valid values: name, date, or size

    //public static DBHandler getInstance(Context context) {
    /*public static synchronized DBHandler getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DBHandler(context.getApplicationContext());
        }
        return mInstance;
    }*/
    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //this.c = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) throws SQLException {
            // Create Imported Maps Table
            createMapsTable(db);

            // Create User Settings Table
            createSettingsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) throws SQLException {
        // For new version do new stuff here. Drop tables and call onCreate
         if (oldVersion != newVersion) {
             db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAPS);
             db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
             onCreate(db);
        }
    }

    /*
    * CRUD OPERATIONS: Create, Read, Update, and Delete
     */
    private void createMapsTable (SQLiteDatabase db) throws SQLException {
        // Create Imported Maps Table
        String CREATE_MAPS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MAPS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PATH + " TEXT,"
                + KEY_BOUNDS + " TEXT," + KEY_MEDIABOX + " TEXT,"
                + KEY_VIEWPORT + " TEXT, " + KEY_THUMBNAIL + " BLOB,"
                + KEY_NAME + " TEXT," + KEY_FILESIZE + " TEXT,"
                + KEY_DISTTOMAP + " TEXT" + ")";
        db.execSQL(CREATE_MAPS_TABLE);
    }

    public void deleteTableMaps() throws SQLException {
        // Delete and recreate Table_Maps
        SQLiteDatabase db = this.getWritableDatabase();
        // delete maps table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAPS);
        // Create maps table again
        onCreate(db);
    }

    // Adding new PDF Map
    public void addMap(PDFMap map) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATH, map.getPath()); // Path and file name of map
        values.put(KEY_BOUNDS, map.getBounds()); // Lat/Long Bounds of the map
        values.put(KEY_MEDIABOX, map.getMediabox()); // Pixel Bounds of the map
        values.put(KEY_VIEWPORT, map.getViewport()); // Margins
        values.put(KEY_THUMBNAIL, map.getThumbnail()); // Thumbnail image
        values.put(KEY_NAME, map.getName()); // Map name without path
        values.put(KEY_FILESIZE, map.getFileSize()); // Map pdf file size 267 Kb
        values.put(KEY_DISTTOMAP, map.getDistToMap()); // Current distance to map
        // Inserting Row
        db.insert(TABLE_MAPS, null, values);
        //db.close(); // 5-21-21 Closing database connection
    }

    // Getting one PDF Map
   /* public PDFMap getMap(int id) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MAPS, new String[]{KEY_ID,
            KEY_PATH, KEY_BOUNDS, KEY_MEDIABOX, KEY_VIEWPORT, KEY_THUMBNAIL, KEY_NAME, KEY_FILESIZE, KEY_DISTTOMAP}, KEY_ID + "=?",
            new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        try {
            PDFMap map = new PDFMap(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4),
                    cursor.getString(5), cursor.getString(6), cursor.getString (7), cursor.getString(8));
            cursor.close();
            //db.close();
            // return geo pdf map
            return map;
        }catch(NullPointerException e) {
            //db.close();
            //Toast.makeText(c, "Error reading database.", Toast.LENGTH_LONG).show();
            throw e;
            //return null;
        }
    }*/

    // Getting All PDF Maps
    public ArrayList<PDFMap> getAllMaps(Context c) throws SQLException {
        // Called by CustomAdapter creation
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<PDFMap> mapList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MAPS;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // If user database does not contain all of the fields recreate it preserving the user's maps.
        if (cursor.getColumnCount() != 9) {
            cursor.close();
            mapList = recreateDB(c);
            return mapList;
        } else {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    PDFMap map = new PDFMap();
                    map.setId(Integer.parseInt(cursor.getString(0)));
                    map.setPath(cursor.getString(1));
                    map.setBounds(cursor.getString(2));
                    map.setMediabox(cursor.getString(3));
                    map.setViewport(cursor.getString(4));
                    // thumbnail was saved to a file, get the path
                    map.setThumbnail(cursor.getString(5));
                    map.setName(cursor.getString(6));
                    map.setFileSize(cursor.getString(7));
                    map.setDistToMap(cursor.getString(8));
                    if (map.getDistToMap().equals("")) {
                        map.setMiles(0.0);
                    }
                    else {
                        map.setMiles(Double.parseDouble(map.getDistToMap()));
                    }
                    // Adding map to list
                    mapList.add(map);
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        //db.close(); // 5-21-21
        // return map list
        return mapList;
    }

    // Recreate database if they do not have all of the fields
    private ArrayList<PDFMap> recreateDB(Context c) throws SQLException {
        // Read what is currently in the database into mapList.
        // Delete database and recreate it. Add maps that they had.
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<PDFMap> mapList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MAPS;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Save user maps in mapList
        if (cursor.moveToFirst()) {
            do {
                PDFMap map = new PDFMap();
                map.setId(Integer.parseInt(cursor.getString(0)));
                map.setPath(cursor.getString(1));
                map.setBounds(cursor.getString(2));
                map.setMediabox(cursor.getString(3));
                map.setViewport(cursor.getString(4));
                // thumbnail was saved to a file, get the path
                map.setThumbnail(cursor.getString(5));
                map.setName(cursor.getString(6));
                if (cursor.getColumnCount() == 9) {
                    map.setFileSize(cursor.getString(7));
                    map.setDistToMap(cursor.getString(8));
                }
                else {
                    // Calculate the File Size
                    File file = new File(map.getPath());
                    String fileSize;
                    long size = file.length() / 1024; // Get size and convert bytes into Kb.
                    if (size >= 1024) {
                        double sizeDbl = (double) size;
                        fileSize = String.format(Locale.ENGLISH,"%.1f%s", (sizeDbl / 1024),c.getResources().getString(R.string.Mb));
                    } else {
                        fileSize = size + c.getResources().getString(R.string.Kb);
                    }
                    map.setFileSize(fileSize);
                    map.setDistToMap("");
                }
                // Adding map to list
                mapList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        //db.close(); // 5-21-21

        // remove database and add again
        deleteTableMaps();

        // fill database
        for (int i=0; i<mapList.size(); i++){
            addMap(mapList.get(i));
        }

        return mapList;
    }

    // Updating a PDF Map
    public void updateMap(PDFMap map) throws SQLException {
        // updat a map in the database
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PATH, map.getPath());
        values.put(KEY_BOUNDS, map.getBounds());
        values.put(KEY_MEDIABOX, map.getMediabox());
        values.put(KEY_VIEWPORT, map.getViewport());
        values.put(KEY_THUMBNAIL, map.getThumbnail());
        values.put(KEY_NAME, map.getName());
        values.put(KEY_FILESIZE, map.getFileSize());
        values.put(KEY_DISTTOMAP, map.getDistToMap());

        // updating row
        db.update(TABLE_MAPS, values, KEY_ID + " = ?",
            new String[]{String.valueOf(map.getId())});
        //db.close(); // 5-21-21
    }

    // Deleting a PDF Map
    public void deleteMap(PDFMap map) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MAPS, KEY_ID + " = ?",
                new String[]{String.valueOf(map.getId())});
        //db.close(); // 5-21-21
    }

    //-----------------
    //  USER SETTINGS
    //-----------------
    public void createSettingsTable(SQLiteDatabase db){
       // SQLiteDatabase db = this.getWritableDatabase();
        String CREATE_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + "("
                + KEY_SETTINGS_ID + " INTEGER PRIMARY KEY," + KEY_MAP_SORT + " TEXT)";
        db.execSQL(CREATE_SETTINGS_TABLE);
        // Insert default user settings
        ContentValues values = new ContentValues();
        values.put(KEY_MAP_SORT, "name");
        db.insert(TABLE_SETTINGS, null, values);
    }
   /* public void resetSettings(){
        SQLiteDatabase db = this.getWritableDatabase();
        // delete maps table
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_SETTINGS);
        // Create settings table again
        createSettingsTable();
        //db.close();
      //  Toast.makeText(c, "Reverted to default settings.", Toast.LENGTH_LONG).show();
    }*/

    //----------------
    //     SORTING
    //---------------
    public void setMapSort(String order) throws SQLException{
        // How to sort the MainActivity imported maps
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MAP_SORT, order);
        String id = "1";
        db.update(TABLE_SETTINGS, values, KEY_SETTINGS_ID + " = ?", new String[]{id});
        //db.close(); // 5-21-21
    }

    public String getMapSort() {
        // How to sort the MainActivity imported maps
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String order = "null";
            String selectQuery = "SELECT " + KEY_MAP_SORT + " FROM " + TABLE_SETTINGS;
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor == null){
                // Settings table does not exist. Create it.
                createSettingsTable(db);
                return "date";
            }
            else {
                // Only one record
                if (cursor.moveToFirst()) {
                    order = cursor.getString(0);
                }
                cursor.close();
                //db.close(); // 5-21-21
                return order;
            }
        }
        catch (SQLException e){
            // Error reading the database Settings table. Re-create the table
            createSettingsTable(db);
            return "date"; // default value
        }
    }
}