package com.example.tammy.mapviewer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GetMoreActivity extends AppCompatActivity {
    // final String TAG = "DEBUG";
    Button downloadsBtn;
    private static final int READ_REQUEST_CODE = 42; // For file picker
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more);

        // add button to start up browser to download maps from CPW page
        Button linkButton = findViewById(R.id.downloadMapsBtn);
        linkButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cpw.state.co.us/learn/Pages/Maps.aspx"));
                startActivity(browserIntent);
        });

        // Open Android File Picker for PDF files API 1
        downloadsBtn = findViewById(R.id.importBtn);
        downloadsBtn.setOnClickListener(v -> {
            /* Fires an intent to spin up the "file chooser" UI and select a pdf
            * ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            * browser. */
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // API 19
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only pdfs, using the pdf MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("application/pdf");
            startActivityForResult(intent,READ_REQUEST_CODE);
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
            Uri uri;
            if (resultData != null) {
                try {
                    Toast.makeText(GetMoreActivity.this,getResources().getString(R.string.importing), Toast.LENGTH_LONG).show();
                    uri = resultData.getData();
                    String name="";
                    long fileSize;

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
                    // Create file name that does not exist in app directory. If it already exists at a digit after the name.
                    File outFile = new File(GetMoreActivity.this.getFilesDir() + "/" +name);
                    int j = 0;
                    while (outFile.exists()){
                        j++;
                        outFile = new File(GetMoreActivity.this.getFilesDir() + "/" + name.substring(0,name.length()-4) +j + ".pdf");
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
                    while ((length=is.read(buffer)) > 0){
                        os.write(buffer,0,length);
                    }
                    os.close();
                    is.close();
                    String newPath =  outFile.getPath();

                    // check if was written
                    if (!outFile.exists()) {
                        Toast.makeText(GetMoreActivity.this,getResources().getString(R.string.createFile), Toast.LENGTH_LONG).show();
                    }
                    PDFMap map = new PDFMap(newPath, "", "", "", null, getResources().getString(R.string.loading), "", "");
                    DBHandler db = new DBHandler(GetMoreActivity.this);
                    db.addMap(map);

                    // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS
                    Intent mainIntent = new Intent(GetMoreActivity.this,MainActivity.class);
                    mainIntent.putExtra("IMPORT_MAP", true);
                    mainIntent.putExtra("PATH", newPath);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);


                    /*Intent returnIntent = getIntent();
                    returnIntent.putExtra("IMPORT_MAP", true);
                    returnIntent.putExtra("PATH", newPath);
                    setResult(RESULT_OK, returnIntent);
                    finish();*/

                } catch (FileNotFoundException e) {
                    Toast.makeText(GetMoreActivity.this,"File does not exist.", Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                }
                catch (IOException e){
                    Toast.makeText(GetMoreActivity.this,"Cannot read file. "+e.getMessage(), Toast.LENGTH_LONG).show();
                    //e.printStackTrace();
                    downloadsBtn.performClick(); // Show file picker again
                }
            }
            else {
                Toast.makeText(GetMoreActivity.this,"File does not exist.", Toast.LENGTH_LONG).show();
                downloadsBtn.performClick(); // Show file picker again
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}