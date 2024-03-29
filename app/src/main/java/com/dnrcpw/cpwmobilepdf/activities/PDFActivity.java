package com.dnrcpw.cpwmobilepdf.activities;

import static android.graphics.Color.argb;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.SQLException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/* show the map */
public class PDFActivity extends AppCompatActivity implements SensorEventListener {
    boolean debug = false;
    PDFView pdfView;
    Menu mapMenu;
    // Color and style of current location point
    Paint cyan;
    Paint cyanTrans;
    Paint red;
    Paint green; // debug
    Paint purple; // debug
    Paint white;
    Paint black;
    Paint outline;
    Paint blue;
    boolean addWayPtFlag;
    // current screen location adjusted by zoom level
    double currentLocationX; // start offscreen
    double currentLocationY;
    double latNow;
    double longNow;
    float accuracy;
    float bearing;

    double mediaBoxX1;
    double mediaBoxY1;
    double mediaBoxX2;
    double mediaBoxY2;

    double bBoxX1;
    double bBoxY1;
    double bBoxX2;
    double bBoxY2;

    double marginTop;
    double marginBottom;
    double marginLeft;
    double marginRight;

    double mediaBoxWidth;
    double mediaBoxHeight;
    double marginXworld; // marginLeft+marginRight
    double marginYworld; // marginTop+marginBottom

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    //private int count=0;
    private double lat1;
    private double long1;
    private double lat2;
    private double long2;
    private double latDiff;
    private double longDiff;

    // Screen Sensor X rotation
    int adjust; // adjust for landscape or portrait. Sensor reports north for top of portrait screen. For landscape add 90 degrees.
    private boolean landscape;
    private boolean landscapeLocked;
    private boolean portraitLocked;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer;
    private float[] mLastMagnetometer;
    private boolean mLastAccelerometerSet;
    private boolean mLastMagnetometerSet;
    private float[] mR;
    private float[] mOrientation;
    //private float mCurrentDegree = 0f;
    private WayPts wayPts;
    private String mapName;
    private DBWayPtHandler db;
    //private DBHandler db2;
    private Boolean markCurrent;
    private int clickedWP; // index of waypoint that was clicked on
    private int lastClickedWP;
    private Boolean newWP; // if added a new waypoint show balloon too
    private TextPaint txtCol; // text color for waypoint balloon popup
    private int txtSize; // text size of waypoint balloon popup
    private int marg; // text margins in waypoint balloon popup
    private int boxHt; // height of waypoint balloon popup
    private float pin_radius; // radius of waypoint pin
    private float pin_ht; // length of pin stem from point to center of pin head (circle)
    private int startY; // bottom of the waypoint balloon popup
    private int margX; // distance on each side of waypoint to register user click
    private int margTop; // distance above waypoint to register user click
    private int margBottom; // distance below waypoint to register user click
    private boolean clickedTopHalf; // user clicked in top half of screen
    private int screenWidth; // Used to see if popup balloon goes off page to the left or right
    private int screenHeight; // Used to get clickedTopHalf
    private StaticLayout lsLayout; // arrow right in waypoint balloon popup
    private String path;
    String bounds;
    private String strBounds;
    String viewPort;
    private String strViewPort;
    String mediaBox;
    private String strMediaBox;
    private RelativeLayout wait; // indeterminate progress bar
    private Boolean showAllWayPtLabels = false;
    private Boolean showAllWayPts = true;
    AtomicReference<Double> optimalPageWidth = new AtomicReference<>((double) 0);
    AtomicReference<Double> optimalPageHeight = new AtomicReference<>((double) 0);
    MenuItem wayPtMenuItem;
    Double[] LatLong;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // final RelativeLayout wait; // indeterminate progress bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        wait = findViewById(R.id.loadingPanel);
        wait.setVisibility(View.VISIBLE);

        addWayPtFlag=false;

        // current screen location adjusted by zoom level
        currentLocationX = 0; // start offscreen
        currentLocationY = 0;

        landscape = false;
        landscapeLocked = false;
        portraitLocked = false;
        mLastAccelerometer = new float[3];
        mLastMagnetometer = new float[3];
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mR = new float[9];
        mOrientation = new float[3];
        //mCurrentDegree = 0f;
        wayPts = null;
        mapName = "";
        db = new DBWayPtHandler(this);
        markCurrent = false;
        clickedWP = -1; // index of waypoint that was clicked on
        lastClickedWP = -1;
        newWP = false; // if added a new waypoint show balloon too
        txtCol = new TextPaint(); // text color for waypoint balloon popup
        txtSize = Math.round(getResources().getDimension(R.dimen.balloon_txt_size)); // text size of waypoint balloon popup ( used to be 30 pixels)
        marg = Math.round(getResources().getDimension(R.dimen.balloon_margin)); // text margins in waypoint balloon popup used to be 20pixels
        boxHt = txtSize + (marg * 2); // height of waypoint balloon popup
        pin_radius = getResources().getDimension(R.dimen.pin_radius);
        pin_ht = getResources().getDimension(R.dimen.pin_height); // length of pin stem from point to center of pin head (circle)
        startY = Math.round(pin_ht + pin_radius) + 12; // bottom of the waypoint balloon popup
        margX = Math.round(getResources().getDimension(R.dimen.wayPtXmarg));
        margTop = Math.round(getResources().getDimension(R.dimen.wayPtTmarg));
        margBottom = Math.round(getResources().getDimension(R.dimen.wayPtBmarg));

