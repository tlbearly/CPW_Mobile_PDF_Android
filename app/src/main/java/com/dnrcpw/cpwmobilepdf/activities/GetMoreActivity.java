package com.dnrcpw.cpwmobilepdf.activities;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GetMoreActivity extends AppCompatActivity {
    // final String TAG = "DEBUG";
    Button downloadsBtn;
    // link to USFS webapp to download PDFs https://www.arcgis.com/apps/webappviewer/index.html?id=b70e11a68aac46b3a5bd911b82b53c1e
    // USFS https://data.fs.usda.gov/geodata/rastergateway/states-regions/states.php
    // USGS https://ngmdb.usgs.gov/topoview/viewer/
    // old USGS link: https://store.usgs.gov/map-locator

    //private static final int READ_REQUEST_CODE = 42; // For file picker

    // use to avoid startActivityForResult deprecated!!!!!
    // Process the selected pdf map from file picker
    // GetContent creates an ActivityResultLauncher<String> to allow you to pass
    // in the mime type you'd like to allow the user to select
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null) return;
                    // Handle the returned Uri
                    Toast toast = Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.importing), Toast.LENGTH_SHORT);
                    toast.show();
                    String name = "";
                    long fileSize;

                    // IMPORT a BLANK MAP INTO DATABASE
                    String scheme = uri.getScheme();
                    if (scheme.equals("file")) {
                        name = uri.getLastPathSegment();
                    } else if (scheme.equals("content")) {
                        Cursor cursor = GetMoreActivity.this.getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            cursor.moveToFirst();
                            int dispName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int size = cursor.getColumnIndex(OpenableColumns.SIZE);
                            if (dispName != -1 && size != -1) {
                                name = cursor.getString(dispName);
                                fileSize = cursor.getLong(size);
                                if (fileSize == 0) {
                                    Toast.makeText(GetMoreActivity.this, "File is empty. File size is 0B", Toast.LENGTH_LONG).show();
                                    downloadsBtn.performClick(); // Show file picker again
                                    return;
                                }
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    // remove _ags_ from file downloaded from hunting or fishing atlas
                    if (name.indexOf("_ags_") == 0) name = name.substring(5);
                    // Create file name that does not exist in app directory. If it already exists at a digit after the name.
                    File outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name);
                    int j = 0;
                    while (outFile.exists()) {
                        j++;
                        outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name.substring(0, name.length() - 4) + j + ".pdf");
                    }
                    //if (j > 0) name = name.substring(0, name.length() - 4) + j + ".pdf";
                    InputStream is;
                    try {
                        is = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        boolean err = outFile.setWritable(true, true); // ownerOnly was false(world write permissions!) always returns false???? but works
                    } catch (Exception e) {
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.writePermission) + " error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Copy the file from file picker to app directory. Don't delete the original.
                    OutputStream os;
                    try {
                        os = new FileOutputStream(outFile);
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                        return;
                    }
                    byte[] buffer = new byte[1024];
                    int length;
                    while (true) {
                        try {
                            assert is != null;
                            if (!((length = is.read(buffer)) > 0)) break;
                        } catch (IOException | NullPointerException exception) {
                            exception.printStackTrace();
                            Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                            return;
                        }
                        try {
                            os.write(buffer, 0, length);
                        } catch (IOException | NullPointerException exception) {
                            exception.printStackTrace();
                            Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                        }
                    }
                    try {
                        os.close();
                    } catch (IOException | NullPointerException exception) {
                        exception.printStackTrace();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                    }
                    try {
                        is.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                    }
                    String newPath = outFile.getPath();

                    // check if was written
                    if (!outFile.exists()) {
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                    }
                    PDFMap map = new PDFMap(newPath, "", "", "", null, getResources().getString(R.string.loading), "", "");
                    //DBHandler db = new DBHandler(GetMoreActivity.this);
                    //Integer index = db.addMap(map);
                    //map.setId(index);

                    String result = map.importMap(GetMoreActivity.this);
                    if (!result.equals(getResources().getString(R.string.importdone))) {
                        toast.cancel();
                        Toast.makeText(GetMoreActivity.this, result, Toast.LENGTH_LONG).show();
                        //db.deleteMap(map);
                        //db.close();
                    }
                    // Map Import Success
                    else {
                        // Display message and load map
                        toast.cancel();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.importdonemsg), Toast.LENGTH_SHORT).show();
                        //db.updateMap(map);
                        //db.close();
                        // open map
                        Intent i = new Intent(GetMoreActivity.this, PDFActivity.class);
                        //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.putExtra("PATH", map.getPath());
                        i.putExtra("NAME", map.getName());
                        i.putExtra("BOUNDS", map.getBounds());
                        i.putExtra("MEDIABOX", map.getMediabox());
                        i.putExtra("VIEWPORT", map.getViewport());
                        startActivity(i);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more);

        // add button to start up browser to download maps from CPW page
        Button linkButton = findViewById(R.id.downloadMapsBtn);
        linkButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(GetMoreActivity.this);
            builder.setTitle(getString(R.string.notice));
            builder.setMessage(getString(R.string.start_browser)).setPositiveButton("OK", dialogClickListener)
                    .setNegativeButton("CANCEL",dialogClickListener).show();
        });

        // add hyperlink to open help 1-8-23
        TextView openHelp = findViewById(R.id.seeHelpTextView);
        openHelp.setOnClickListener(v -> {
            Intent mainIntent = new Intent(GetMoreActivity.this, GetMoreHelpActivity.class);
            startActivity(mainIntent);
        });

        // Open Android File Picker for PDF files API 1
        downloadsBtn = findViewById(R.id.importBtn);
        downloadsBtn.setOnClickListener(v -> {
            /* Fires an intent to spin up the "file chooser" UI and select a pdf
            * ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            * browser. */
            mGetContent.launch("application/pdf");
        });
    }

    // Handle file selected from File Picker
   /* @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        // The ACTION_GET_CONTENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                try {
                    Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.importing), Toast.LENGTH_LONG).show();
                    uri = resultData.getData();
                    String name = "";
                    long fileSize;

                    // IMPORT a BLANK MAP INTO DATABASE
                    String scheme = uri.getScheme();
                    if (scheme.equals("file")) {
                        name = uri.getLastPathSegment();
                    } else if (scheme.equals("content")) {
                        Cursor cursor = GetMoreActivity.this.getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            cursor.moveToFirst();
                            name = cursor.getString(
                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                            if (fileSize == 0) {
                                Toast.makeText(GetMoreActivity.this, "File is empty. File size is 0B", Toast.LENGTH_LONG).show();
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
                    // Create file name that does not exist in app directory. If it already exists at a digit after the name.
                    File outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name);
                    int j = 0;
                    while (outFile.exists()) {
                        j++;
                        outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name.substring(0, name.length() - 4) + j + ".pdf");
                    }
                    //if (j > 0) name = name.substring(0, name.length() - 4) + j + ".pdf";
                    InputStream is = getContentResolver().openInputStream(uri);
                    boolean ok = outFile.setWritable(true, false); // ownerOnly was false(world write permissions!) always returns false???? but works
                    //if (!ok){
                    //    Toast.makeText(GetMoreActivity.this,getResources().getString(R.string.writePermission), Toast.LENGTH_LONG).show();
                    //}
                    // Copy the file from file picker to app directory. Don't delete the original.
                    OutputStream os = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                    os.close();
                    is.close();
                    String newPath = outFile.getPath();

                    // check if was written
                    if (!outFile.exists()) {
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                    }
                    PDFMap map = new PDFMap(newPath, "", "", "", null, getResources().getString(R.string.loading), "", "");
                    DBHandler db = new DBHandler(GetMoreActivity.this);
                    db.addMap(map);
                    db.close();

                    // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS
                    Intent mainIntent = new Intent(GetMoreActivity.this, MainActivity.class);
                    mainIntent.putExtra("IMPORT_MAP", true);
                    mainIntent.putExtra("PATH", newPath);
                    mainIntent.putExtra("UPDATES", "true");
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                } catch (FileNotFoundException e) {
                    Toast.makeText(GetMoreActivity.this, "File does not exist.", Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                } catch (IOException e) {
                    Toast.makeText(GetMoreActivity.this, "Cannot read file. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                }
            } else {
                Toast.makeText(GetMoreActivity.this, "File does not exist.", Toast.LENGTH_LONG).show();
                downloadsBtn.performClick(); // Show file picker again
            }
        }
    }*/

    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                //Show Browser - if the web  page does not load make sure you are signed in to you gmail account in settings on the emulator
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cpw.state.co.us/learn/Pages/Maps.aspx"));
                    startActivity(browserIntent);
                } catch(ActivityNotFoundException browserNotFound){
                    Toast.makeText(this, "Cannot start your browser.", Toast.LENGTH_SHORT).show();
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                //CANCEL button clicked
                break;
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_more, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Found in res/menu/menu_main.xml
        int id = item.getItemId();

        if (id == R.id.action_help){
            Intent intent = new Intent(GetMoreActivity.this, GetMoreHelpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        // return flag UPDATES, to tell MainActivity to not show
        // "you need to update" dialog again!
        else if (item.getItemId() == android.R.id.home) {
            // back button pressed, return
            Intent mainIntent = new Intent(GetMoreActivity.this, MainActivity.class);
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}