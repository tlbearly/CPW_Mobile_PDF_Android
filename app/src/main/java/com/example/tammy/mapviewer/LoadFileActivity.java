package com.example.tammy.mapviewer;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * List all PDF files in the downloads directory. When a file is clicked
 * it imports the geo pdf into the database and displays the map.
 *
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LoadFileActivity extends AppCompatActivity {
    private ListView lv;
    private FileAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_file);

       // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
       // setSupportActionBar(toolbar);
        lv = (ListView) findViewById(R.id.lv2);
        myAdapter = (FileAdapter) new FileAdapter(LoadFileActivity.this, getPDFs());
        lv.setAdapter(myAdapter);
    }

    class SortByDate implements Comparator<File>{
        // Sort array list of type pdfDoc by last modified first.
        @Override
        public int compare(File f1, File f2){
            long firstDate = f1.lastModified();
            long secondDate = f2.lastModified();
            if (firstDate < secondDate) return 1;
            else
                return -1;
        }
    }

    private ArrayList<PDFDoc> getPDFs() {
        ArrayList<PDFDoc> pdfDocs = new ArrayList<>(); // Array of all PDF files in downloads directory in the pdfDoc class format.
        PDFDoc pdfDoc=null; // PDF file in the downloads directory. Contains file name and path (which also includes the file name).

        //TARGET FOLDER
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (downloadsFolder.exists()) {
            // GET ALL FILES IN DOWNLOAD FOLDER
            File[] files = downloadsFolder.listFiles();
            Arrays.sort(files,new SortByDate()); // Sort files in the downloads directory by last modified at the top.

            if (files == null)
                Toast.makeText(LoadFileActivity.this, "No files were found in the downloads folder.", Toast.LENGTH_LONG).show();
            else {
                // LOOP THRU THOSE FILES GETTING NAME AND URI
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getPath().endsWith("pdf")) {
                        pdfDoc = new PDFDoc();
                        pdfDoc.setName(file.getName());
                        pdfDoc.setPath(file.getAbsolutePath());
                        pdfDocs.add(pdfDoc);
                    }
                }
                if (pdfDocs == null) Toast.makeText(LoadFileActivity.this, "No PDFs were found in the downloads folder.", Toast.LENGTH_LONG).show();
            }
        }
        return pdfDocs;
    }
}