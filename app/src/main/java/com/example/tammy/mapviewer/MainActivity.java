package com.example.tammy.mapviewer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
// Displays list of imported pdf maps and an add more button. When an item is clicked, it loads the map.
    private ListView lv;
    private CustomAdapter myAdapter; // list of imported pdf maps
    private DBHandler db;
    private String TAG = "MainActivity";
    boolean sortFlag=true;
    Toolbar toolbar;
    Integer selectedId;

    // location variables
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    double latNow;
    double longNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DBHandler.getInstance(MainActivity.this);
        // top menu with ... button
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // GET THE LIST FROM THE DATABASE
        myAdapter = new CustomAdapter(MainActivity.this, db.getAllMaps());
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(myAdapter);
        registerForContextMenu(lv); // set up edit/trash context menu

        // Make sure all the maps in the database still exist
        myAdapter.checkIfExists();

        // Display note if no records found
        if (myAdapter.pdfMaps.size() == 0){
            TextView msg = (TextView) findViewById(R.id.txtMessage);
            msg.setText("No maps have been imported.\nUse the + button to import a map.");
        }

        // IMPORT NEW MAP INTO LIST
        // CustomAdapter calls MapImportTask class in CustomAdapter.java
        final Intent i = this.getIntent();
        if (i.getExtras()!=null && !i.getExtras().isEmpty()) {
            if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                Boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED
                if (import_map) {
                    sortFlag=false; // hold off on sorting.
                    // Scroll down to last item. The one just added.
                   // String name = new File(i.getExtras().getString("PATH")).getName();
                   // int pos = myAdapter.findName();
                    int pos = myAdapter.getCount()-1;
                    if (pos > -1) lv.setSelection(pos);
                }
                i.removeExtra("PATH");
                i.removeExtra("IMPORT_MAP");
            }
            // RENAME MAP
            else if (i.getExtras().containsKey("RENAME") && i.getExtras().containsKey("NAME") && i.getExtras().containsKey("ID")) {
                // Renamed map, update with new name
                String name = i.getExtras().getString("NAME");
                Integer id = i.getExtras().getInt("ID");
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
                if (myAdapter.pdfMaps.size() == 0){
                    TextView msg = (TextView) findViewById(R.id.txtMessage);
                    msg.setText("No maps have been imported.\nUse the + button to import a map.");
                }
            }
        }

        // FLOATING ACTION BUTTON CLICK
         FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
         fab.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent i=new Intent(MainActivity.this,GetMoreActivity.class);
                 i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                 startActivity(i);
            }
        });

        // SET UP LOCATION SERVICES
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data

                    latNow = location.getLatitude();
                    longNow = location.getLongitude(); // make it positive
                    //bearing = location.getBearing(); // 0-360 degrees 0 at North
                    //accuracy = location.getAccuracy();
                    updateDistToMap();
                }
            }
        };
    }

    @Override
    protected void onResume(){
        super.onResume();
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            stopLocationUpdates();
        }
    }



    // Location Functions

    //  LOCATION UPDATES
    private void startLocationUpdates() {
        LocationRequest mLocationRequest;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //update location every 1 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /*looper*/);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void updateDistToMap() {
        for (int i=0; i<myAdapter.getCount(); i++) {
            PDFMap map = myAdapter.pdfMaps.get(i);
            //View cell = lv.getChildAt(i);
            View cell = lv.getChildAt(i - lv.getFirstVisiblePosition());

            if (cell == null || map == null || map.getBounds() == null)
                continue;
            String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
            if (bounds == null || bounds.length() == 0) continue; // it will be 0 length if is importing
            bounds = bounds.trim(); // remove leading and trailing spaces
            int pos = bounds.indexOf(" ");
            Double lat1 = Double.valueOf(bounds.substring(0, pos));
            bounds = bounds.substring(pos + 1); // strip off 'lat1 '
            pos = bounds.indexOf(" ");
            Double long1 = Double.valueOf(bounds.substring(0, pos));
            // FIND LAT2
            bounds = bounds.substring(pos + 1); // strip off 'long1 '
            pos = bounds.indexOf(" ");
            Double lat2 = Double.valueOf(bounds.substring(0, pos));
            // FIND LONG2
            pos = bounds.lastIndexOf(" ");
            Double long2 = Double.valueOf(bounds.substring(pos + 1));

            // Get text field to display distance
            TextView distToMapText = (TextView) cell.findViewById(R.id.distToMapTxt);
            ImageView locIcon = cell.findViewById(R.id.locationIcon);

            // Is on map?
            if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2){
                locIcon.setVisibility(View.VISIBLE);
                distToMapText.setText("on map");
            }
            else {
                locIcon.setVisibility(View.GONE);
                String direction = "";
                Double dist = 0.0;
                if (latNow > lat1) direction = "S";
                else if (latNow > lat2) direction = "";
                else direction = "N";
                if (longNow < long1) direction += "E";
                else if (longNow > long2) direction += "W";

                switch (direction) {
                    case "S":
                        dist = distance_on_unit_sphere(latNow, longNow, lat2, longNow);
                        break;
                    case "N":
                        dist = distance_on_unit_sphere(latNow, longNow, lat1, longNow);
                        break;
                    case "E":
                        dist = distance_on_unit_sphere(latNow, longNow, latNow, long2);
                        break;
                    case "W":
                        dist = distance_on_unit_sphere(latNow, longNow, latNow, long1);
                        break;
                    case "SE":
                        dist = distance_on_unit_sphere(latNow, longNow, lat2, long2);
                        break;
                    case "SW":
                        dist = distance_on_unit_sphere(latNow, longNow, lat1, long2);
                        break;
                    case "NE":
                        dist = distance_on_unit_sphere(latNow, longNow, lat2, long1);
                        break;
                    case "NW":
                        dist = distance_on_unit_sphere(latNow, longNow, lat1, long1);
                        break;
                }
                String distStr = String.format("%.1f", dist);

                distToMapText.setText("    " + distStr + " mi " + direction);
            }
        }
    }

    private Double distance_on_unit_sphere(Double lat1, Double long1, Double lat2, Double long2){
        // Convert latitude and longitude to
        // spherical coordinates in radians.
        Double degrees_to_radians = Math.PI/180.0;

        // phi = 90 - latitude
        Double phi1 = (90.0 - lat1)*degrees_to_radians;
        Double phi2 = (90.0 - lat2)*degrees_to_radians;

        // theta = longitude
        Double theta1 = long1*degrees_to_radians;
        Double theta2 = long2*degrees_to_radians;

        // Compute spherical distance from spherical coordinates.
        // For two locations in spherical coordinates
        // (1, theta, phi) and (1, theta, phi)
        // cosine( arc length ) =
        //    sin phi sin phi' cos(theta-theta') + cos phi cos phi'
        // distance = rho * arc length
        Double cosine = (Math.sin(phi1) * Math.sin(phi2) * Math.cos(theta1 - theta2) + Math.cos(phi1) * Math.cos(phi2));
        Double arc = Math.acos( cosine );

        // Remember to multiply arc by the radius of the earth
        // in your favorite set of units to get length.
        return arc * 3963; // 3,962 is the radius of earth in miles
    }

    // ...................
    //     ... MENU
    // ...................
    MenuItem nameItem;
    MenuItem dateItem;
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //DELETE all imported maps clicked and recreate the database *****
                    //db.deleteTable(MainActivity.this); // this removes the database table. Do this if added/removed fields to/from the database
                    myAdapter.removeAll();
                    // Display note if no records found
                    if (myAdapter.pdfMaps.size() == 0){
                        TextView msg = (TextView) findViewById(R.id.txtMessage);
                        msg.setText("No maps have been imported.\nUse the + button to import a map.");
                    }
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
        nameItem = menu.findItem(R.id.action_sort_by_name);
        dateItem = menu.findItem(R.id.action_sort_by_date);
        // read Settings table in the database and get user preferences
        String sort = db.getMapSort();
        if (sortFlag && sort.equals("name")){
            myAdapter.SortByName();
            lv.setAdapter(myAdapter);
            nameItem.setChecked(true);
            dateItem.setChecked(false);
        }
        else if (sortFlag && sort.equals("date")){
            myAdapter.SortByDate();
            lv.setAdapter(myAdapter);
            dateItem.setChecked(true);
            nameItem.setChecked(false);
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
        // Sort map list by name
        else if (id == R.id.action_sort_by_name){
            myAdapter.SortByName();
            lv.setAdapter(myAdapter);
            item.setChecked(true);
            dateItem.setChecked(false);
            try {
                db.setMapSort("name");
            }
            catch (SQLException e){
                Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        // Sort map list by date
        else if (id == R.id.action_sort_by_date){
            myAdapter.SortByDate();
            lv.setAdapter(myAdapter);
            item.setChecked(true);
            nameItem.setChecked(false);
            try {
                db.setMapSort("date");
            }
            catch (SQLException e){
                Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        // This is not visible in main activity
        else if (id == R.id.action_open){
            lv.setAdapter(new CustomAdapter(MainActivity.this,db.getAllMaps()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}