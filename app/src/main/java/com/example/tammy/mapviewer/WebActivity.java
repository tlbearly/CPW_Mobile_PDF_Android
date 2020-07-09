package com.example.tammy.mapviewer;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebActivity extends AppCompatActivity {
// Load a CPW, HuntingAtals, or FishingAtlas Website in an activity window and listen for download file
    WebView myWebView;
    private long downloadQueueId;
    private DownloadManager downloadManager;
    String fileName="";
    static final int MY_PERMISSIONS_STORAGE = 2;
    static final int MY_PERMISSIONS_INTERNET = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        AlertDialog.Builder builder;
        // check for internet permission or return.
        if (ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_DENIED) {
            // Permission is not granted. Request the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(WebActivity.this, Manifest.permission.INTERNET)) {
                builder = new AlertDialog.Builder(WebActivity.this);
                builder.setTitle("Notice");
                builder.setMessage("Permission for the internet is needed to download Maps.").setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button. Hide dialog. Ask again
                        ActivityCompat.requestPermissions(WebActivity.this,
                                new String[]{Manifest.permission.INTERNET},
                                MY_PERMISSIONS_INTERNET);
                    }
                }).show();
            }else {
                ActivityCompat.requestPermissions(WebActivity.this,
                        new String[]{Manifest.permission.INTERNET},
                        MY_PERMISSIONS_INTERNET);
            }
        }
        // check for write permission or return
        Intent intent = this.getIntent();
        String url = "";

            //  GET URL to Load
            try {
                url = intent.getExtras().getString("URL");
                if (url.contains("nationalmap"))
                    setTitle("USGS Website");
                else if (url.contains("Hunting"))
                    setTitle("Hunting Atlas Website");
                else if (url.contains("Fishing"))
                    setTitle("Fishing Atlas Website");
                else if (url.contains("cpw"))
                    setTitle("CPW Website");
            } catch (NullPointerException e) {
                Toast.makeText(WebActivity.this, "Trouble reading the web address. Read: " + url, Toast.LENGTH_LONG).show();
                return;
            }
            myWebView = findViewById(R.id.webview);
            WebSettings webSettings = myWebView.getSettings();
            // NOTE: Side note: you'll be warned for a security issue (because JavaScript is evil!). Simply add the SuppressWarning annotation to the onCreate() method
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // open dialog for location tracking
            webSettings.setJavaScriptEnabled(true);
            webSettings.setGeolocationEnabled(true);
            webSettings.supportMultipleWindows();
            final CookieManager cookieManager = CookieManager.getInstance();
            // for api 21. We are targeting 17
            if (Build.VERSION.SDK_INT >= 21) {
                cookieManager.setAcceptThirdPartyCookies(myWebView,true);
            }
            else {
                cookieManager.setAcceptCookie(true);
                //cookieManager.getInstance().setAcceptCookie(true);
            }
            registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            myWebView.setWebChromeClient(new WebChromeClient(){
                @Override
                public void onGeolocationPermissionsShowPrompt(String origin,
                                                              GeolocationPermissions.Callback callback) {
                    callback.invoke(origin, true, false);
                }
            });
            // 11-7-19  if SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
            if (Build.VERSION.SDK_INT >= 19) {
                myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            else if(Build.VERSION.SDK_INT >=11 && Build.VERSION.SDK_INT < 19) {
                myWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }


            // 11-7-19 test calling a java routine here to handle download from the web page
            //myWebView.addJavascriptInterface(new WebviewInterface(), "Interface");



            myWebView.loadUrl(url);

            myWebView.setWebViewClient(new WebViewClient() {
                // Get notified when a new url is about to take over
                @Override
                public boolean shouldOverrideUrlLoading (WebView view, String url) {
                    boolean shouldOverride = false;
                    File destinationDir = new File(getExternalFilesDir(null),"");// Can't do internal file  WebActivity.this.getFilesDir(); // get path to app directory
                    // We only want to handle requests for pdf files, everything else the webview
                    // can handle normally
                    if (url.indexOf(".pdf")>-1) {
                        Toast.makeText(WebActivity.this, "Downloading...", Toast.LENGTH_LONG).show();
                        shouldOverride = true;
                        Uri source = Uri.parse(url);
                        fileName = source.getLastPathSegment();

                        // Use the same file name for the destination
                        File destinationFile = new File (destinationDir, source.getLastPathSegment());

                        // Make a new request pointing to the pdf url
                        DownloadManager.Request request = new DownloadManager.Request(source)
                                .setTitle("Map Download")// Title of the Download Notification
                                .setDescription("Downloading")// Description of the Download Notification
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                                .setDestinationUri(Uri.fromFile(destinationFile))// Uri of the destination file
                                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                                .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
                        if (Build.VERSION.SDK_INT >= 24) {
                            request.setRequiresCharging(false);// Set if charging is required to begin the download
                        }
                        // Add it to the download manager
                        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        downloadQueueId = downloadManager.enqueue(request);// enqueue puts the download request in the queue.
                    }
                    else {
                        Toast.makeText(WebActivity.this, "Must be a PDF file, not "+url.substring(url.length()-4), Toast.LENGTH_LONG).show();
                    }
                    return shouldOverride;
                }
            });


            if (url.indexOf("Hunting")>-1)
                Toast.makeText(WebActivity.this, "Loading HuntingAtlas...", Toast.LENGTH_LONG).show();
            else if (url.indexOf("Fishing")>-1)
                Toast.makeText(WebActivity.this, "Loading FishingAtlas...", Toast.LENGTH_LONG).show();
            else  if (url.indexOf("Maps-Library")>-1)
                Toast.makeText(WebActivity.this, "Loading CPW Maps Library...", Toast.LENGTH_LONG).show();
            else  if (url.indexOf("nationalmap")>-1)
                Toast.makeText(WebActivity.this, "Loading USGS...", Toast.LENGTH_LONG).show();
       // }
    }

    // Handle Download Here
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadQueueId == id) {
                //Toast.makeText(WebActivity.this, "Download Completed.", Toast.LENGTH_SHORT).show();
                // IMPORT a BLANK MAP INTO DATABASE
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadQueueId);
                Cursor c = downloadManager.query(query);
                String newPath = "";
                String path = "";
                if (c.moveToFirst()) {
                    // show status
                    DownloadStatus(c,downloadQueueId);
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                        try {
                            path = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            if (path.indexOf("file://")>-1) path=path.substring(7);
                            // Replace spaces with _
                            path = path.replaceAll("%20", " ");
                            File inFile = new File(path);
                            String name = fileName;
                            // remove _ags_ from file downloaded from hunting or fishing atlas
                            if (name.indexOf("_ags_") == 0) name = name.substring(5);
                            File outFile = new File(WebActivity.this.getFilesDir() + "/" + name);
                            int i = 0;
                            while (outFile.exists()) {
                                i++;
                                outFile = new File(WebActivity.this.getFilesDir() + "/" + name.substring(0, name.length() - 4) + i + ".pdf");
                            }
                            if (i > 0)
                                name = name.substring(0, name.length() - 4) + i + ".pdf";

                            InputStream is = new FileInputStream(inFile);
                            outFile.setWritable(true, false);
                            OutputStream os = new FileOutputStream(outFile);
                            byte buffer[] = new byte[1024];
                            int length = 0;
                            while ((length = is.read(buffer)) > 0) {
                                os.write(buffer, 0, length);
                            }
                            os.close();
                            is.close();
                            newPath = outFile.getPath();
                            // remove temp file
                            inFile.delete();

                        } catch (FileNotFoundException e) {
                            Toast.makeText(WebActivity.this, "File not found. " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            return;
                        } catch (IOException e) {
                            Toast.makeText(WebActivity.this, "Import failed. " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                PDFMap map = new PDFMap(newPath, "", "", "", null, "Loading...", "", "");
                DBHandler db = new DBHandler(WebActivity.this);
                db.addMap(map);

                // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS WITH FLAG TO IMPORT THE NEW MAP
                Intent i = new Intent(WebActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("IMPORT_MAP", true);
                i.putExtra("PATH", newPath);
                WebActivity.this.startActivity(i);

               /* Intent returnIntent = getIntent();
                returnIntent.putExtra("IMPORT_MAP", true);
                returnIntent.putExtra("PATH", newPath);
                setResult(RESULT_OK, returnIntent);
                finish();*/
            }
        };
    };


    // 11-7-19 java code to call from web browser to handle download
    // call it:
    // <script type="text/javascript">
    //   Interface.javaMehod("This information sent from html page.");
    //</script>
    public class WebviewInterface {
        @JavascriptInterface
        public void javaMehod(String val) {
            Log.i("WebActivity", val);
            Toast.makeText(WebActivity.this, val, Toast.LENGTH_SHORT).show();
        }
    }

    // handle error messages
    private void DownloadStatus(Cursor cursor, long DownloadId){
        //column for download  status
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);
        //column for reason code if the download failed or paused
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);
        String statusText = "";
        String reasonText = "";

        switch(status){
            case DownloadManager.STATUS_FAILED:
                statusText = "FAILED";
                switch(reason){
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        reasonText = "ERROR_CANNOT_RESUME";
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        reasonText = "ERROR_DEVICE_NOT_FOUND";
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        reasonText = "ERROR_FILE_ALREADY_EXISTS";
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        reasonText = "ERROR_FILE_ERROR";
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        reasonText = "ERROR_HTTP_DATA_ERROR";
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        reasonText = "ERROR_INSUFFICIENT_SPACE";
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        reasonText = "ERROR_TOO_MANY_REDIRECTS";
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        reasonText = "ERROR_UNHANDLED_HTTP_CODE";
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        reasonText = "ERROR_UNKNOWN";
                        break;
                }
                break;
            case DownloadManager.STATUS_PAUSED:
                statusText = "PAUSED";
                switch(reason){
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        reasonText = "PAUSED_QUEUED_FOR_WIFI";
                        break;
                    case DownloadManager.PAUSED_UNKNOWN:
                        reasonText = "PAUSED_UNKNOWN";
                        break;
                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                        reasonText = "PAUSED_WAITING_FOR_NETWORK";
                        break;
                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                        reasonText = "PAUSED_WAITING_TO_RETRY";
                        break;
                }
                break;
            case DownloadManager.STATUS_PENDING:
                statusText = "PENDING";
                break;
            case DownloadManager.STATUS_RUNNING:
                statusText = "RUNNING";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                statusText = "Successful";
                break;
        }

        Toast toast = Toast.makeText(WebActivity.this,
                "Download " + statusText + " " +
                        reasonText,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 25, 400);
        toast.show();
    }

    // Permission for location service?
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        AlertDialog.Builder builder;
        if (grantResults.length == 0)return;
        switch (requestCode) {
            case MY_PERMISSIONS_STORAGE:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    //Toast.makeText(this,"Permission granted to read external storage.", Toast.LENGTH_LONG).show();

                } else {
                    // permission denied, close app
                    Toast.makeText(this,"Permission needed to read external storage.", Toast.LENGTH_LONG).show();
                    if (Build.VERSION.SDK_INT >= 21) {
                        finishAndRemoveTask();
                    }
                    else {
                        finish();
                    }
                }
                return;
            }
            case MY_PERMISSIONS_INTERNET:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    //Toast.makeText(this,"Permission granted for the internet.", Toast.LENGTH_LONG).show();

                } else {
                    // permission denied, close the app.
                    Toast.makeText(this,"Permission needed for the internet.", Toast.LENGTH_LONG).show();
                    if (Build.VERSION.SDK_INT >= 21) {
                        finishAndRemoveTask();
                    }
                    else {
                        finish();
                    }
                }
                return;
            }
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }
}