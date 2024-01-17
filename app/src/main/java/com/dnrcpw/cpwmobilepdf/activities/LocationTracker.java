package com.dnrcpw.cpwmobilepdf.activities;
/* DEBUG
  Try to pull out location to it's own class. Call it from MainActivity and PDFActivity

  Can it run in the background?
 */
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationTracker {
    private Context mContext = null;
    boolean checkGPS = false;
    boolean checkNetwork = false;
    boolean canGetLocation = false;

    Location loc;
    double latitude;
    double longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    protected LocationManager locationManager;

    public LocationTracker(Context mContext) {
        this.mContext = mContext;
        startLocationUpdates();
    }


    // location variables
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private double latNow, latBefore;
    private double longNow, longBefore = 0.0;
    double updateProximityDist = 160.9344; // default change in distance that triggers updating proximity .1 miles


    //  LOCATION UPDATES
    private void startLocationUpdates() {
        try {
            // Used to keep track of user movement
            latBefore = 0.0;
            longBefore = 0.0;
            LocationRequest mLocationRequest;
            if (Build.VERSION.SDK_INT >= 31){
                mLocationRequest = new LocationRequest.Builder(500)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setWaitForAccurateLocation(false)
                        .setMinUpdateIntervalMillis(500)
                        .setMaxUpdateDelayMillis(1000)
                        .build();
            }
            // API <= 30
            else{
                mLocationRequest = new LocationRequest();
                if (mLocationRequest != null) {
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    mLocationRequest.setInterval(1000); //update location every 1 seconds
                }
            }


            if ((ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                if (mFusedLocationClient != null)
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null); /*Looper.getMainLooper()*/
            } else {
                Toast.makeText(mContext, "Fine Location Services are off.", Toast.LENGTH_LONG).show();
            }
        }
        // 6-15-22 If looper is null and this method is executed in a thread that has not called Looper.prepare().
        catch(IllegalStateException e){
            Toast.makeText(mContext, "No Location Services."+e, Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }
    protected void setupLocation(){
        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        } catch (Exception e){
            // no gps service
            return;
        }
        // UPDATE CURRENT POSITION
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                try {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        float[] results = new float[1];
                        latNow = location.getLatitude();
                        longNow = location.getLongitude(); // make it positive

                        // save current location so we can see how much they moved
                        latBefore = latNow;
                        longBefore = longNow;
                    }
                }
                // try to keep app from crashing no gps 6-15-22
                catch (Exception e) {
                    //return;
                }
            }
        };
    }

    public double getLongitude() {
        if (loc != null) {
            longitude = loc.getLongitude();
        }
        return longitude;
    }

    public double getLatitude() {
        if (loc != null) {
            latitude = loc.getLatitude();
        }
        return latitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public boolean isGPSEnabled(Context context){
        // 6-15-22 Check if GPS is enabled
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(IllegalArgumentException ex){
            return true;
        }
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
}
