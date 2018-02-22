package com.example.tammy.mapviewer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;

import static android.graphics.Color.argb;

public class PDFActivity extends AppCompatActivity implements SensorEventListener {
    PDFView pdfView;
    private static final String TAG = PDFActivity.class.getSimpleName();
    // Color and style of current location point
    Paint cyan = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint cyanTrans = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint red = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint outline = new Paint();
    // current screen location adjusted by zoom level
    double currentLocationX = 0; // start offscreen
    double currentLocationY = 0;
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
    boolean bestQuality = false;

    private FusedLocationProviderClient mFusedLocationClient;
    //private Location mCurrentLocation;
    private LocationCallback mLocationCallback;
    //private int count=0;
    private double lat1, long1, lat2, long2, latDiff, longDiff;

    // Screen Sensor X rotation
    private int adjust; // adjust for landscape or portrait. Sensor reports north for top of portrait screen. For landscape add 90 degrees.
    private boolean landscape=false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;



    // Set global value bestQuality
    public void setPDFQuality(String quality){
        if (quality.equals("best")) bestQuality = true;
        else bestQuality = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        // keep app from timing out and going to screen saver
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // UNPACK OUR DATA FROM INTENT
        String path=null;
        String bounds=null;
        String viewPort=null;
        String mediaBox = null;
        Intent i = this.getIntent();

        //  GET LAT LONG BOUNDS, CONVERT FROM STRING "LAT1 LONG1 LAT2 LONG1 LAT2 LONG2 LAT1 LONG2" TO FLOATS
        try {
            bestQuality = i.getExtras().getBoolean("BEST_QUALITY");
            path = i.getExtras().getString("PATH");
            // GET LAT/LONG
            bounds = i.getExtras().getString("BOUNDS"); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
            bounds = bounds.trim(); // remove leading and trailing spaces
            //Toast.makeText(PDFActivity.this, bounds, Toast.LENGTH_LONG).show();
            int pos = bounds.indexOf(" ");
            lat1 = Double.valueOf(bounds.substring(0, pos));
            bounds = bounds.substring(pos + 1); // strip off 'lat1 '
            pos = bounds.indexOf(" ");
            long1 = Double.valueOf(bounds.substring(0, pos));
            // FIND LAT2
            bounds = bounds.substring(pos + 1); // strip off 'long1 '
            pos = bounds.indexOf(" ");
            lat2 = Double.valueOf(bounds.substring(0, pos));
            // FIND LONG2
            pos = bounds.lastIndexOf(" ");
            long2 = Double.valueOf(bounds.substring(pos + 1));
            longDiff = (long2+180) - (long1+180);
            latDiff = (90-lat1)-(90-lat2);
        }
        catch (Exception e) {
            Toast.makeText(PDFActivity.this, "Trouble reading lat/long from Geo PDF. Read: "+bounds, Toast.LENGTH_LONG).show();
        }
        try {
            // GET MEDIA BOX or PAGE BOUNDARIES for example: "0 0 612 792"
            // FIND X1
            mediaBox = i.getExtras().getString("MEDIABOX");
            mediaBox = mediaBox.trim(); // remove leading and trailing spaces
            int pos = mediaBox.indexOf(" ");
            mediaBoxX1 = Double.valueOf(mediaBox.substring(0, pos));
            // FIND Y1
            mediaBox = mediaBox.substring(pos + 1); // string off 'X1 '
            pos = mediaBox.indexOf(" ");
            mediaBoxY1 = Double.valueOf(mediaBox.substring(0, pos));
            // FIND X2
            mediaBox = mediaBox.substring(pos + 1); // string off 'Y1 '
            pos = mediaBox.indexOf(" ");
            mediaBoxX2 = Double.valueOf(mediaBox.substring(0, pos));
            // FIND Y2
            mediaBox = mediaBox.substring(pos + 1); // string off 'X2 '
            pos = mediaBox.indexOf(" ");
            mediaBoxY2 = Double.valueOf(mediaBox.substring(pos + 1));
        }
        catch (Exception e) {
            Toast.makeText(PDFActivity.this, "Trouble reading mediaBox page boundaries from Geo PDF. Read: "+bounds, Toast.LENGTH_LONG).show();
        }
        try {
            // GET MARGINS - origin is at bottom left. BBox[23 570 768 48]
            viewPort = i.getExtras().getString("VIEWPORT");
            viewPort = viewPort.trim();
            // FIND bBoxX1
            int pos = viewPort.indexOf(" ");
            bBoxX1 = Double.valueOf(viewPort.substring(0, pos));
            // FIND bBoxY1
            viewPort = viewPort.substring(pos + 1); // string off 'bBoxX1 '
            pos = viewPort.indexOf(" ");
            bBoxY1 = Double.valueOf(viewPort.substring(0, pos));
            // FIND bBoxX2
            viewPort = viewPort.substring(pos + 1); // string off 'bBoxY1 '
            pos = viewPort.indexOf(" ");
            bBoxX2 = Double.valueOf(viewPort.substring(0, pos));
            // FIND bBoxY2
            viewPort = viewPort.substring(pos + 1); // string off 'bBoxX2 '
            pos = viewPort.indexOf(" ");
            bBoxY2 = Double.valueOf(viewPort.substring(pos + 1));

            marginTop=mediaBoxY2-bBoxY1;
            marginBottom=bBoxY2;
            marginLeft=bBoxX1;
            marginRight=mediaBoxX2-bBoxX2;

            mediaBoxWidth = mediaBoxX2 - mediaBoxX1;
            mediaBoxHeight = mediaBoxY2 - mediaBoxY1;
            marginXworld = marginLeft+marginRight;
            marginYworld = marginTop+marginBottom;
        }
        catch (Exception e) {
            Toast.makeText(PDFActivity.this, "Trouble reading viewPort margins from Geo PDF. Read: "+viewPort, Toast.LENGTH_LONG).show();
        }

        // Setup Screen Sensor for X Axis rotation (flat)
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);



