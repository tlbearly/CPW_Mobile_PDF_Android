package com.dnrcpw.cpwmobilepdf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
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
    //private static final int READ_REQUEST_CODE = 42; // For file picker

    // use to avoid startActivityForResult deprecated!!!!!
    // Process the selected pdf map from file picker
    // GetContent creates an ActivityResultLauncher<String> to allow you to pass
    // in the mime type you'd like to allow the user to select
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null) return;
                    // Handle the returned Uri
                    Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.importing), Toast.LENGTH_LONG).show();
                    //uri = resultData.getData();
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
                    InputStream is;
                    try {
                        is = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                        Toast.makeText(GetMoreActivity.this, getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        outFile.setWritable(true, true); // ownerOnly was false(world write permissions!) always returns false???? but works
                    }catch (Exception e){
                        Toast.makeText(GetMoreActivity.this,getResources().getString(R.string.writePermission)+" error: "+e.getMessage(), Toast.LENGTH_LONG).show();
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
                    DBHandler db = new DBHandler(GetMoreActivity.this);
                    db.addMap(map);
                    db.close();

                    // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS
                    Intent mainIntent = new Intent(GetMoreActivity.this, MainActivity.class);
                    mainIntent.putExtra("IMPORT_MAP", true);
                    mainIntent.putExtra("PATH", newPath);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
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
            //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cpw.state.co.us/learn/Pages/Maps.aspx"));
            //    startActivity(browserIntent);
        });

        // Open Android File Picker for PDF files API 1
        downloadsBtn = findViewById(R.id.importBtn);
        downloadsBtn.setOnClickListener(v -> {
            /* Fires an intent to spin up the "file chooser" UI and select a pdf
            * ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            * browser. */
            //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            //intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only pdfs, using the pdf MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            //intent.setType("application/pdf");
            // TODO: deprecated startActivityForResult
            //startActivityForResult (intent, READ_REQUEST_CODE);
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
                //Show browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cpw.state.co.us/learn/Pages/Maps.aspx"));
                startActivity(browserIntent);
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
}