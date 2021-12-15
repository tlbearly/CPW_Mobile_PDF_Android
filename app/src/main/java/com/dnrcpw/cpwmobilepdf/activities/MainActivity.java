package com.dnrcpw.cpwmobilepdf.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    // Displays list of imported pdf maps and an add more button. When an item is clicked, it loads the map.
    private ListView lv;
    private Boolean debug=true;
    private CustomAdapter myAdapter; // list of imported pdf maps
    private DBHandler dbHandler;
    //private String TAG = "MainActivity";
    boolean sortFlag = true;
    Toolbar toolbar;
    Integer selectedId;
    static final int MY_PERMISSIONS_LOCATION = 0;
   /* private final int IMPORT_REQUEST_CODE = 1;
    private final int RENAME_REQUEST_CODE = 2;
    private final int DELETE_REQUEST_CODE = 3;*/

    // location variables
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    double latNow, latBefore = 0.0;
    double longNow, longBefore = 0.0;
    double updateProximityDist = 160.9344; // default change in distance that triggers updating proximity .1 miles
    Spinner sortByDropdown;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // added to report non sdk apis 12/13/21
        if (debug) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DEBUG ***********
        //latBefore = 38.5;
        //longBefore = -105.0;


        // top menu with ... button
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // FILL SORT BY OPTIONS
        sortByDropdown = findViewById(R.id.sortBy);
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        // sortByItems is an array defined in res/values/strings.xml
        // width is set in res/layout/spinner_dropdown_item.xml
        ArrayAdapter<CharSequence> sortByAdapter = ArrayAdapter.createFromResource(this, R.array.sortByItems,
                R.layout.spinner_dropdown_item);
        //set the sortBy adapter to the previously created one.
        sortByDropdown.setAdapter(sortByAdapter);
        // set on click functions: onItemSelected and nothingSelected (must have these names)
        sortByDropdown.setOnItemSelectedListener(this);
        sortFlag = true;

        // Used to keep track of user movement
        latBefore = 0.0;
        longBefore = 0.0;

        // FLOATING ACTION BUTTON CLICK
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GetMoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // SET UP LOCATION SERVICES
        AlertDialog.Builder builder;
        // Ask for location permissions
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)) {
            // Permission is not granted. Request the permission
            if ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) ||
                    (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION))) {
                builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Location Permission Needed");
                builder.setMessage("Permission to access this device's location is needed to show your current location on the map. Please click ALLOW when asked.")
                        .setPositiveButton("OK", (dialog, id) -> {
                            // User clicked OK button. Hide dialog. Ask again
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSIONS_LOCATION);
                        }).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_LOCATION);
            }
        }

        // Check if location services are turned on
        if (!isLocationEnabled(MainActivity.this)) {
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Notice");
            builder.setMessage("Please turn ON Location Services. This can be done in your phone's Settings. If this is not turned on your current location will not appear on the map.")
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        // User clicked OK button. Hide dialog.
                    }).show();
        }
        else
            setupLocation();
    }

    protected void setupLocation(){
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    float[] results = new float[1];
                    latNow = location.getLatitude();
                    longNow = location.getLongitude(); // make it positive

                    // for debugging ****************
                    //latBefore = latBefore + .5;
                    //longBefore = longBefore -.2;

                    if (myAdapter == null) return;
                    myAdapter.setLocation(location);
                    //bearing = location.getBearing(); // 0-360 degrees 0 at North

                    // if accuracy is worse than 1/10 of a mile do not update distance to map
                    float accuracy = location.getAccuracy();
                    //Log.d(TAG, "onLocationResult: accuracy="+accuracy);
                    if (accuracy > 160.9344) {
                        Toast.makeText(MainActivity.this, "Acquiring location...", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (latBefore != 0.0) {
                        try {
                            Location.distanceBetween(latBefore, longBefore, latNow, longNow, results);
                        } catch (IllegalArgumentException e) {
                            return;
                        }
                    }

                    // if change in location is > .1 miles update distance to map
                    if (results[0] > updateProximityDist || latBefore == 0.0) {
                        // Update distance to map.
                         myAdapter.getDistToMap();

                         String sort = dbHandler.getMapSort();
                         if ((sort.equals("proximity") || sort.equals("proximityrev")) && sortFlag) {
                             if (sort.equals("proximity"))
                                 myAdapter.SortByProximity();
                             else
                                 myAdapter.SortByProximityReverse();
                             myAdapter.notifyDataSetChanged();

                             // Refresh all data in visible table cells
                             for (int i=0; i < myAdapter.pdfMaps.size(); i++) {
                                 View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                 if(v == null)
                                     return;

                                 ImageView img = v.findViewById(R.id.pdfImage);
                                 try {
                                     File imgFile = new File(myAdapter.pdfMaps.get(i- lv.getFirstVisiblePosition()).getThumbnail());
                                     Bitmap myBitmap;
                                     myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                     if (myBitmap != null)
                                         img.setImageBitmap(myBitmap);
                                     else
                                         img.setImageResource(R.drawable.pdf_icon);
                                 }catch (Exception ex){
                                     Toast.makeText(MainActivity.this, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                                     img.setImageResource(R.drawable.pdf_icon);
                                 }

                                 TextView name = v.findViewById(R.id.nameTxt) ;
                                 name.setText(myAdapter.pdfMaps.get(i- lv.getFirstVisiblePosition()).getName());
                                 TextView fileSize = v.findViewById(R.id.fileSizeTxt);
                                 fileSize.setText(myAdapter.pdfMaps.get(i).getFileSize());
                                 TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                 String dist = myAdapter.pdfMaps.get(i- lv.getFirstVisiblePosition()).getDistToMap();
                                 distToMap.setText(dist);
                                 if (dist.equals("")){
                                     v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                 }
                                 else {
                                     v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                 }
                             }
                         }
                         // Refresh only dist to map
                        else if (sortFlag){
                             // Refresh visible table cells
                             for (int i = 0; i < myAdapter.pdfMaps.size(); i++) {
                                 View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                 if (v == null)
                                     return;
                                 TextView distToMap = v.findViewById(R.id.distToMapTxt);
                                 String dist = myAdapter.pdfMaps.get(i).getDistToMap();
                                 distToMap.setText(dist);
                                 if (dist.equals("")) {
                                     v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                 } else {
                                     v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                 }
                             }
                         }
                    }

                    // save current location so we can see how much they moved
                    latBefore = latNow;
                    longBefore = longNow;
                }
            }
        };
    }

    // PERMISSIONS
    // location service enabled?
    public boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (locationMode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }
    // Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AlertDialog.Builder builder;
        if (grantResults.length == 0)return;
        if (requestCode == MY_PERMISSIONS_LOCATION) {// If request is cancelled, the result arrays are empty.
            //if (grantResults.length > 0
            //    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            //Toast.makeText(this,"Permission granted to access location.", Toast.LENGTH_LONG).show();
            //}
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // permission denied (this is the first time "never ask again" is not checked)
                // so ask again explaining the usage of permission
                // shouldShowRequestPermissionRationale will return true
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Location Permission Needed");
                    builder.setMessage("This app will not run without permission to access this device's location. Please click ALLOW when asked.")
                            .setPositiveButton("OK", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                dialog.dismiss();
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_LOCATION);
                            })
                            .setNegativeButton("No, Exit App", (dialogInterface, i) -> {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    finishAndRemoveTask();
                                } else {
                                    finish();
                                }
                            }).create().show();
                }
                // permission is denied (and never ask again is checked) go to Settings or exit
                // shouldShowRequestPermissionRationale will return false
                else {
                    // Ask user to go to setting and manually allow permissions
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Permissions Needed");
                    builder.setMessage("You have denied some needed permissions. Go to Settings, click on Permissions and allow all permissions.  Then restart this app.")
                            .setPositiveButton("Yes, Go to Settings", (dialog, id) -> {
                                // User clicked OK button. Hide dialog. Ask again
                                dialog.dismiss();
                                // Go to app settings
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                if (Build.VERSION.SDK_INT >= 21) {
                                    finishAndRemoveTask();
                                } else {
                                    finish();
                                }
                            })
                            .setNegativeButton("No, Exit App", (dialog, i) -> {
                                dialog.dismiss();
                                if (Build.VERSION.SDK_INT >= 21) {
                                    finishAndRemoveTask();
                                } else {
                                    finish();
                                }
                            }).create().show();
                }
            }
            else
                setupLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Importing a Map hides this button, show it again
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fillList(); // get maps list from database
        // Start Location Services
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            startLocationUpdates();
        }
        else Toast.makeText(MainActivity.this,"Please turn on Location Services.",Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        //Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT).show();
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            stopLocationUpdates();
        }
        dbHandler.close();
    }

    @Override
    protected void onStop(){
        super.onStop();

        // try to free memory leaks. Did not seem to help!!!!!!
        // Unregister mLocationCallback
        // unregister  dialogClickListener !!!!!!!!!!!
    }



    //--------------------
    // Database Calls
    //--------------------
    private void fillList() {
        // GET THE LIST FROM THE DATABASE
        // TODO: Start the new thread to read maps from database
        //this did not work!!!!!
        // new Thread(new Runnable() {
        //    public void run() {
        //Looper.prepare();
        //dbHandler = DBHandler.getInstance(MainActivity.this);
        dbHandler = new DBHandler(MainActivity.this);
        try {
            myAdapter = new CustomAdapter(MainActivity.this, dbHandler.getAllMaps(MainActivity.this));
        } catch (SQLException e) {
            Toast.makeText(MainActivity.this, "Error reading database table: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        lv = findViewById(R.id.lv);
        lv.setAdapter(myAdapter);
        registerForContextMenu(lv); // set up edit/trash context menu
        // Make sure all the maps in the database still exist
        myAdapter.checkIfExists();
        // Display note if no records found
        showHideNoImportsMessage();


        // set selected sort by item
        String sort = dbHandler.getMapSort();
        int sortID = 0;
        switch (sort) {
            case "namerev":
                sortID = 1;
                break;
            case "date":
                sortID = 2;
                break;
            case "daterev":
                sortID = 3;
                break;
            case "size":
                sortID = 4;
                break;
            case "sizerev":
                sortID = 5;
                break;
            case "proximity":
                sortID = 6;
                break;
            case "proximityrev":
                sortID = 7;
                break;
        }
        sortByDropdown.setSelection(sortID, true);
        // Update myAdapter list and database if import/rename/delete happened
        checkForActivityResult();
        // check of returned from another activity and change the maps list accordingly
        // When return from GetMoreActivity or EditMapNameActivity update maps list
    }

    private void checkForActivityResult() {
        // GetMoreActivity gets map to import
        // EditWayPointActivity renames or deletes a map
        // They return extras to pass back the results, update adapter and database here
        final Intent i = MainActivity.this.getIntent();
        if (i.getExtras() != null && !i.getExtras().isEmpty()) {

            // IMPORT NEW MAP INTO LIST
            // CustomAdapter calls MapImportTask class in CustomAdapter.java
            if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED
                if (import_map) {
                    sortFlag = false; // hold off on sorting.
                    FloatingActionButton fab = findViewById(R.id.fab);
                    fab.setVisibility(View.GONE);
                    // Scroll down to last item. The one just added.
                    // String name = new File(i.getExtras().getString("PATH")).getName();
                    // int pos = myAdapter.findName();
                    int pos = myAdapter.getCount() - 1;
                    if (pos > -1) lv.setSelection(pos);
                    //Toast.makeText(MainActivity.this, "Map imported: "+i.getExtras().getString("PATH"), Toast.LENGTH_LONG).show();
                }
                i.removeExtra("PATH");
                i.removeExtra("IMPORT_MAP");
            }

            // RENAME MAP
            else if (i.getExtras().containsKey("RENAME") && i.getExtras().containsKey("NAME") && i.getExtras().containsKey("ID")) {
                // Renamed map, update with new name
                String name = i.getExtras().getString("NAME");
                int id = i.getExtras().getInt("ID");
                myAdapter.rename(id, name);
                //Toast.makeText(MainActivity.this, "Map renamed to: " + name, Toast.LENGTH_LONG).show();
                i.removeExtra("NAME");
                i.removeExtra("ID");
                i.removeExtra("RENAME");
            }

            // DELETE MAP
            else if (i.getExtras().containsKey("DELETE") && i.getExtras().containsKey("ID")) {
                // Delete map, remove from Imported Maps list, delete from database
                selectedId = i.getExtras().getInt("ID");
                myAdapter.removeItem(selectedId);
                Toast.makeText(MainActivity.this, "Map removed", Toast.LENGTH_LONG).show();
                i.removeExtra("ID");
                i.removeExtra("DELETE");
                // Display note if no records found
                showHideNoImportsMessage();
            }
        }
    }


    //--------------------
    // Location Functions
    //--------------------

    //  LOCATION UPDATES
    private void startLocationUpdates() {
        LocationRequest mLocationRequest;
        mLocationRequest = LocationRequest.create();//new LocationRequest();
        mLocationRequest.setInterval(30000); //update location every 30 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Was handled in StartActivity
            Toast.makeText(MainActivity.this,"Fine Location Services are off.",Toast.LENGTH_LONG).show();
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /*Looper.getMainLooper()*/);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    // ...................
    //   Sort By DropDown
    // ...................
    private  void sortMaps(String sortBy) {
        if (myAdapter == null) return;
        switch (sortBy) {
            case "name":
                // Sort by Name
                myAdapter.SortByName();
                lv.setAdapter(myAdapter);
                try {
                    // save user sort preference in database
                    dbHandler.setMapSort("name");
                 }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "namerev":
                // Sort by Name Reverse
                myAdapter.SortByNameReverse();
                lv.setAdapter(myAdapter);
                try {
                    // save user sort preference in database
                    dbHandler.setMapSort("namerev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "date":
                // Sort by Date
                myAdapter.SortByDate();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("date");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "daterev":
                // Sort by Date Reverse
                myAdapter.SortByDateReverse();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("daterev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "size":
                // Sort by Size
                myAdapter.SortBySize();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("size");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "sizerev":
                // Sort by Size Reverse
                myAdapter.SortBySizeReverse();
                lv.setAdapter(myAdapter);
                try {
                    dbHandler.setMapSort("sizerev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "proximity":
                // Sort by Proximity
                //lv.getFirstVisiblePosition(); // get current top position
                myAdapter.SortByProximity();
                lv.setAdapter(myAdapter); // scrolls to the top
                try {
                    dbHandler.setMapSort("proximity");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "proximityrev":
                // Sort by Proximity Reverse
                //lv.getFirstVisiblePosition(); // get current top position
                myAdapter.SortByProximityReverse();
                lv.setAdapter(myAdapter); // scrolls to the top
                try {
                    dbHandler.setMapSort("proximityrev");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            default:
                Toast.makeText(getApplicationContext(), "Sort method not found: "+sortBy, Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) throws IllegalStateException {
        // sort by dropdown required callback
        if (!sortFlag) return;
        switch (position) {
            case 0:
                // Sort by Name
                sortMaps("name");
                break;
            case 1:
                sortMaps("namerev");
                break;
            case 2:
                // Sort by Date
                sortMaps("date");
                break;
            case 3:
                sortMaps("daterev");
                break;
            case 4:
                // Sort by Size
                sortMaps("size");
                break;
            case 5:
                sortMaps("sizerev");
                break;
            case 6:
                // Sort by Proximity
                sortMaps("proximity");
                break;
            case 7:
                sortMaps("proximityrev");
                break;
            default:
                Toast.makeText(getApplicationContext(), "Sort method not found: "+position, Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Sort by dropdown required callback
        // Do not remove!!!!
    }

    // Show or Hide no maps imported message
    private void showHideNoImportsMessage(){
        TextView msg = findViewById(R.id.txtMessage);
        TextView sortTitle = findViewById(R.id.sortTitle);
        Spinner sortBy = findViewById(R.id.sortBy);
        if (myAdapter.pdfMaps.size() == 0){
            msg.setVisibility(View.VISIBLE);
            sortTitle.setVisibility(View.GONE);
            sortBy.setVisibility(View.GONE);
            //msg.setText("No maps have been imported.\nUse the + button to import a map.");
        }
        else {
            msg.setVisibility(View.GONE);
            sortTitle.setVisibility(View.VISIBLE);
            sortBy.setVisibility(View.VISIBLE);
        }
    }

    // ...................
    //     ... MENU
    // ...................
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //DELETE all imported maps clicked and recreate the database *****
                    //dbHandler.deleteTable(MainActivity.this); // this removes the database table. Do this if added/removed fields to/from the database
                    myAdapter.removeAll();
                    // Disable "Delete all Imported Maps" if there aren't any maps
                    MenuItem delMapsMenuItem = toolbar.getMenu().findItem(R.id.action_deleteAll);
                    if (myAdapter.pdfMaps.size() == 0) {
                        delMapsMenuItem.setEnabled(false);
                    }
                    else{
                        delMapsMenuItem.setEnabled(true);
                    }
                    // Display note if no records found
                    showHideNoImportsMessage();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //CANCEL button clicked
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Disable "Delete all Imported Maps" if there aren't any maps
        MenuItem delMapsMenuItem = toolbar.getMenu().findItem(R.id.action_deleteAll);
        if (myAdapter.pdfMaps.size() == 0) {
            delMapsMenuItem.setEnabled(false);
        }
        else{
            delMapsMenuItem.setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Found in res/menu/menu_main.xml
        int id = item.getItemId();

        // Delete all imported maps
        if (id == R.id.action_deleteAll){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete all imported maps?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL",dialogClickListener).show();
            return true;
        }
        else if (id == R.id.action_help){
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}