        // Set color and fill of the current location point
        cyan.setAntiAlias(true);
        cyan.setColor(Color.CYAN);
        cyan.setStyle(Paint.Style.FILL);
        outline.setColor(Color.WHITE);
        outline.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        red.setColor(Color.RED);
        red.setStyle(Paint.Style.FILL);
        black.setColor(Color.BLACK);
        black.setStyle(Paint.Style.FILL);
        cyanTrans.setColor(argb(40,0, 255,255));
        cyanTrans.setStyle(Paint.Style.FILL);

        //PDFVIEW WILL DISPLAY OUR PDFS
        pdfView = (PDFView) findViewById(R.id.pdfView);

        // SACRIFICE MEMORY FOR QUALITY
        pdfView.useBestQuality(bestQuality);

        pdfView.enableAntialiasing(true); // improve rendering a little bit on low-res screens


        // SET UP LOCATION SERVICES
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(PDFActivity.this, permissions, 1);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // I tured location off on my phone and did not hit this error message?????????????
            Toast.makeText(PDFActivity.this, "location services are off.", Toast.LENGTH_SHORT).show();
            return;
        }
       /* mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocationX = location.getLatitude();
                            currentLocationY = location.getLongitude();
                            //Toast.makeText(PDFActivity.this, "Lat/Long " + location.getLatitude() + " / " + location.getLongitude(), Toast.LENGTH_LONG).show();
                        }
                    }
                });*/

       // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    GeomagneticField geoField;

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

                    // Redraw the current location point
                    pdfView.invalidate();
                }
            };
        };









        // GET THE PDF FILE
        File file = new File(path);
        if (file.canRead()) {
            // LOAD IT, load only first page
            pdfView.fromFile(file).defaultPage(0).pages(0).onDraw(new OnDrawListener() {
                @Override
                public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {

                    // -------------------------------------
                    //   Draw current location
                    // ---------------------------
                    double zoom=(double)pdfView.getZoom();
                    // convert to screen coordinates
                    double toScreenCordX = (pdfView.getOptimalPageWidth() * zoom) / mediaBoxWidth;
                    double toScreenCordY = (pdfView.getOptimalPageHeight() * zoom) / mediaBoxHeight;
                    double marginL = toScreenCordX * marginLeft;
                    double marginT = toScreenCordY * marginTop;
                    double marginx = toScreenCordX * marginXworld;
                    double marginy = toScreenCordY * marginYworld;


                    // debug:  place black dots at page width/height
                    //canvas.translate(0, 0);
                    //canvas.drawCircle(0, 0, 10f, black);
                    //canvas.drawCircle((float) (pdfView.getOptimalPageWidth()*zoom), (float) (pdfView.getOptimalPageHeight()*zoom), 10f, black);

                    // debug: red dots at page margins
                    //canvas.drawCircle((float)marginL, (float)marginT, 10f, red);
                   // Toast.makeText(PDFActivity.this, "xRatio="+xRatio+"  left=" + x1+"  top="+y1+"  width="+pdfView.getOptimalPageWidth(), Toast.LENGTH_SHORT).show();
                    //double x1=((pdfView.getOptimalPageWidth() *zoom) - (toScreenCordX * marginRight));
                    //double y1=((pdfView.getOptimalPageHeight() * zoom) - (toScreenCordY * marginBottom));
                    //canvas.drawCircle((float)x1, (float)y1, 10f, red);

                    // count++;
                   // Log.d("border","zoom="+zoom);
                    //Toast.makeText(PDFActivity.this, "counter="+count+"   zoom="+zoom+"  right=" + x1+"  bottom="+y1, Toast.LENGTH_SHORT).show();

                    // Draw the current location as a point on the map. Color of the point is defined in paint & outline above.
                    //  CONVERT LAT LONG TO SCREEN COORDINATES




                    // convert lat/long to screen coordinates
                    currentLocationX = (((longNow+180) - (long1+180))  / longDiff) * (((double)(pdfView.getOptimalPageWidth()* zoom)-marginx)) + marginL;
                    currentLocationY = ((((90-latNow) - (90-lat2)) / latDiff) * ((double)(pdfView.getOptimalPageHeight()* zoom)-marginy)) + marginT;
                    canvas.translate((float) currentLocationX, (float) currentLocationY);


                    // Transparent Arc showing bearing (top of user screen)
                    // drawArc (RecF(left,top,right,bottom), starting arc in degrees (drawn clockwise, 0=3 o'clock,90=6 o'clock, 180=9 o'clock), finish arc in degrees, use center? paint)
                    if (accuracy > 25){
                        accuracy = accuracy/2;
                        RectF rec = new RectF(-1*accuracy, -1*accuracy, accuracy, accuracy);
                        canvas.drawArc(rec, 0, 360, true, cyanTrans); // transparent blue circle
                    }
                    else {
                        RectF rec = new RectF(-45f, -45f, 45f, 45f);
                        int arcSize = 90;
                        float startArc=bearing-arcSize/2;
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


                    // debug: offset
                    //canvas.translate(0,0);
                    //canvas.drawCircle(pdfView.getCurrentXOffset()-(float)currentLocationX,pdfView.getCurrentYOffset()-(float)currentLocationY,20f, red);

                    //Toast.makeText(PDFActivity.this,"Re-Draw "+count, Toast.LENGTH_SHORT).show();
                }
            }).onLoad(new OnLoadCompleteListener() {
                @Override
                public void loadComplete(int nbPages) {
                    pdfView.fitToWidth();
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
                }
            }).load();
        } else {
            Toast.makeText(PDFActivity.this, "Cannot read file: " + path, Toast.LENGTH_LONG).show();
        }



        // SET LEVELS TO ZOOM TO WHEN DOUBLE CLICK
        pdfView.setMaxZoom(7.0f);// used to be 3.0f
        pdfView.setMidZoom(3.5f);// used to be 1.75f
        pdfView.setMinZoom(1f); // default is 1 (full document, no zoom)
    }


    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        // Start Screen Sensor Listener
        mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer,SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
        // Stop Screen Sensor Listener
        mSensorManager.unregisterListener(this,mAccelerometer);
        mSensorManager.unregisterListener(this,mMagnetometer);
    }


    //  LOCATION UPDATES
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(PDFActivity.this,"Please turn on Location Services.", Toast.LENGTH_SHORT).show();
            return;
        }
        LocationRequest mLocationRequest;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //update location every 1 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /*looper*/);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }









    // -------------------------------------------------
    //    Screen Orientation or Screen Rotate Event
    // -------------------------------------------------
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
       // float currentZoom = pdfView.getZoom();
        //Toast.makeText(PDFActivity.this,"Before: xoffset="+pdfView.getCurrentXOffset()+"  height= "+pdfView.getHeight()+" w= "+pdfView.getWidth(), Toast.LENGTH_SHORT).show();
        super.onConfigurationChanged(newConfig);
        //mFusedLocationClient.getLastLocation();
        //PointF pt = new PointF((currentLocationX+pdfView.getCurrentXOffset())*pdfView.getZoom(),(currentLocationY+pdfView.getCurrentYOffset())*pdfView.getZoom());
        // shift to center point
       // PointF pt = new PointF(currentLocationX,currentLocationY);
        //pdfView.zoomCenteredTo(pdfView.getZoom(),pt);

        PointF pt = new PointF((float)currentLocationX/pdfView.getZoom(),(float)currentLocationY/pdfView.getZoom());
        //PointF pt = new PointF((float)currentLocationX,(float)currentLocationY);

        //pdfView.setLeft((int)pt.x);
        //pdfView.setTop((int)(pt.y*pdfView.getZoom()));

        //pdfView.setTranslationX(pt.x); //moves the canvas right leaving white space!!!
        //pdfView.setTranslationY(pt.y); //moves the canvas down leaving white space!!!
        //float zoom=pdfView.getZoom();
        //pdfView.zoomTo(1);
        //pdfView.zoomCenteredTo(zoom,pt);
        //pdfView.zoomCenteredRelativeTo(pdfView.getZoom(),pt);
        //pdfView.zoomCenteredRelativeTo(1,pt);
        //pdfView.getPaddingLeft();
        //float baseX=pdfView.getCurrentXOffset();
        //float baseY=pdfView.getCurrentYOffset();
        //baseX -= pt.x;
        //baseY += pdfView.getOptimalPageHeight() - pt.y;
       // pdfView.moveTo(baseX,baseY);
        //pdfView.loadPages();

        // load page and center at same spot
        int cPos=0;
        int centerPos=pdfView.getPageAtPositionOffset(cPos);
        float pageHt = pdfView.getOptimalPageHeight();
        float viewHt = pdfView.getHeight();
        pdfView.zoomTo(viewHt / pageHt);
        pdfView.setPositionOffset(centerPos);
        pdfView.jumpTo(0);
        pdfView.loadPages();


        //Log.d(TAG, "onConfigurationChanged: x="+pdfView.getX()+" y="+pdfView.getY());

        // DEBUG MESSAGE TO SCREEN
        //TextView bTxt = (TextView)findViewById(R.id.debug);
        //bTxt.setText("X="+(int)pt.x+"  Y="+(int)pt.y);
        //bTxt.setText("Left="+pdfView.getCurrentXOffset()+" Top="+pdfView.getCurrentYOffset());
        //bTxt.setText("Left="+baseX+" Top="+baseY);
        //bTxt.setText("center Y="+centerPos);




        //pdfView.moveTo((float)currentLocationX,(float)currentLocationY);
        //pdfView.moveTo(pdfView.getCurrentXOffset(),pdfView.getCurrentYOffset());

        // debug

        //bTxt.setText("x="+(int)currentLocationX+"  y="+(int)currentLocationY);
       // Toast.makeText(this, "Move to "+currentLocationX*pdfView.getZoom()+" "+currentLocationY*pdfView.getZoom(), Toast.LENGTH_LONG).show();


        // Checks the orientation of the screen for landscape and portrait
         if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)  {
            landscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            landscape = false;
        }
    }

    public void zoomToCurrentLocation(float x,float y){
        PointF pivot = (PointF) new  PointF(x,y);
        pdfView.zoomCenteredTo(3,pivot);
    }


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
            if (landscape) {
                if (mOrientation[2] < 0)
                    adjust = 90;
                else
                    adjust = 270;
                bearing = azimuthInDegress + adjust - 90; // Adjust for landscape. Subtract 90 degrees because canvas needs 0 at the East, this returns 0 at the North.
            }
            else
                bearing = azimuthInDegress - 90;
            if (bearing < 0) bearing = 360 + bearing;
            if (bearing > 360) bearing = bearing - 360;
            //TextView bTxt = (TextView)findViewById(R.id.debug);
            //bTxt.setText("bearing="+(int)bearing+"  Z="+(int)mOrientation[2] );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }


    // -------------
    //   ... Menu
    // -------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_quality).setChecked(bestQuality);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_quality) {
            if (item.isChecked()) {
                item.setChecked(false);
                setPDFQuality("normal");
                pdfView.useBestQuality(false);
            }
            else {
                item.setChecked(true);
                setPDFQuality("best");
                pdfView.useBestQuality(true);
            }
            return true;
        }
        else if (id == R.id.action_open){
            Intent i = new Intent(PDFActivity.this,MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