        // Get variables to check for user click in top or bottom half of screen
        clickedTopHalf = false;
        if (Build.VERSION.SDK_INT >= 28 && Build.VERSION.SDK_INT < 30) {
            screenHeight = getResources().getDisplayMetrics().heightPixels;
            screenWidth = getResources().getDisplayMetrics().widthPixels;

        } else if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics deviceWindowMetrics = getApplicationContext().getSystemService(WindowManager.class).getMaximumWindowMetrics();
            screenWidth = deviceWindowMetrics.getBounds().width();
            screenHeight = deviceWindowMetrics.getBounds().height();

        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }

        // keep app from timing out and going to screen saver
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // UNPACK OUR DATA FROM INTENT
        path = null;
        bounds = null;
        viewPort = null;
        mediaBox = null;

        Intent i = this.getIntent();

        //  GET LAT LONG BOUNDS, CONVERT FROM STRING "LAT1 LONG1 LAT2 LONG1 LAT2 LONG2 LAT1 LONG2" TO FLOATS
        try {
            // Check that values were passed
            if (i.getExtras() == null) {
                Toast.makeText(PDFActivity.this, "Can't display map, no map specifications were found.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            path = i.getExtras().getString("PATH");

            // Display Map Name
            mapName = i.getExtras().getString("NAME");
            this.setTitle(mapName);
            wayPts = db.getWayPts(mapName);
            wayPts.SortPts();

            // GET LAT/LONG
            try {
                bounds = i.getExtras().getString("BOUNDS"); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
            }catch (NullPointerException e){
                Toast.makeText(PDFActivity.this,"Could not read page lat/long.",Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            assert bounds != null;
            bounds = bounds.trim(); // remove leading and trailing spaces
            strBounds = bounds;

            //Toast.makeText(PDFActivity.this, bounds, Toast.LENGTH_LONG).show();
            // Get Latitude, Longitude bounds.
            // lat1 and long1 are the smallest values SW corner
            // lat2 and long2 are the largest NE corner
            String[] arrLatLong = bounds.split(" ");
            // convert strings to double
            LatLong = new Double[arrLatLong.length];
            for (int l=0; l<arrLatLong.length; l++) {
                LatLong[l] = Double.parseDouble(arrLatLong[l]);
            }
            // Find the smallest and largest values
            lat1 = LatLong[0];
            long1 = LatLong[1];
            lat2 = LatLong[0];
            long2 = LatLong[1];
            for (int l=0; l<LatLong.length; l=l+2) {
                if (LatLong[l] < lat1) lat1 = LatLong[l];
                if (LatLong[l] > lat2) lat2 = LatLong[l];
                if (LatLong[l+1] < long1) long1 = LatLong[l+1];
                if (LatLong[l+1] > long2) long2 = LatLong[l+1];
            }
            longDiff = long2 - long1;
            latDiff = lat2 - lat1;
        } catch (AssertionError | Exception ae){
            Toast.makeText(PDFActivity.this, "Trouble reading lat/long from Geo PDF. Read: " + bounds, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            // GET MEDIA BOX or PAGE BOUNDARIES for example: "0 0 612 792"
            try {
                mediaBox = Objects.requireNonNull(i.getExtras()).getString("MEDIABOX");
            } catch (NullPointerException e) {
                Toast.makeText(PDFActivity.this, "Could not read page size.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            assert mediaBox != null;
            mediaBox = mediaBox.trim(); // remove leading and trailing spaces
            strMediaBox = mediaBox;
            int pos = mediaBox.indexOf(" ");
            // FIND X1
            mediaBoxX1 = Double.parseDouble(mediaBox.substring(0, pos));
            // FIND Y1
            mediaBox = mediaBox.substring(pos + 1); // strip off 'X1 '
            pos = mediaBox.indexOf(" ");
            mediaBoxY1 = Double.parseDouble(mediaBox.substring(0, pos));
            // FIND X2
            mediaBox = mediaBox.substring(pos + 1); // strip off 'Y1 '
            pos = mediaBox.indexOf(" ");
            mediaBoxX2 = Double.parseDouble(mediaBox.substring(0, pos));
            // FIND Y2
            mediaBox = mediaBox.substring(pos + 1); // strip off 'X2 '
            pos = mediaBox.indexOf(" ");
            mediaBoxY2 = Double.parseDouble(mediaBox.substring(pos + 1));
        } catch (AssertionError | Exception ae) {
            Toast.makeText(PDFActivity.this, "Trouble reading mediaBox page boundaries from Geo PDF. Read: " + mediaBox, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            // GET MARGINS
            // bBox x1,y1 is lower-left
            // bBox x2,y2 is upper-right
            //  - origin at top-left: BBox[23 570 768 48]
            //  - origin at bottom-left: BBox[23 48 768 570] (Melissa's file)
            //  - origin at bottom-right: BBox[768 48 23 570]
            try {
                viewPort = Objects.requireNonNull(i.getExtras()).getString("VIEWPORT");
            } catch (NullPointerException e) {
                Toast.makeText(PDFActivity.this, "Trouble reading viewport from Geo PDF.", Toast.LENGTH_LONG).show();
            }
            assert  viewPort != null;
            viewPort = viewPort.trim();
            strViewPort = viewPort;
            // FIND bBoxX1
            int pos = viewPort.indexOf(" ");
            bBoxX1 = Double.parseDouble(viewPort.substring(0, pos));
            // FIND bBoxY1
            viewPort = viewPort.substring(pos + 1); // strip off 'bBoxX1 '
            pos = viewPort.indexOf(" ");
            bBoxY1 = Double.parseDouble(viewPort.substring(0, pos));
            // FIND bBoxX2
            viewPort = viewPort.substring(pos + 1); // strip off 'bBoxY1 '
            pos = viewPort.indexOf(" ");
            bBoxX2 = Double.parseDouble(viewPort.substring(0, pos));
            // FIND bBoxY2
            viewPort = viewPort.substring(pos + 1); // strip off 'bBoxX2 '
            pos = viewPort.indexOf(" ");
            bBoxY2 = Double.parseDouble(viewPort.substring(pos + 1));

            // 5-17-22 A map made by Melissa in ArcGIS had these switched
            if(bBoxY1 < bBoxY2) {
                marginTop = mediaBoxY2 - bBoxY2;
                marginBottom = bBoxY1;
            }else{
                marginTop = mediaBoxY2 - bBoxY1;
                marginBottom = bBoxY2;
            }
            if (bBoxX1 < bBoxX2){
                marginLeft = bBoxX1;
                marginRight = mediaBoxX2 - bBoxX2;
            }else{
                marginLeft = bBoxX2;
                marginRight = mediaBoxX2 - bBoxX1;
            }

            mediaBoxWidth = mediaBoxX2 - mediaBoxX1;
            mediaBoxHeight = mediaBoxY2 - mediaBoxY1;
            marginXworld = marginLeft + marginRight;
            marginYworld = marginTop + marginBottom;
        } catch (AssertionError | Exception ae) {
            Toast.makeText(PDFActivity.this, "Trouble reading viewPort margins from Geo PDF. Read: " + viewPort, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup Screen Sensor for X Axis rotation (flat)
        try {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } catch (NullPointerException e) {
            Toast.makeText(PDFActivity.this, "Trouble reading phone orientation", Toast.LENGTH_LONG).show();
        }

        //PDFVIEW WILL DISPLAY OUR PDFS
        pdfView = findViewById(R.id.pdfView);

        pdfView.enableAntialiasing(true); // improve rendering a little bit on low-res screens

        // SET UP LOCATION SERVICES
        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        } catch (Exception e){
            // no gps service
            return;
        }

        // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //Log.d("LocationCallback","updating location, refreshing waypoints");
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    //GeomagneticField geoField;

                    latNow = location.getLatitude();
                    longNow = location.getLongitude(); // make it positive
                    //bearing = location.getBearing(); // 0-360 degrees 0 at North
                    accuracy = location.getAccuracy();
                    // Makes top of map (north) off
            /*geoField = new GeomagneticField(
                    Double.valueOf(latNow).floatValue(),
                    Double.valueOf(longNow).floatValue(),
                    Double.valueOf(location.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );*/
                    //bearing += geoField.getDeclination(); // Adjust for declination - difference between magnetic north and true north. Phone returns magnetic north.
                    //bearing -= 90; // Adjust by 90 degrees. Canvas needs 0 at East, this returns 0 at North
                    //if (bearing<0) bearing = 360 + bearing;

                    // debug
                    //TextView bTxt = (TextView)findViewById(R.id.debug);
                    //bTxt.setText(Float.toString(bearing)+"  adjust: "+Float.toString((geoField.getDeclination()))+ "  bear: "+Float.toString(location.getBearing()));

                    // Redraw the current location point & waypoints
                    pdfView.invalidate();
                }
            }
        };
        setupPDFView();
    }

    private void setupPDFView(){
        // Set color and fill of the current location point
        cyan = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyanTrans = new Paint(Paint.ANTI_ALIAS_FLAG);
        red = new Paint(Paint.ANTI_ALIAS_FLAG);
        green = new Paint(Paint.ANTI_ALIAS_FLAG); // debug
        purple = new Paint(Paint.ANTI_ALIAS_FLAG); // debug
        white = new Paint(Paint.ANTI_ALIAS_FLAG);
        black = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline = new Paint();
        blue = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyan.setAntiAlias(true);
        cyan.setColor(Color.CYAN);
        cyan.setStyle(Paint.Style.FILL);
        outline.setColor(Color.WHITE);
        outline.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        red.setColor(Color.RED);
        red.setStyle(Paint.Style.FILL);
        green.setColor(Color.GREEN); // debug
        green.setStyle(Paint.Style.FILL); // debug
        purple.setColor(Color.argb(100,255,0,255)); // debug
        purple.setStyle(Paint.Style.FILL); // debug
        black.setColor(Color.BLACK);
        black.setStyle(Paint.Style.FILL);
        cyanTrans.setColor(argb(40, 0, 255, 255));
        cyanTrans.setStyle(Paint.Style.FILL);
        blue.setAntiAlias(true);
        blue.setColor(Color.BLUE);
        blue.setStyle(Paint.Style.FILL);
        // Waypoint popup text
        txtCol.setStyle(Paint.Style.FILL);
        txtCol.setColor(Color.BLACK);
        txtCol.setTextSize(txtSize);
        final int emoji_width = Math.round(getResources().getDimension(R.dimen.emoji_width));
        TextPaint txtPaint = new TextPaint(); // Size of the emoji arrow in push pin balloon
        txtPaint.setTextSize(emoji_width);
        // Waypoint popup text arrow emoji
        //Initialise the layout & add a TextView with the emoji in it
        String emoji = new String(Character.toChars(0x279e)); //0x279d)); // 0x279c arrow right //0x27b2)); //0x1F369)); //Doughnut
        // StaticLayout was deprecated in API level 28 use StaticLayout.Builder
        // Check if we're running on Android 6.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder sb= StaticLayout.Builder.obtain(emoji, 0, emoji.length(), txtPaint, emoji_width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(1,1)
                    .setIncludePad (true);
            lsLayout = sb.build();
        } else {
            //StaticLayout layout = new StaticLayout(text, paint, width, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, false);
            lsLayout = new StaticLayout(emoji, txtPaint, emoji_width, Layout.Alignment.ALIGN_CENTER, 1, 1, true);
        }

        // GET THE PDF FILE
        File file = new File(path);
        if (file.canRead()) {
            // LOAD IT, load only first page
            //
            // LISTEN FOR TAP TO ADD WAYPOINT OR DISPLAY PT DATA
            //
            pdfView.fromFile(file).defaultPage(0).pages(0).onRender((onRenderListener) -> {
                pdfView.fitToWidth(0); // optionally pass page number
            })
                    // SINGLE TAP
                    .onTap(e -> {
                        //Log.d("onTap","Clicked on map. clickedWP="+clickedWP);
                        updatePageSize(); // get new pdf page width and height
                        // set flag if user clicked on a waypoint in the top or bottom half of the map. Used to display popup balloon above or below waypoint
                        if ((clickedWP == -1 || (lastClickedWP != -1 && lastClickedWP != clickedWP)) && !showAllWayPtLabels) {
                            clickedTopHalf = e.getY() < screenHeight / 2.0;
                        }

                        if (!showAllWayPts && !addWayPtFlag) {
                            //Toast.makeText(PDFActivity.this,"Waypoints are hidden.",Toast.LENGTH_LONG).show();
                            return false;
                        }
                        // show wait icon
                        //wait.setVisibility(View.VISIBLE);
                        boolean found = false;
                        newWP = false; // if added a new waypoint show balloon too
                        float x, y;
                        float zoom = pdfView.getZoom();
                        double toScreenCordX = (optimalPageWidth.get() * zoom) / mediaBoxWidth;
                        double toScreenCordY = (optimalPageHeight.get() * zoom) / mediaBoxHeight;
                        double marginL = toScreenCordX * marginLeft;
                        double marginT = toScreenCordY * marginTop;
                        double marginx = toScreenCordX * marginXworld;
                        double marginy = toScreenCordY * marginYworld;
                        x = (e.getX() - pdfView.getCurrentXOffset());
                        y = (e.getY() - pdfView.getCurrentYOffset());
                        double wayPtX, wayPtY;
                        //
                        // Check if clicked on waypoint popup balloon of the single waypoint that is showing the balloon
                        //
                        if (clickedWP != -1 && clickedWP < wayPts.size()) {
                            wayPtX = (((wayPts.get(clickedWP).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx)) + marginL;
                            wayPtY = (((lat2 - wayPts.get(clickedWP).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                            String desc = wayPts.get(clickedWP).getDesc();
                            if (desc.length() > 13) desc = desc.substring(0, 12);
                            float textWidth = txtCol.measureText(desc);
                            int emoji_width1 = Math.round(getResources().getDimension(R.dimen.emoji_width));//used to be 80
                            int marg = Math.round(getResources().getDimension(R.dimen.ten_dp));//10
                            if (pdfView.getCurrentXOffset() + wayPtX <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
                                // Test for balloon popup going off right side of screen
                                int offsetBox = getOffsetXBox((int) wayPtX, textWidth, emoji_width1);

                                // Test for balloon popup going off top of screen
                                int offsetYBox = 0;
                                if (clickedTopHalf) {
                                    offsetYBox = getOffsetYBox();//startY + boxHt;
                                }
                                if (x > ((wayPtX - (textWidth / 2) - marg) + offsetBox) && x < ((wayPtX + (textWidth / 2) + marg + emoji_width1) + offsetBox) &&
                                        y < (wayPtY - startY + marg + offsetYBox) && y >= (wayPtY - startY - boxHt - marg + offsetYBox)) {
                                    try {
                                        //Log.d("onTap","Clicked on waypoint balloon.");
                                        // Open EditWayPointActivity
                                        Intent i1 = new Intent(PDFActivity.this, EditWayPointActivity.class);
                                        //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        i1.putExtra("CLICKED", clickedWP);
                                        i1.putExtra("NAME", mapName);
                                        i1.putExtra("PATH", path);
                                        i1.putExtra("BOUNDS", strBounds);
                                        i1.putExtra("MEDIABOX", strMediaBox);
                                        i1.putExtra("VIEWPORT", strViewPort);
                                        //i1.putExtra("LANDSCAPE", landscape);
                                        startActivity(i1);
                                        // hide wait icon
                                        wait.setVisibility(View.GONE);
                                        return false;
                                    } catch (OutOfMemoryError memoryError) {
                                        Toast.makeText(PDFActivity.this, "Out of memory", Toast.LENGTH_SHORT).show();
                                        return false;
                                    } catch (Exception error) {
                                        Toast.makeText(PDFActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        return false;
                                    }
                                }
                            }
                        }

                        // check if clicked on any balloon when all waypoints are showing their labels
                        if (showAllWayPtLabels) {
                            //Log.d("PDFActivity","Show all waypoint labels.");
                            for (int j = 0; j < wayPts.size(); j++) {
                                wayPtX = (((wayPts.get(j).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx)) + marginL;
                                wayPtY = (((lat2 - wayPts.get(j).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                                String desc;
                                desc = wayPts.get(j).getDesc();
                                if (desc.length() > 13) desc = desc.substring(0, 12);
                                float textWidth = txtCol.measureText(desc);
                                int emoji_width1 = Math.round(getResources().getDimension(R.dimen.emoji_width));//used to be 80
                                int marg = Math.round(getResources().getDimension(R.dimen.ten_dp));//10
                                if (pdfView.getCurrentXOffset() + wayPtX <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
                                    // Test for balloon popup going off right or left side of screen
                                    int offsetBox = getOffsetXBox((int) wayPtX, textWidth, emoji_width1);
                                    //int offsetBox;
                                    //offsetBox = (int) Math.round((optimalPageWidth.get() * zoom) - (wayPtX + (textWidth / 2) + marg + emoji_width1));
                                    // Test for balloon popup going off the left side of screen
                                    //if (offsetBox >= 0) {
                                    //    offsetBox = 0;
                                    //    if ((wayPtX - (textWidth / 2) - marg) < 0)
                                    //        offsetBox = (int) (-1 * Math.round(wayPtX - (textWidth / 2) - marg));
                                    //}
                                    // Test for balloon popup going off top or bottom of screen
                                    int offsetYBox = 0;
                                    if ((wayPtY + pdfView.getCurrentYOffset()) < (pdfView.getHeight()/2.0)){
                                    //if (clickedTopHalf) {
                                        offsetYBox = getOffsetYBox();//startY + boxHt;
                                    }
                                    if (x > ((wayPtX - (textWidth / 2) - marg) + offsetBox) && x < ((wayPtX + (textWidth / 2) + marg + emoji_width1) + offsetBox) &&
                                            y < (wayPtY - startY + marg + offsetYBox) && y >= (wayPtY - startY - boxHt - marg + offsetYBox)) {
                                        try {
                                            //Log.d("onTap","Clicked on waypoint balloon (all labels showing).");
                                            // Open EditWayPointActivity
                                            Intent i1 = new Intent(PDFActivity.this, EditWayPointActivity.class);
                                            //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            i1.putExtra("CLICKED", j);
                                            i1.putExtra("NAME", mapName);
                                            i1.putExtra("PATH", path);
                                            i1.putExtra("BOUNDS", strBounds);
                                            i1.putExtra("MEDIABOX", strMediaBox);
                                            i1.putExtra("VIEWPORT", strViewPort);
                                            startActivity(i1);
                                            // hide wait icon
                                            wait.setVisibility(View.GONE);
                                            return false;
                                        } catch (OutOfMemoryError memoryError) {
                                            Toast.makeText(PDFActivity.this, "Out of memory", Toast.LENGTH_SHORT).show();
                                            return false;
                                        } catch (Exception error) {
                                            Toast.makeText(PDFActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                            return false;
                                        }
                                    }
                                }
                            }
                        }

                        //
                        // Check if clicked on existing waypoint
                        //
                        double longitude = (((x - marginL) / ((optimalPageWidth.get() * zoom) - marginx)) * longDiff) + long1;
                        double latitude = ((((y - marginT) / ((optimalPageHeight.get() * zoom) - marginy)) * latDiff) - lat2) * -1;
                        // If showing all balloons, and click to add new point it should add it and not hide the currently selected balloon.
                        if (showAllWayPtLabels) clickedWP = -1;

                        //TextView bTxt = (TextView)findViewById(R.id.debug);
                        //bTxt.setTextColor(Color.WHITE);
                        //bTxt.setText("X offset: "+pdfView.getCurrentXOffset()+" Tap at: " +x+", "+y+" Long: "+String.format("%.4f",longitude)+ " Lat: "+String.format("%.4f",latitude));

                        // If clicked on existing waypoint show balloon with name
                        for (int i1 = wayPts.size() - 1; i1 > -1; i1--) {
                            // convert this waypoint lat, long to screen coordinates
                            wayPtX = ((wayPts.get(i1).getX() - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                            wayPtY = (((lat2 - wayPts.get(i1).getY()) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                            if (x > (wayPtX - margX) && x < (wayPtX + margX) &&
                                    y < (wayPtY + margBottom) && y >= (wayPtY - margTop)) {
                                lastClickedWP = clickedWP;
                                clickedWP = i1;
                                found = true;
                                //Log.d("onTap","Clicked on existing waypoint.");
                                break;
                            }
                        }

                        // Add new waypoint
                        if (clickedWP == -1) {
                            // Check if waypoint menu item is active and set it to inactive
                            if (wayPtMenuItem != null)
                                wayPtMenuItem.setIcon(R.mipmap.ic_grey_pin_forgnd);
                            //Log.d("onTap", "map click detected (not on existing waypoint.)");
                            // Make sure user click is not off the map!
                            if (!(latitude > lat1 && latitude < lat2 && longitude > long1 && longitude < long2)) {
                                Toast.makeText(PDFActivity.this, " Off Map.", Toast.LENGTH_LONG).show();
                                clickedWP = -1;
                            }
                            // Check if clicked too close to edge, Warn user
                    /*else if (!(latitude > lat1 && latitude < (lat2-.01) && longitude > long1 && longitude < long2)){
                        Toast.makeText(PDFActivity.this, " Too close to edge.", Toast.LENGTH_SHORT).show();
                        clickedWP = -1;
                    }*/
                            else {
                                //Log.d("onTap","Add new waypoint to database.");
                                // ignore taps unless pressed add waypoint button first
                                if (!addWayPtFlag) return false;
                                wait.setVisibility(View.VISIBLE);
                                newWP = true;
                                String location = String.format(Locale.US,"%.5f, %.5f", latitude,longitude);
                                int num = wayPts.size() + 1;
                                WayPt wayPt = wayPts.add(mapName, "Waypoint " + num, (float) longitude, (float) latitude, "blue", location);
                                //String desc = wayPt.getDesc();
                                wayPts.SortPts();
                                try {
                                    db.addWayPt(wayPt);
                                } catch (SQLException exc) {
                                    Toast.makeText(PDFActivity.this, "Failed to save waypoint. "+exc.getMessage(), Toast.LENGTH_LONG).show();
                                    clickedWP = -1;
                                    newWP = false;
                                    addWayPtFlag=false;
                                    return false;
                                }
                                // get the index of the new waypoint
                                for (int i1 = 0; i1 < wayPts.size(); i1++) {
                                    //if (wayPts.get(i1).getDesc().equals(desc)) {
                                    if (wayPts.get(i1).getX() == (float) longitude && wayPts.get(i1).getY() == (float) latitude) {
                                        lastClickedWP = clickedWP;
                                        clickedWP = i1;
                                        //Log.d("onTap","Added to database, clickedWP="+clickedWP);
                                        break;
                                    }
                                }
                                // reset add waypoint button
                                addWayPtFlag=false;
                            }
                        }
                        // hide old balloon
                        else if (!found)
                            clickedWP = -1;
                        return false;
                    }).onDraw((canvas, pageWidth, pageHeight, displayedPage) -> {
                //Log.d("onDraw", "enter onDraw");
                updatePageSize(); // get new pdf page width and height
                // Display current lat/long position
                TextView pTxt = findViewById(R.id.cur_pos);
                pTxt.setTextColor(Color.WHITE);
                String str;
                if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2) {
                    str = getString(R.string.CurPos) + String.format(Locale.US,"%.05f", latNow) + ", " + String.format(Locale.US,"%.05f", longNow);
                } else {
                    str = getString(R.string.CurPos) + "Not on Map";
                }
                pTxt.setText(str);

                // -------------------------------------
                //   Draw current location
                // -------------------------------------
                double zoom = pdfView.getZoom();
                // convert to screen coordinates
                double toScreenCordX = (optimalPageWidth.get() * zoom) / mediaBoxWidth;
                double toScreenCordY = (optimalPageHeight.get() * zoom) / mediaBoxHeight;
                double marginL = toScreenCordX * marginLeft;
                double marginT = toScreenCordY * marginTop;
                double marginx = toScreenCordX * marginXworld;
                double marginy = toScreenCordY * marginYworld;

                //double x1,x2, y1, y2;
                //if (debug ) {
                /*    Log.d("MediaBox","0 0 "+mediaBoxX2+" "+mediaBoxY2);
                    Log.d("BBox",bBoxX1+" "+bBoxY1+" "+bBoxX2+" "+bBoxY2);
                    Log.d("Margins","Left="+marginLeft+" Right="+marginRight+" Top="+marginTop+" Bottom="+marginBottom);
                    Log.d("PageWidth",""+(optimalPageWidth.get()*zoom));
                    Log.d("Long1",""+""+long1);
                    Log.d("Long2",""+long2);
                    Log.d("longDiff",""+longDiff);
                    Log.d("marginx",""+marginx);
                    Log.d("marginL",""+marginL);
                    Log.d("PageHeight",""+(optimalPageHeight.get()*zoom));
                    Log.d("Lat1",""+""+lat1);
                    Log.d("Lat2",""+lat2);
                    Log.d("latDiff",""+latDiff);
                    Log.d("marginy",""+marginy);
                    Log.d("marginT",""+marginT);*/
                    // debug: blue dots at lat long bounds
                    //currentLocationX = (((longNow + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                    //currentLocationY = ((((90 - latNow) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                    // long1, lat1
                    /*x1 = (((LatLong[1] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                     y1 = ((((90 - LatLong[0]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                    canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                    // long2, lat2
                    x1 = (((LatLong[3] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                    y1 = ((((90 - LatLong[2]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                    canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                    // long3,lat3
                    x1 = (((LatLong[5] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                    y1 = ((((90 - LatLong[4]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                    canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
                    // long4, lat4
                    x1 = (((LatLong[7] + 180) - (long1 + 180)) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                    y1 = ((((90 - LatLong[6]) - (90 - lat2)) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;
                    canvas.drawCircle((float) x1+9f, (float) y1+9f, 18f, blue);
*/
                    // debug:  place black dots at page width/height
                   /* canvas.translate(0, 0);
                    canvas.drawCircle(5, 0, 5f, black);
                    canvas.drawCircle((float) (optimalPageWidth.get() * zoom)-5f, (float) (optimalPageHeight.get() * zoom)-5f, 5f, black);
                    */
                    // debug: green dots at bbox Viewport
                    /*canvas.translate(0, 0);
                    x1 = toScreenCordX * bBoxX1;
                    y1 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY1;
                    canvas.drawCircle((float)x1+5f,(float)y1+5f,14f, green);
                    x2 = toScreenCordX * bBoxX2;
                    y2 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY2;
                    canvas.drawCircle((float)x2-5f,(float)y2-5f,14f, green);
                    x1 = toScreenCordX * bBoxX2;
                    y1 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY1;
                    canvas.drawCircle((float)x1-5f,(float)y1+5f,14f, green);
                    x2 = toScreenCordX * bBoxX1;
                    y2 = (optimalPageHeight.get() * zoom) - toScreenCordY * bBoxY2;
                    canvas.drawCircle((float)x2+5f,(float)y2-5f,14f, green);

                    // debug: purple dots at MediaBox
                    canvas.drawCircle((float)8f,(float)-8f,16f, purple);
                    x2 = toScreenCordX * mediaBoxX2;
                    y2 = toScreenCordY * mediaBoxY2;
                    canvas.drawCircle((float)x2-8f,(float)y2-8f,16f, purple);


                    // debug: red dots at page margins
                   canvas.drawCircle((float) marginL+5f, (float) marginT+5f, 10f, red);
                    // Toast.makeText(PDFActivity.this, "xRatio="+xRatio+"  left=" + x1+"  top="+y1+"  width="+pdfView.getOptimalPageWidth(), Toast.LENGTH_SHORT).show();
                    x1 = ((optimalPageWidth.get() * zoom) - (toScreenCordX * marginRight));
                    y1 = ((optimalPageHeight.get() * zoom) - (toScreenCordY * marginBottom));
                    canvas.drawCircle((float) x1-5f, (float) y1-5f, 10f, red);
                    canvas.drawCircle((float) marginL+5f, (float) y1-5f, 10f, red);
                    canvas.drawCircle((float) x1-5f, (float) marginT+5f, 10f, red);
                     */
                //}
                // debug lat/long at corners for Poudre Park FS
                //x1 = (((-105.37499 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                //y1 = ((((90 - 40.75) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                //x1 = (((-105.25 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                //y1 = ((((90 - 40.625) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                //x1 = (((-105.25 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                //y1 = ((((90 - 40.75) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);
                //x1 = (((-105.37499 + 180) - (long1 + 180)) / longDiff) * ((pdfView.getOptimalPageWidth() * zoom) - marginx) + marginL;
                //y1 = ((((90 - 40.625) - (90 - lat2)) / latDiff) * ((pdfView.getOptimalPageHeight() * zoom) - marginy)) + marginT;
                //canvas.drawCircle((float) x1-5, (float) y1-5, 10f, red);



                // count++;
                // Log.d("border","zoom="+zoom);
                //Toast.makeText(PDFActivity.this, "counter="+count+"   zoom="+zoom+"  right=" + x1+"  bottom="+y1, Toast.LENGTH_SHORT).show();

                // Draw the current location as a point on the map. Color of the point is defined in paint & outline above.
                //  CONVERT LAT LONG TO SCREEN COORDINATES
                currentLocationX = ((longNow - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                currentLocationY = (((lat2 - latNow) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;


                // Add waypoint at current location
                if (markCurrent) {
                    //Log.d("PDFActivity", "onDraw: waypoint at current location");
                    markCurrent = false;
                    float theLat = (float) latNow;
                    float theLong = (float) longNow;
                    String location = String.format(Locale.US,"%.5f", theLat) + ", " + String.format(Locale.US,"%.5f", theLong);
                    int num = wayPts.size() + 1;
                    WayPt wayPt = wayPts.add(mapName, "Waypoint " + num, theLong, theLat, "red", location);
                    wayPts.SortPts();
                    try {
                        db.addWayPt(wayPt);
                    } catch (SQLException exc) {
                        Toast.makeText(PDFActivity.this, "Failed to add pt to database.", Toast.LENGTH_LONG).show();
                    }
                    for (int i1 = 0; i1 < wayPts.size(); i1++) {
                        if (wayPts.get(i1).getX() == theLong && wayPts.get(i1).getY() == theLat) {
                            lastClickedWP = clickedWP;
                            clickedWP = i1;
                            //Log.d("onDraw","Added waypoint at current location, clickedWP="+clickedWP);
                            break;
                        }
                    }
                }
                // Draw Waypoints
                if (showAllWayPts) {
                    for (int i12 = 0; i12 < wayPts.size(); i12++) {
                        //Log.d("PDFActivity","drawing waypoint "+i12);
                        double xLong = wayPts.get(i12).getX();
                        double yLat = wayPts.get(i12).getY();
                        // convert lat, long to screen coordinates
                        float x = (float) (((xLong - long1) / longDiff) * (((optimalPageWidth.get() * zoom) - marginx)) + marginL);
                        float y = (float) (((lat2 - yLat) / latDiff) * (((optimalPageHeight.get() * zoom) - marginy)) + marginT);

                        float inner_radius = getResources().getDimension(R.dimen.pin_inner_radius);
                        float pin_stem = pin_ht - pin_radius;
                        float cl_offset = getResources().getDimension(R.dimen.pin_cl_offset); // catch light offset
                        float cl_radius = getResources().getDimension(R.dimen.pin_cl_radius);
                        float pt_radius = getResources().getDimension(R.dimen.pin_pt_radius);
                        canvas.drawCircle(x, y - pin_ht, pin_radius, white); // white outline
                        Paint color = blue;
                        if (wayPts.get(i12).getColorName().equals("cyan")) color = cyan;
                        else if (wayPts.get(i12).getColorName().equals("red")) color = red;

                        // xy point oval
                        RectF pt_rect = new RectF(x - (pt_radius * 2), y - pt_radius, x + (pt_radius * 2), y + pt_radius);
                        canvas.drawOval(pt_rect, black); // point
                        canvas.drawCircle(x, y - pin_ht, inner_radius, color); // center color
                        canvas.drawCircle(x - cl_offset, y - pin_ht - cl_offset, cl_radius, white); // catch light
                        // Draw 5 pixel wide pin stem
                        canvas.drawLine(x - 3, y - pin_stem, x - 3, y, black);
                        canvas.drawLine(x - 2, y - pin_stem, x - 2, y, white);
                        canvas.drawLine(x - 1, y - pin_stem, x - 1, y, white);
                        canvas.drawLine(x, y - pin_stem, x, y, white);
                        canvas.drawLine(x + 1, y - pin_stem, x + 1, y, white);
                        canvas.drawLine(x + 2, y - pin_stem, x + 2, y, white);

                        // Show all Waypoint Labels
                        if (showAllWayPtLabels) {
                            String desc = wayPts.get(i12).getDesc();
                            if (desc.length() > 13) desc = desc.substring(0, 12);

                            float textWidth = txtCol.measureText(desc);
                            if (pdfView.getCurrentXOffset() + x <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
                                // Test for balloon popup going off right or left side of screen
                                int offsetBox = getOffsetXBox(x, textWidth, emoji_width);
                                // Test for waypoint at top half of screen, display popup below
                                int offsetYBox = 0;
                                int offsetYTriangle = 0;
                                if ((y + pdfView.getCurrentYOffset()) < (pdfView.getHeight()/2.0)){
                                    offsetYBox = getOffsetYBox();//startY + boxHt;
                                    offsetYTriangle = getOffsetYTriangle();
                                }
                                // black border
                                Paint recCol = new Paint();
                                int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                                switch (currentNightMode) {
                                    case Configuration.UI_MODE_NIGHT_NO:
                                        // Night mode is not active, we're using the light theme
                                        // black border
                                        recCol.setColor(Color.BLACK);
                                        recCol.setStrokeWidth(3);
                                        canvas.drawRect((x - (textWidth / 2) - marg - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (textWidth / 2) + marg + emoji_width + 3) + offsetBox, y + offsetYBox - startY, recCol);
                                        // white rectangle and triangle
                                        recCol.setColor(Color.WHITE);
                                        recCol.setStrokeWidth(0); // solid fill
                                        canvas.drawRect((x - (textWidth / 2) - marg) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (textWidth / 2) + marg + emoji_width) + offsetBox, y + offsetYBox - startY - 3, recCol);
                                        drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), marg, offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                                        // black text
                                        txtCol.setColor(Color.BLACK);
                                        canvas.drawText(desc, (x - (textWidth / 2)) + offsetBox, y + offsetYBox - startY - (boxHt / 2.0f) - 5 + (txtSize / 2.0f), txtCol);
                                        break;
                                    case Configuration.UI_MODE_NIGHT_YES:
                                        // Night mode is active, we're using dark theme
                                        // White border
                                        recCol.setColor(Color.BLACK);
                                        recCol.setStrokeWidth(3);
                                        canvas.drawRect((x - (textWidth / 2) - marg - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (textWidth / 2) + marg + emoji_width + 3) + offsetBox, y + offsetYBox - startY, recCol);
                                        // gray rectangle and triangle
                                        recCol.setColor(Color.GRAY);
                                        recCol.setStrokeWidth(0); // solid fill
                                        canvas.drawRect((x - (textWidth / 2) - marg) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (textWidth / 2) + marg + emoji_width) + offsetBox, y + offsetYBox - startY - 3, recCol);
                                        drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), marg, offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                                        // white text
                                        txtCol.setColor(Color.WHITE);
                                        canvas.drawText(desc, (x - (textWidth / 2)) + offsetBox, y + offsetYBox - startY - (boxHt / 2.0f) - 5 + (txtSize / 2.0f), txtCol);
                                        break;
                                }

                                // add right arrow emoji in lsLayout defined above
                                canvas.save();
                                canvas.translate((x + (textWidth / 2)) + offsetBox + 10, y + offsetYBox - startY - boxHt - 12);
                                lsLayout.draw(canvas);
                                canvas.restore();
                            }
                        }
                    }
                }

                // Draw popup if waypoint was clicked on
                if ((newWP || clickedWP != -1) && !showAllWayPtLabels && showAllWayPts) {
                    //Log.d("PDFActivity", "onDraw: draw waypoint and popup balloon. newWP="+newWP+" clickedWP="+clickedWP);
                    if (clickedWP != -1 && clickedWP < wayPts.size()) {
                        int i12 = clickedWP;
                        double xLong = wayPts.get(i12).getX();
                        double yLat = wayPts.get(i12).getY();

                        // convert lat, long to screen coordinates
                        float x = (float) (((xLong - long1) / longDiff) * (((optimalPageWidth.get() * zoom) - marginx)) + marginL);
                        float y = (float) (((lat2 - yLat) / latDiff) * (((optimalPageHeight.get() * zoom) - marginy)) + marginT);

                        String desc = wayPts.get(i12).getDesc();
                        if (desc.length() > 13) desc = desc.substring(0, 12);

                        float textWidth = txtCol.measureText(desc);

                        // check if waypoint has scrolled off screen
                        if (pdfView.getCurrentXOffset() + x <= screenWidth && pdfView.getCurrentXOffset() + x > 0) {
                            // Test for balloon popup going off right or left side of screen
                            int offsetBox = getOffsetXBox(x, textWidth, emoji_width);
                            // Test for waypoint at top half of screen, display popup below
                            int offsetYBox = 0;
                            int offsetYTriangle = 0;
                            if ((y + pdfView.getCurrentYOffset()) < (pdfView.getHeight() / 2.0)) {
                                offsetYBox = getOffsetYBox();//startY + boxHt;
                                offsetYTriangle = getOffsetYTriangle();//2 * startY - 3 - boxHt;
                            }
                            // black border
                            Paint recCol = new Paint();
                            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                            switch (currentNightMode) {
                                case Configuration.UI_MODE_NIGHT_NO:
                                    // Night mode is not active, we're using the light theme
                                    // black border
                                    recCol.setColor(Color.BLACK);
                                    recCol.setStrokeWidth(3);
                                    canvas.drawRect((x - (textWidth / 2) - marg - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (textWidth / 2) + marg + emoji_width + 3) + offsetBox, y + offsetYBox - startY, recCol);
                                    // white rectangle
                                    recCol.setColor(Color.WHITE);
                                    recCol.setStrokeWidth(0); // solid fill
                                    canvas.drawRect((x - (textWidth / 2) - marg) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (textWidth / 2) + marg + emoji_width) + offsetBox, y + offsetYBox - startY - 3, recCol);
                                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), marg, offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                                    // white triangle, reset text to black
                                    txtCol.setColor(Color.BLACK);
                                    canvas.drawText(desc, (x - (textWidth / 2)) + offsetBox, y + offsetYBox - startY - (boxHt / 2.0f) - 5 + (txtSize / 2.0f), txtCol);
                                    break;
                                case Configuration.UI_MODE_NIGHT_YES:
                                    // Night mode is active, we're using dark theme
                                    // White border
                                    recCol.setColor(Color.BLACK);
                                    recCol.setStrokeWidth(3);
                                    canvas.drawRect((x - (textWidth / 2) - marg - 3) + offsetBox, y + offsetYBox - startY - boxHt, (x + (textWidth / 2) + marg + emoji_width + 3) + offsetBox, y + offsetYBox - startY, recCol);
                                    // black rectangle
                                    recCol.setColor(Color.GRAY);
                                    recCol.setStrokeWidth(0); // solid fill
                                    canvas.drawRect((x - (textWidth / 2) - marg) + offsetBox, y + offsetYBox - startY - boxHt + 3, (x + (textWidth / 2) + marg + emoji_width) + offsetBox, y + offsetYBox - startY - 3, recCol);
                                    drawTriangle(canvas, recCol, (int) (x), (int) (y + offsetYTriangle - startY - 3), marg, offsetYTriangle); // passing offsetYBox tells if triangle should be up or down
                                    // white text
                                    txtCol.setColor(Color.WHITE);
                                    canvas.drawText(desc, (x - (textWidth / 2)) + offsetBox, y + offsetYBox - startY - (boxHt / 2.0f) - 5 + (txtSize / 2.0f), txtCol);
                                    break;
                            }

                            // add right arrow emoji in lsLayout defined above
                            canvas.save();
                            canvas.translate((x + (textWidth / 2)) + offsetBox + 10, y + offsetYBox - startY - boxHt - 12);
                            lsLayout.draw(canvas);
                            canvas.restore();
                        }
                    }
                }

                //-----------------------
                // Draw Current Location
                //-----------------------
                // Transparent Arc showing bearing (top of user screen)
                //  CONVERT LAT LONG TO SCREEN COORDINATES
                currentLocationX = ((longNow - long1) / longDiff) * ((optimalPageWidth.get() * zoom) - marginx) + marginL;
                currentLocationY = (((lat2 - latNow) / latDiff) * ((optimalPageHeight.get() * zoom) - marginy)) + marginT;

                canvas.translate((float) currentLocationX, (float) currentLocationY);
                // drawArc (RecF(left,top,right,bottom), starting arc in degrees (drawn clockwise, 0=3 o'clock,90=6 o'clock, 180=9 o'clock), finish arc in degrees, use center? paint)
                // Draw the current location as a point on the map. Color of the point is defined in paint & outline above.
                if (accuracy > 25) {
                    accuracy = accuracy / 2;
                    RectF rec = new RectF(-1 * accuracy, -1 * accuracy, accuracy, accuracy);
                    canvas.drawArc(rec, 0, 360, true, cyanTrans); // transparent blue circle
                } else {
                    RectF rec = new RectF(-45f, -45f, 45f, 45f);
                    int arcSize = 90;
                    float startArc = bearing - 45;
                    if (startArc < 0) startArc = 360 + startArc;
                    canvas.drawArc(rec, startArc, arcSize, true, cyanTrans); // transparent blue arc
                    //TextView bTxt = (TextView)findViewById(R.id.debug);
                    //bTxt.setText("bearing="+(int)bearing+"  start="+(int)startArc);
                }

                //Log.d("latlong", "long="+longDiff+"    "+(int)Math.round(currentLocationY)+" long="+(int)Math.round(currentLocationX));

                // DRAW POINT AT CURRENT LOCATION drawCircle(x,y,radius,paint)
                // drawCircle(centerX,centerY,radius,paint)
                canvas.drawCircle(0, 0, 20f, cyan); // final blue outline
                canvas.drawCircle(0, 0, 19f, white); // larger white outline
                canvas.drawCircle(0, 0, 15f, cyan); // blue center
                canvas.translate((float) -currentLocationX, (float) -currentLocationY);

                // hide wait icon
                wait.setVisibility(View.GONE);
                // debug: offset
                //canvas.translate(0,0);
                //canvas.drawCircle(pdfView.getCurrentXOffset()-(float)currentLocationX,pdfView.getCurrentYOffset()-(float)currentLocationY,20f, red);

                //Toast.makeText(PDFActivity.this,"Re-Draw "+count, Toast.LENGTH_SHORT).show();
            }).onLoad(nbPages -> {
                //Log.d("PDFActivity", "onLoad");
                // Do not rotate pdf when they rotate the screen. It loses their location! pdfView cannot zoom to a point on the screen. ??? Seems to work 6/16/22
                // Lock current screen rotation
                /*if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    action_landscape.setChecked(false);
                    action_portrait.setChecked(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = false;
                } else {
                    action_landscape.setChecked(true);
                    action_portrait.setChecked(false);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = true;
                }*/

                // SET LEVELS TO ZOOM TO WHEN DOUBLE CLICK, 34x44=3168, 22x34=2448
                if (mediaBoxWidth > 1500) {
                    pdfView.setMaxZoom(20f);// used to be 3.0f, 7, 20
                    pdfView.setMidZoom(7f);// used to be 1.75f 3.5
                } else {
                    pdfView.setMaxZoom(10f);// used to be 3.0f, 7, 20
                    pdfView.setMidZoom(3.5f);// used to be 1.75f 3.5
                }
                pdfView.setMinZoom(1f); // default is 1 (full document, no zoom)
                //Toast.makeText(PDFActivity.this,String.valueOf(nbPages), Toast.LENGTH_LONG).show();
                //Toast.makeText(PDFActivity.this,"Loaded file ", Toast.LENGTH_SHORT).show();

                // META DATA
                        /*PdfDocument.Meta meta = pdfView.getDocumentMeta();
                        Log.d(TAG, "title = " + meta.getTitle().toString());
                        Log.d(TAG, "author = " + meta.getAuthor());
                        Log.d(TAG, "subject = " + meta.getSubject());
                        Log.d(TAG, "keywords = " + meta.getKeywords());
                        Log.d(TAG, "creator = " + meta.getCreator());
                        Log.d(TAG, "producer = " + meta.getProducer());
                        Log.d(TAG, "creationDate = " + meta.getCreationDate());
                        Log.d(TAG, "modDate = " + meta.getModDate());*/

                // hide wait icon
                wait.setVisibility(View.GONE);
            }).load();
        } else {
            Toast.makeText(PDFActivity.this, "Cannot read file: " + path, Toast.LENGTH_LONG).show();
        }
    }
    /*public static float px2dp(Resources resource, float px) {
        // Convert pixels to dp (device independent)
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, resource.getDisplayMetrics());
    }*/

    public int getOffsetXBox(float x,float textWidth, int emoji_width){
        // Return the adjustment for popup going off screen horizontally
        // Test for balloon popup going off right side of screen
        int offsetBox = (int) Math.round(screenWidth - (pdfView.getCurrentXOffset() + x + ((textWidth / 2) + marg + emoji_width)));

        // Test for balloon popup going off the left side of screen
        if (offsetBox >= 0) {
            offsetBox = 0;
            if ((pdfView.getCurrentXOffset() + x - (textWidth / 2) - marg) < 0)
                offsetBox = (int) (-1 * Math.round(pdfView.getCurrentXOffset() + x - (textWidth / 2) - marg));
        }
        return offsetBox;
    }
    public int getOffsetYBox(){
        return startY + boxHt;
    }
    public int getOffsetYTriangle(){
        return 2 * startY - 3 - boxHt;
    }

    public void drawTriangle(Canvas canvas, Paint paint, int x, int y, int width, int offsetYBox) {
        // ----  White triangle with black v. For waypoint balloon.
        // \  /
        //  \/
        // if offsetYBox > 0 draw up arrow
        int halfWidth = width / 2;
        Path path = new Path();
        if (offsetYBox == 0) {
            path.moveTo(x - halfWidth, y); // Top left
            path.lineTo(x, y + halfWidth); //  Bottom
            path.lineTo(x + halfWidth, y); // Top right
            path.lineTo(x - halfWidth, y); // Back to Top left
        }else{
            path.moveTo(x - halfWidth, y); // Bottom left
            path.lineTo(x, y - halfWidth); // Top
            path.lineTo(x + halfWidth, y); // Bottom right
            path.lineTo(x - halfWidth, y); // Back to Bottom left
        }
        path.close();

        canvas.drawPath(path, paint);
        Paint outline = new Paint();
        outline.setColor(Color.BLACK);
        outline.setStrokeWidth(3);
        if (offsetYBox == 0) {
            canvas.drawLine(x - halfWidth, y + 3, x, y + halfWidth, outline);
            canvas.drawLine(x + halfWidth, y + 3, x, y + halfWidth, outline);
        }else{
            canvas.drawLine(x - halfWidth, y - 3, x, y - halfWidth, outline);
            canvas.drawLine(x + halfWidth, y - 3, x, y - halfWidth, outline);
        }
    }

    protected void updatePageSize(){
        optimalPageWidth.set((double)pdfView.getPageSize(0).getWidth()); // pdfView.getOptimalPageWidth();
        optimalPageHeight.set((double)pdfView.getPageSize(0).getHeight()); // pdfView.getOptimalPageHeight();
        //Log.d("page size","width="+optimalPageWidth.get()+" height="+optimalPageHeight.get());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();

        // Update Waypoints
        wayPts = db.getWayPts(mapName);
        wayPts.SortPts();
        clickedWP = -1; // hide balloon
        // Start Screen Sensor Listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

        // if user had checked lock orientation, then applay it when return from help or edit waypoint 6/22/22
        if (landscapeLocked){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        else if (portraitLocked){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
    }

   /* @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("landscape", landscape);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        landscape = savedInstanceState.getBoolean("landscape");
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d("PDFActivity:onPause","close dbWayPtHandler, stop location updates");
        db.close();
        stopLocationUpdates();

        // Stop Screen Sensor Listener
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    //  LOCATION UPDATES
    private void startLocationUpdates() {
        LocationRequest mLocationRequest;
        if (Build.VERSION.SDK_INT >= 31){
            mLocationRequest = LocationRequest.create();
            if (mLocationRequest != null) {
                mLocationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(1000); //update location every 30 seconds
            }
        }
        // API <= 30
        else{
            mLocationRequest = new LocationRequest();
            if (mLocationRequest != null) {
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(1000); //update location every 30 seconds
            }
        }

        if ((ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
            }
        }
    }

    private void stopLocationUpdates() {
        if ((ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(PDFActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        }
    }


    // -------------------------------------------------
    //    Screen Orientation or Screen Rotate Event
    // -------------------------------------------------
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Reset screen width and height
        if (Build.VERSION.SDK_INT >= 28 && Build.VERSION.SDK_INT < 30) {
            screenHeight = getResources().getDisplayMetrics().heightPixels;
            screenWidth = getResources().getDisplayMetrics().widthPixels;
        } else if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics deviceWindowMetrics = getApplicationContext().getSystemService(WindowManager.class).getMaximumWindowMetrics();
            screenWidth = deviceWindowMetrics.getBounds().width();
            screenHeight = deviceWindowMetrics.getBounds().height();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }
        // Checks the orientation of the screen for landscape and portrait
         if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)  {
            landscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            landscape = false;
        }
    }

    /*public void zoomToCurrentLocation(float x,float y){
        PointF pivot = new  PointF(x,y);
        pdfView.zoomCenteredTo(3,pivot);
    }*/


    // -------------------------------------------------
    //     Sense Screen Spin (X Axis) Event.
    //     Point phone in new direction.
    // -------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer){
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;

            // mOrentation[0] is azimuth, mOrientation[1] is pitch, and mOrientation[2] is roll
            // if the roll is positive you're in reverse landscape (landscape right)
            // and if the roll is negative you're in landscape (landscape left).
            // -12 to correct for magnetic north versus true north
            if (landscape) {
                // landscape left
                if (mOrientation[2] < 0) {
                    adjust = 90;
                }
                // landscape right
                else {
                    adjust = 270;
                }
                bearing = azimuthInDegress +12 + adjust - 90; // Adjust for landscape. Subtract 90 degrees because canvas needs 0 at the East, this returns 0 at the North.
            }
            else
                bearing = azimuthInDegress +12 - 90;
            if (bearing < 0) bearing = 360 + bearing;
            if (bearing > 360) bearing = bearing - 360;
            //TextView bTxt = (TextView)findViewById(R.id.debug);
            //bTxt.setText("bearing="+(int)bearing+"  Z="+(int)mOrientation[2] );
            // Debug
            //TextView bTxt = (TextView)findViewById(R.id.debug);
            //if (landscape && mOrientation[2] < 0)
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  landscape-left   Bearing: "+(int)bearing);
            //else if (landscape)
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  landscape-right   Bearing: "+(int)bearing);
            //else
            //    bTxt.setText("azimuthInDegrees="+(int)azimuthInDegress+"  portrait   Bearing: "+(int)bearing);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }


    // -------------
    //   ... Menu
    // -------------
    MenuItem action_portrait;
    MenuItem action_landscape;
    MenuItem action_showAll;
    MenuItem action_showWayPts;
    /*DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    // Lock LANDSCAPE
                    action_landscape.setChecked(true);
                    action_portrait.setChecked(false);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = true;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // Lock PORTRAIT
                    action_landscape.setChecked(false);
                    action_portrait.setChecked(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    landscape = false;
                    break;
            }
        }
    };*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mapMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_menu, menu);
        //Lock in the current orientation
        action_portrait = menu.findItem(R.id.action_portrait);
        action_landscape = menu.findItem(R.id.action_landscape);
        action_showAll = menu.findItem(R.id.action_showAll);
        action_showWayPts = menu.findItem(R.id.action_showWayPts);
        wayPtMenuItem = menu.findItem(R.id.action_add_way_pt);
        return true;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_markCurrent){
            TextView pTxt = findViewById(R.id.cur_pos);
            String str = getString(R.string.CurPos) + String.format(Locale.US,"%.05f", latNow) + ", " + String.format(Locale.US,"%.05f", longNow);
            // check if current location is on this map
            if (pTxt.getText().equals(str))
                markCurrent = true;
            else {
                markCurrent = false;
                Toast.makeText(PDFActivity.this,"Current location not on map", Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_deleteAll){
            try {
                clickedWP = -1;
                newWP = false;
                db.deleteWayPts(mapName);
                wayPts.removeAll();
            }catch (SQLException e){
                Toast.makeText(PDFActivity.this,"Problem deleting "+mapName, Toast.LENGTH_LONG).show();
            }
        }
        // All Waypoint Labels
        else if (id == R.id.action_showAll){
            // isChecked() returns the state before the user clicked on it
            // uncheck show all labels
            if (action_showAll.isChecked()){
                action_showAll.setChecked(false);
                showAllWayPtLabels = false;
            }
            // check show all labels, also turn on show waypoints
            else{
                action_showAll.setChecked(true);
                showAllWayPtLabels = true;
                action_showWayPts.setChecked(true);
                showAllWayPts = true;
            }
        }
        // Waypoints
        else if (id == R.id.action_showWayPts){
            // isChecked() returns the state before the user clicked on it
            // uncheck waypoints
            if (action_showWayPts.isChecked()){
                action_showWayPts.setChecked(false);
                showAllWayPts = false;
            }
            // check waypoints
            else{
                action_showWayPts.setChecked(true);
                showAllWayPts = true;
            }
        }
        else if (id == R.id.action_portrait){
            if (!action_portrait.isChecked()) {
                action_portrait.setChecked(true);
                action_landscape.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

                //db2.setMapOrient("portrait"); // update user preference
                landscape = false;
                portraitLocked = true;
                landscapeLocked = false;
            }
            else {
                action_portrait.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                portraitLocked = false;
                landscapeLocked = false;
            }
        }
        else if (id == R.id.action_landscape){
            if (!action_landscape.isChecked()) {
                action_portrait.setChecked(false);
                action_landscape.setChecked(true);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                //db2.setMapOrient("landscape"); // update user preference
                landscape = true;
                portraitLocked = false;
                landscapeLocked = true;
            } else {
                action_landscape.setChecked(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                portraitLocked = false;
                landscapeLocked = false;
            }
        }
        /*else if (id == R.id.action_lock_orient){
            AlertDialog.Builder builder = new AlertDialog.Builder(PDFActivity.this);
            builder.setTitle("Lock Orientation:");
            builder.setMessage("Locgetk map orientation in which mode?").setPositiveButton("LANDSCAPE", dialogClickListener)
                        .setNegativeButton("PORTRAIT", dialogClickListener).show();
            return true;
        }*/
        else if (id == R.id.action_add_way_pt) {
            // turn off add waypoint pin
            if (addWayPtFlag){
                addWayPtFlag = false;
                wayPtMenuItem.setIcon(R.mipmap.ic_grey_pin_forgnd);
            }
            // turn on add waypoint pin
            else {
                addWayPtFlag = true;
                clickedWP = -1; // hide balloon popups
                newWP = false;
                showAllWayPts = true;
                wayPtMenuItem.setIcon(R.mipmap.ic_cyan_pin_forgnd);
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.wayPtInstr), Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.action_add_way_pt_menu) {
                addWayPtFlag = true;
                clickedWP = -1; // hide balloon popups
                newWP = false;
                showAllWayPts = true;
                wayPtMenuItem.setIcon(R.mipmap.ic_cyan_pin_forgnd);
                Toast.makeText(PDFActivity.this, getResources().getString(R.string.wayPtInstr), Toast.LENGTH_LONG).show();
        }
        else if (id == R.id.action_help){
            Intent i = new Intent(PDFActivity.this, PDFHelpActivity.class);
            startActivity(i);
        }
        else if (item.getItemId() == android.R.id.home){
            // back button pressed, return
            Intent mainIntent = new Intent(PDFActivity.this, MainActivity.class);
            mainIntent.putExtra("ID",id);
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}