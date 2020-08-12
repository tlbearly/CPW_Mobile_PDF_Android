package com.example.tammy.mapviewer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private CustomAdapter myAdapter; // list of imported pdf maps
    private DBHandler db;
    private String TAG = "MainActivity";
    boolean sortFlag = true;
    Toolbar toolbar;
    Integer selectedId;
   /* private final int IMPORT_REQUEST_CODE = 1;
    private final int RENAME_REQUEST_CODE = 2;
    private final int DELETE_REQUEST_CODE = 3;*/

    // location variables
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    double latNow, latBefore=0.0;
    double longNow, longBefore=0.0;
    double updateProximityDist=160.9344; // default change in distance that triggers updating proximity .1 miles
    Spinner sortByDropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(MainActivity.this, "onCreate", Toast.LENGTH_SHORT).show();


        // DEBUG ***********
        //latBefore = 38.5;
        //longBefore = -105.0;



        setContentView(R.layout.activity_main);
        db = DBHandler.getInstance(MainActivity.this);
        // top menu with ... button
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Fill Sort By Options
        sortByDropdown = findViewById(R.id.sortBy);
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        // sortByItems is an array defined in res/values/strings.xml
        ArrayAdapter<CharSequence> sortByAdapter = ArrayAdapter.createFromResource(this, R.array.sortByItems,
                R.layout.spinner_dropdown_item);
        //set the sortBy adapter to the previously created one.
        sortByDropdown.setAdapter(sortByAdapter);
        // set on click functions: onItemSelected and nothingSelected (must have these names)
        sortByDropdown.setOnItemSelectedListener(this);

        // GET THE LIST FROM THE DATABASE
        try {
            myAdapter = new CustomAdapter(MainActivity.this, db.getAllMaps(MainActivity.this));
        } catch (SQLException e) {
            Toast.makeText(MainActivity.this, "Error deleting database table: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(myAdapter);
        registerForContextMenu(lv); // set up edit/trash context menu

        // Make sure all the maps in the database still exist
        myAdapter.checkIfExists();

        // Display note if no records found
        showHideNoImportsMessage();
        /*if (myAdapter.pdfMaps.size() == 0) {
            TextView msg = (TextView) findViewById(R.id.txtMessage);
            msg.setText("No maps have been imported.\nUse the + button to import a map.");
        }*/

        // Used to keep track of user movement
        latBefore = 0.0;
        longBefore = 0.0;

        // IMPORT NEW MAP INTO LIST
        // CustomAdapter calls MapImportTask class in CustomAdapter.java
        final Intent i = this.getIntent();
        if (i.getExtras() != null && !i.getExtras().isEmpty()) {
            if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                Boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED
                if (import_map) {
                    sortFlag = false; // hold off on sorting.
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
                showHideNoImportsMessage();
                /*if (myAdapter.pdfMaps.size() == 0) {
                    TextView msg = (TextView) findViewById(R.id.txtMessage);
                    msg.setText("No maps have been imported.\nUse the + button to import a map.");
                }*/
            }
        }

        if (sortFlag){
            // set selected sort by item
            String sort = db.getMapSort();
            int sortID = 0;
            if (sort.equals("date")) sortID = 1;
            else if (sort.equals("size")) sortID = 2;
            else if (sort.equals("proximity")) sortID = 3;
            sortByDropdown.setSelection(sortID, true);
        }

        // FLOATING ACTION BUTTON CLICK
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GetMoreActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
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
                    float[] results = new float[1];
                    latNow = location.getLatitude();
                    longNow = location.getLongitude(); // make it positive



                    // for debugging ****************
                    latBefore = latBefore + .5;
                    longBefore = longBefore -.2;




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

                         String sort = db.getMapSort();
                         if (sort.equals("proximity") && sortFlag) {
                             myAdapter.SortByProximity();
                             //myAdapter.notifyDataSetChanged();


                             // Refresh all data in visible table cells
                             for (int i=0; i < myAdapter.pdfMaps.size(); i++) {
                                 View v = lv.getChildAt(i - lv.getFirstVisiblePosition());
                                 if(v == null)
                                     return;

                                 ImageView img = v.findViewById(R.id.pdfImage);
                                 try {
                                     File imgFile = new File(myAdapter.pdfMaps.get(i- lv.getFirstVisiblePosition()).getThumbnail());
                                     Bitmap myBitmap = null;
                                     myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                     if (myBitmap != null)
                                         img.setImageBitmap(myBitmap);
                                     else
                                         img.setImageResource(R.drawable.pdf_icon);
                                 }catch (Exception ex){
                                     Toast.makeText(MainActivity.this, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                                     img.setImageResource(R.drawable.pdf_icon);
                                 }

                                 TextView name = (TextView) v.findViewById(R.id.nameTxt) ;
                                 name.setText(myAdapter.pdfMaps.get(i- lv.getFirstVisiblePosition()).getName());
                                 TextView fileSize = (TextView) v.findViewById(R.id.fileSizeTxt);
                                 fileSize.setText(myAdapter.pdfMaps.get(i).getFileSize());
                                 TextView distToMap = (TextView) v.findViewById(R.id.distToMapTxt);
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
                                 TextView distToMap = (TextView) v.findViewById(R.id.distToMapTxt);
                                 String dist = myAdapter.pdfMaps.get(i).getDistToMap();
                                 distToMap.setText(dist);
                                 if (dist.equals("")) {
                                     v.findViewById(R.id.locationIcon).setVisibility(View.VISIBLE);
                                 } else {
                                     v.findViewById(R.id.locationIcon).setVisibility(View.GONE);
                                 }
                             }
                         }
                        else sortFlag = true;
                    }

                    // save current location so we can see how much they moved
                    latBefore = latNow;
                    longBefore = longNow;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            startLocationUpdates();
        }
        //Toast.makeText(MainActivity.this, "onResume", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            stopLocationUpdates();
        }
        //Toast.makeText(MainActivity.this, "onPause", Toast.LENGTH_SHORT).show();
    }

    //--------------------
    // Location Functions
    //--------------------

    //  LOCATION UPDATES
    private void startLocationUpdates() {
        LocationRequest mLocationRequest;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000); //update location every 30 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Was handled in StartActivity
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /*looper*/);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    // ...................
    //   Sort By DropDown
    // ...................
    private  void sortMaps(String sortBy) {
        switch (sortBy) {
            case "name":
                // Sort by Name
                myAdapter.SortByName();
                lv.setAdapter(myAdapter);
                try {
                    // save user sort preference in db
                    db.setMapSort("name");
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
                    db.setMapSort("date");
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
                    db.setMapSort("size");
                }
                catch (SQLException e){
                    Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            case "proximity":
                // Sort by Proximity
                int pos = lv.getFirstVisiblePosition(); // get current top position
                myAdapter.SortByProximity();
                lv.setAdapter(myAdapter); // scrolls to the top
                try {
                    db.setMapSort("proximity");
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
        switch (position) {
            case 0:
                // Sort by Name
                sortMaps("name");
                break;
            case 1:
                // Sort by Date
                sortMaps("date");
                break;
            case 2:
                // Sort by Size
                sortMaps("size");
                break;
            case 3:
                // Sort by Proximity
                sortMaps("proximity");
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
        TextView msg = (TextView) findViewById(R.id.txtMessage);
        TextView sortTitle = (TextView) findViewById(R.id.sortTitle);
        Spinner sortBy = (Spinner) findViewById(R.id.sortBy);
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
      /*  nameItem = menu.findItem(R.id.action_sort_by_name);
        dateItem = menu.findItem(R.id.action_sort_by_date);
        // read Settings table in the database and get user preferences
        String sort = db.getMapSort();
        if (sortFlag && sort.equals("name")){
            myAdapter.SortByName();
            lv.setAdapter(myAdapter);
            nameItem.setChecked(true);
            dateItem.setChecked(false);
            Log.i(TAG, "******onCreateOptionsMenu: sort by name");
        }
        else if (sortFlag && sort.equals("date")){
            myAdapter.SortByDate();
            lv.setAdapter(myAdapter);
            dateItem.setChecked(true);
            nameItem.setChecked(false);
            Log.i(TAG, "******onCreateOptionsMenu: sort by date");
        }
       */
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
       /* else if (id == R.id.action_sort_by_name){
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
        }*/
        // This is not visible in main activity
        /*else if (id == R.id.action_open){
            lv.setAdapter(new CustomAdapter(MainActivity.this,db.getAllMaps(MainActivity.this)));
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
}