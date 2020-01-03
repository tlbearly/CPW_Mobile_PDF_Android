package com.example.tammy.mapviewer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.OpenableColumns;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GetMoreActivity extends AppCompatActivity {
    public static FileObserver observer;
    public final String TAG = "DEBUG";
    Button downloadsBtn;
    private static final int READ_REQUEST_CODE = 42; // For file picker
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Open a web page inside this app (using a WebView)
        Button cpwBtn = (Button) findViewById(R.id.CPWBtn);
        cpwBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent browser1 = new Intent(GetMoreActivity.this,WebActivity.class);
                browser1.putExtra("URL","https://cpw.state.co.us/learn/Pages/Maps-Library.aspx");
                browser1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(browser1);
            }
        });

        Button huntBtn = (Button) findViewById(R.id.huntBtn);
        huntBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Intent browser2 = new Intent(ACTION_VIEW, Uri.parse("https://ndismaps.nrel.colostate.edu/indexM.html?app=HuntingAtlas"));
                Intent browser2 = new Intent(GetMoreActivity.this,WebActivity.class);
                browser2.putExtra("URL","https://ndis-flex-2.nrel.colostate.edu/debug/indexM.html?app=HuntingAtlas");
                //browser2.putExtra("URL","https://ndismaps.nrel.colostate.edu/indexM.html?app=HuntingAtlas");
                browser2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(browser2);
            }
        });

        Button fishBtn = (Button) findViewById(R.id.fishBtn);
        fishBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Intent browser3 = new Intent(ACTION_VIEW, Uri.parse("https://ndismaps.nrel.colostate.edu/indexM.html?app=FishingAtlas")); // start up Chrome
                // Open a web page inside this app (using a WebView)
                Intent browser3 = new Intent(GetMoreActivity.this,WebActivity.class);
                browser3.putExtra("URL","https://ndis-flex-2.nrel.colostate.edu/debug/indexM.html?app=FishingAtlas");
                //browser3.putExtra("URL","https://ndismaps.nrel.colostate.edu/indexM.html?app=FishingAtlas");
                browser3.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(browser3);
            }
        });

        // Open Android File Picker for PDF files API 1
        downloadsBtn = (Button) findViewById(R.id.downloadsBtn);
        downloadsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /**
                 * Fires an intent to spin up the "file chooser" UI and select a pdf.
                 */
                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                // browser.
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // API 19
                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Filter to show only images, using the image MIME data type.
                // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
                // To search for all documents available via installed storage providers,
                // it would be "*/*".
                intent.setType("application/pdf");
                startActivityForResult(intent,READ_REQUEST_CODE);
            }
        });

        // USGS TopoView
        Button usgsBtn = (Button) findViewById(R.id.usgsBtn);
        usgsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Open a web page inside this app (using a WebView)
                Intent browser4 = new Intent(GetMoreActivity.this,WebActivity.class);
                // historical topo maps from usgs https://www.natgeomaps.com/trail-maps/pdf-quads
                // Current US Topo maps from USGS https://viewer.nationalmap.gov/basic/?basemap=b1&category=histtopo%2Custopo&title=Map%20View
                browser4.putExtra("URL","https://viewer.nationalmap.gov/apps/mobile_client/");//?basemap=b1&category=histtopo%2Custopo&title=Map%20View");
                browser4.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(browser4);
            }
        });
    }

    // Handle file selected from File Picker
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        // The ACTION_GET_CONTENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                String newPath = "";
                try {
                    uri = resultData.getData();
                    String name="";
                    long fileSize=0;

                    // IMPORT a BLANK MAP INTO DATABASE
                    String scheme = uri.getScheme();
                    if (scheme.equals("file")) {
                        name = uri.getLastPathSegment();
                    }
                    else if (scheme.equals("content")) {
                        Cursor cursor = GetMoreActivity.this.getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            cursor.moveToFirst();
                            name = cursor.getString(
                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            fileSize=cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                            if (fileSize == 0){
                                Toast.makeText(GetMoreActivity.this,"File is empty. File size is 0B", Toast.LENGTH_LONG).show();
                                downloadsBtn.performClick(); // Show file picker again
                                return;
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    // remove _ags_ from file downloaded from hunting or fishing atlas
                    if (name.indexOf("_ags_") == 0) name = name.substring(5);
                    File outFile = new File(GetMoreActivity.this.getFilesDir() + "/" +name);
                    int j = 0;
                    while (outFile.exists()){
                        j++;
                        outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name.substring(0,name.length()-4) +j + ".pdf");
                    }
                    if (j > 0) name = name.substring(0,name.length()-4) +j + ".pdf";
                    InputStream is = getContentResolver().openInputStream(uri);
                    outFile.setWritable(true,false);
                    OutputStream os = new FileOutputStream(outFile);
                    byte buffer[] = new byte[1024];
                    int length = 0;
                    while ((length=is.read(buffer)) > 0){
                        os.write(buffer,0,length);
                    }
                    os.close();
                    is.close();
                    newPath =  outFile.getPath();

                    PDFMap map = new PDFMap(newPath, "", "", "", null, "Loading...");
                    DBHandler db = new DBHandler(GetMoreActivity.this);
                    db.addMap(map);

                    // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS WITH FLAG TO IMPORT THE NEW MAP
                    Intent i = new Intent(GetMoreActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra("IMPORT_MAP", true);
                    i.putExtra("PATH", newPath);
                    startActivity(i);

                } catch (FileNotFoundException e) {
                    Toast.makeText(GetMoreActivity.this,"File does not exist.", Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                    return;
                }
                catch (IOException e){
                    Toast.makeText(GetMoreActivity.this,"Cannot read file. "+e.getMessage(), Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                    return;
                }
            }
            else {
                Toast.makeText(GetMoreActivity.this,"File does not exist.", Toast.LENGTH_LONG).show();
                downloadsBtn.performClick(); // Show file picker again
                return;
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
