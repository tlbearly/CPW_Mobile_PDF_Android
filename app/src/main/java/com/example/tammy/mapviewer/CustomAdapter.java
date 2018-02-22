package com.example.tammy.mapviewer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by tammy on 9/7/2017.
 * List of Geo PDF Maps that have been scanned for geo bounds, margins, page size in pixels.
 * This info is stored in a database.
 * On click displays a selected geo pdf.
 */

public class CustomAdapter extends BaseAdapter {
    Context c;
    ArrayList<PDFMap> pdfMaps;
    boolean bestQuality;
    byte[] data;
    int percent;
    int selectedId;
    ActionMode mActionMode;
    int vis,hide;
    EditText renameTxt;
    TextView nameTxt;


    public CustomAdapter(Context c, ArrayList<PDFMap> pdfMaps){
        this.c = c;
        this.pdfMaps = pdfMaps;
    }

    public class ImportMapTask extends AsyncTask<Integer,Integer,String> {
        ProgressBar progressBar;
        String path;
        PDFMap pdfMap;
        Context c;

         public ImportMapTask(Context c, PDFMap pdfMap, ProgressBar pb){
             this.c = c;
             this.pdfMap = pdfMap;
             this.progressBar = pb;
             this.path = pdfMap.getPath();
         }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            // show progress bar
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Integer... params){
            // preform background computation
            publishProgress(10); // calls onProgressUpdate

            // load thumbnail
            File file = new File(path);

            // get thumbnail image
            byte[] thumbnail;
            int pageNum = 0;
            PdfiumCore pdfiumCore = new PdfiumCore(c);
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

                pdfiumCore.openPage(pdfDocument, pageNum);

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

                // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
                // RGB_565 - little worse quality, twice less memory usage
                Bitmap bitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.RGB_565);
                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                        width, height);
                //if you need to render annotations and form fields, you can use
                //the same method above adding 'true' as last param
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                thumbnail = stream.toByteArray();
                pdfMap.setThumbnail(thumbnail);
                publishProgress(20);

                // Read binary pdf
                data = readPDF(file);
                if (data.equals(null)){
                    pdfMap.setName("deleting...");
                    return "Import Failed"; //: Problem reading file.";
                }
                publishProgress(40);

                // Read Lat/Long Bounds
                String bounds = getBoundingBox(file);
                if (bounds.equals("")) {
                    pdfMap.setName("deleting...");
                    return "Import Failed"; //: Trouble reading lat/long bounds. Maybe the file is NOT a Geospatial PDF.";
                }
                pdfMap.setBounds(bounds);
                publishProgress(50);

                // Read media box
                String mediaBox = getMediaBox(file);
                if (mediaBox.equals("")) {
                    pdfMap.setName("deleting...");
                    return "Import failed"; //: Trouble reading media box. Maybe the file is NOT a Geospatial PDF.";
                }
                pdfMap.setMediabox(mediaBox);
                publishProgress(60);

                // Read View Port
                String viewPort  = getViewPort(file);
                if (viewPort.equals("")) {
                    pdfMap.setName("deleting...");
                    return "Import Failed"; //: Trouble reading view port. Maybe the file is NOT a Geospatial PDF.";
                }
                publishProgress(70);

                // Get Map Name
                String name = file.getName();
                publishProgress(80);

                // IMPORT INTO the DATABASE
                pdfMap.setName(name);
                pdfMap.setThumbnail(thumbnail);
                pdfMap.setViewport(viewPort);
                pdfMap.setMediabox(mediaBox);
                pdfMap.setBounds(bounds);
                publishProgress(90);
                DBHandler db = DBHandler.getInstance(c);
                db.updateMap(pdfMap);
                publishProgress(100);
            }
            catch(IOException ex) {
                ex.printStackTrace();
                pdfMap.setName("deleting...");
                return "Import Failed";//: Cannot read PDF thumbnail.";
            }
            return "Import Done";
        }

        @Override
        protected void onProgressUpdate(Integer... progress){
            // update the progress bar. The value you pass in publishProgress
            // is passed in the values parameter of this method
            super.onProgressUpdate(progress);
            progressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(String result){
            // result of background computation is sent here
            progressBar.setVisibility(View.GONE);
            if (result != "Import Done"){
                Toast.makeText(c,result, Toast.LENGTH_LONG).show();
                removeItem(pdfMap.getId());
            }
            else
                notifyDataSetChanged(); // Refresh list of pdf maps
        }
    }

    public void setBestQuality(boolean quality){
        bestQuality = quality;
    }

    @Override
    public int getCount() {
        return pdfMaps.size();
    }

    @Override
    public Object getItem(int i){
        return pdfMaps.get(i);
    }

    public void add(PDFMap pdfMap){
        pdfMaps.add(pdfMap);
    }

    public void rename(int id, String name){
        // Rename an imported map
        try {
            DBHandler db = DBHandler.getInstance(c);
            PDFMap map;
            for (int i=0; i<pdfMaps.size();i++ ) {
                if (pdfMaps.get(i).getId() == id) {
                    map = pdfMaps.get(i);
                    Toast.makeText(c,"Renaming: "+map.getName(), Toast.LENGTH_LONG).show();
                    map.setName(name);
                    db.updateMap(map);
                    notifyDataSetChanged();
                    break;
                }
            }
        }
        catch (IndexOutOfBoundsException e){
            Toast.makeText(c,"Problem renaming map: "+e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public long getItemId(int i){
        return i;
    }

    public void removeItem(int id){
        // remove item i from the list and the database
        try {
            DBHandler db = DBHandler.getInstance(c);
            for (int i=0; i<pdfMaps.size();i++ ){
                if (pdfMaps.get(i).getId() == id) {
                    PDFMap map = pdfMaps.get(i);
                    //Toast.makeText(c,"Deleting: "+map.getName(), Toast.LENGTH_LONG).show();
                    db.deleteMap(map);
                    pdfMaps.remove(i);
                    notifyDataSetChanged();
                    if (getCount() == 0){
                        //TextView msg = (TextView) findViewById(R.id.txtMessage);
                        //msg.setText("No maps have been imported. Use the + button to import a map.");
                    }
                    break;
                }
            }
        }
        catch (IndexOutOfBoundsException e){
            Toast.makeText(c,"Problem removing map: "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup){
        if(view==null) {
            // INFLATE CUSTOM LAYOUT
            view = LayoutInflater.from(c).inflate(R.layout.model,viewGroup,false);
        }
        view.setBackgroundResource(android.R.color.transparent);
        view.setClickable(true);
        view.setLongClickable(true);
        final PDFMap pdfMap = (PDFMap)this.getItem(i);

        renameTxt = (EditText) view.findViewById(R.id.rename);
        vis = View.VISIBLE;
        hide = View.GONE;
        nameTxt = (TextView) view.findViewById(R.id.nameTxt);
        ImageView img = view.findViewById(R.id.pdfImage);
        ProgressBar pb = view.findViewById(R.id.loadProgress);
        pb.setVisibility(View.GONE);
        // use thumbnail as the image if it is found.
        byte[] image = pdfMap.getThumbnail();
        if (image != null)
            img.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        else
            img.setImageResource(R.drawable.pdf_icon);

        // BIND DATA
        // Read lat/long bounds, margins, thumbnail and write to database
        if (pdfMap.getName().equals("Loading...")){
            nameTxt.setText("Loading...");
            // call AsyncTask to read pdf binary and update progress bar and import into database
            ImportMapTask importMap = new ImportMapTask(c,pdfMap, pb);
            importMap.execute();
        }
        //else if (pdfMap.getName().equals("deleting..."))
        //    removeItem(pdfMap.getId());
        else {
            nameTxt.setText(pdfMap.getName());
        }

        // VIEW ITEM CLICK
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Display the map
                String bounds = pdfMap.getBounds();
                if (bounds==null){
                    Toast.makeText(c,"This file is not a Geo PDF. Missing GPTS Bounds.",Toast.LENGTH_LONG).show();
                    return;
                }
                String mediaBox = pdfMap.getMediabox();
                if (mediaBox==null){
                    Toast.makeText(c,"This file is not a Geo PDF. Missing MediaBox.",Toast.LENGTH_LONG).show();
                    return;
                }
                String viewPort = pdfMap.getViewport();
                if (viewPort==null){
                    Toast.makeText(c,"This file is not a Geo PDF. Missing Viewport.",Toast.LENGTH_LONG).show();
                    return;
                }
                openPDFView(pdfMap.getPath(),bounds,mediaBox,viewPort);
            }
        });

        // ITEM LONG CLICK - show menu delete item, rename item
        //view.onCreateOptionsMenu(Menu menu){}
        view.setOnLongClickListener(new View.OnLongClickListener() {
              @Override
              public boolean onLongClick(View view) {
                  // save selected id in global
                  selectedId = pdfMap.getId();

                  Toast.makeText(c, "Id: "+selectedId+"  "+pdfMap.getName(), Toast.LENGTH_LONG).show();
                  // Check if EDIT MENU is already showing
                  if (mActionMode != null) {
                      return false;
                  }

                  // Start the EDIT MENU CAB using the ActionMode.Callback defined above
                  mActionMode = view.startActionMode(mActionModeCallback);
                  // Show selected style from drawable ****************NOT WORKING
                  view.setSelected(true);
                  view.setBackgroundResource(R.color.colorAccent); //Use drawable selected state
                  return true;
              }
        });

        return view;
    }

    // EDIT MENU
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.edit_map_name:
                    for (int i=0; i<pdfMaps.size();i++ ) {
                        if (pdfMaps.get(i).getId() == selectedId)
                            renameTxt.setText(pdfMaps.get(i).getName());
                    }
                    nameTxt.setVisibility(hide);
                    renameTxt.setVisibility(vis);
                    renameTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                            boolean handled = false;
                            if(actionId == EditorInfo.IME_ACTION_DONE){
                                rename(selectedId, renameTxt.getText().toString());
                                handled = true;
                            }
                            return handled;
                        }
                    });
                    //rename(selectedId);
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.delete_map:
                    // display alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(c);
                    builder.setTitle("Delete");
                    builder.setMessage("Delete the imported map? This will not remove it from your device storage.").setPositiveButton("DELETE", dialogClickListener)
                            .setNegativeButton("CANCEL",dialogClickListener).show();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };


    // Remove Imported Map dialog
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove map from imported maps
                    removeItem(selectedId);
                    Toast.makeText(c, "Map removed", Toast.LENGTH_LONG).show();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    break;
            }
        }
    };
    // OPEN PDF VIEW - load the map
    private void openPDFView(String path,String bounds,String mediaBox, String viewPort){
        Intent i=new Intent(c,PDFActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("PATH",path);
        i.putExtra("BOUNDS",bounds);
        i.putExtra("MEDIABOX",mediaBox);
        i.putExtra("VIEWPORT",viewPort);
        i.putExtra("BEST_QUALITY",bestQuality);
        c.startActivity(i);
    }


    // ........................................
    // ...   MENU Displayed on long click    ...
    // .........................................


    // ...................
    // ... IMPORT PDF  ...
    // ...................
    // READ PDF BINARY
    /**
     * Knuth-Morris-Pratt Algorithm for Pattern Matching
     */
    class KMPMatch {
        /**
         * Finds the first occurrence of the pattern in the text.
         */
        public int indexOf(byte[] data, byte[] pattern) {
            int[] failure = computeFailure(pattern);

            int j = 0;
            if (data.length == 0) return -1;

            for (int i = 0; i < data.length; i++) {
                while (j > 0 && pattern[j] != data[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == data[i]) { j++; }
                if (j == pattern.length) {
                    return i - pattern.length + 1;
                }
            }
            return -1;
        }

        /**
         * Computes the failure function using a boot-strapping process,
         * where the pattern is matched against itself.
         */
        private int[] computeFailure(byte[] pattern) {
            int[] failure = new int[pattern.length];

            int j = 0;
            for (int i = 1; i < pattern.length; i++) {
                while (j > 0 && pattern[j] != pattern[i]) {
                    j = failure[j - 1];
                }
                if (pattern[j] == pattern[i]) {
                    j++;
                }
                failure[i] = j;
            }

            return failure;
        }
    }
    // Read PDF into byte array
    private byte[] readPDF(File file){
        // Read 4k of bytes from the file into a buffer
        byte[] data; // The pdf file as a byte array
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try
        {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        }catch(IOException e)
        {
            return null;
        }
        finally
        {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
                return null;
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
                return null;
            }
        }
        data=ous.toByteArray(); // PDF FILE AS BYTE ARRAY
        return data;
    }

    // GET LAT LONG BOUNDS
    private String getBoundingBox(File file){
        // Search for the start and stop byte pattern and pull out the xyxy decimal degrees bounding box
        // Read the pdf file for the geo bounding box coordinates in lat/long decimal degrees.
        // GPTS[x1 y1 x2 y2]
        String startStr = "/GPTS[";
        String stopStr="]";
        byte[] startByte = startStr.getBytes(); // The start pattern to match as a byte array
        byte[] stopByte = stopStr.getBytes(); // The stop pattern to match as a byte array

        KMPMatch kmpmatch = new KMPMatch();
        // find start of pattern "/GPTS[" in data
        int startPos = kmpmatch.indexOf(data,startByte);

        if (startPos == -1) {
            return "";
        }
        else {
            startPos += startByte.length; // advance to the position of lat1 in GPTS[lat1 long1 lat2 long1 lat2 long2 lat1 long2]
        }

        // Read until ]
        byte[] remainingData = Arrays.copyOfRange(data,startPos, data.length);
        int stopPos = kmpmatch.indexOf(remainingData,stopByte);
        if (stopPos == -1) {
            return "";
        }
        byte[] xy = Arrays.copyOfRange(data,startPos,startPos+stopPos);
        String xyStr = new String(xy); // convert byte array to a string

        kmpmatch=null;

        //Toast.makeText(c,"Lat/Long bounds="+xyStr+"  "+file.getName(), Toast.LENGTH_LONG).show();
        return  xyStr;
    }

    // GET PIXEL PAGE BOUNDS x1 y1 x2 y2
    private String getMediaBox(File file){
        // Search for the start and stop byte pattern and pull out the media bounding box:
        // MediaBox[0 0 612 792] or MediaBox [0 0 792 1224]
        String startStr = "/MediaBox";
        String stopStr="]";
        byte[] startByte = startStr.getBytes(); // The start pattern to match as a byte array
        byte[] stopByte = stopStr.getBytes(); // The stop pattern to match as a byte array

        KMPMatch kmpmatch = new KMPMatch();
        // find start of pattern "/MediaBox" in data
        int startPos = kmpmatch.indexOf(data,startByte);

        if (startPos == -1) {
            return "";
        }
        else {
            startPos += startByte.length; // advance to the position of lat1 in GPTS[lat1 long1 lat2 long1 lat2 long2 lat1 long2]
        }

        // Read until ]
        byte[] remainingData = Arrays.copyOfRange(data,startPos, data.length);
        int stopPos = kmpmatch.indexOf(remainingData,stopByte);
        if (stopPos == -1) {
            return "";
        }
        byte[] xy = Arrays.copyOfRange(data,startPos,startPos+stopPos);
        String xyStr = new String(xy); // convert byte array to a string
        return xyStr.substring(xyStr.indexOf("[")+1);
    }

    // GET PAGE VIEWPORT marginLeft marginTop marginRight marginBottom where origin is in the bottom left
    private String getViewPort(File file){
        // Search for the start and stop byte pattern and pull out the viewport:
        // /VP[<</BBox[23.0379 570.713 768.6 48.3] or
        // /VP[<</Type/ViewPort/BBox[28.1 712.5 583.3 129]
        String startStr = "/VP[";
        String stopStr="]";
        byte[] startByte = startStr.getBytes(); // The start pattern to match as a byte array
        byte[] stopByte = stopStr.getBytes(); // The stop pattern to match as a byte array

        KMPMatch kmpmatch = new KMPMatch();
        // Find start of pattern /VP[
        int startPos = kmpmatch.indexOf(data,startByte);

        if (startPos == -1) {
            return "";
        }
        else {
            startPos += startByte.length; // advance to the position of <</BBox[23.0379 570.713 768.6 48.3] or
            // <</Type/ViewPort/BBox[28.1 712.5 583.3 129]
        }

        /// find end of pattern "VP[...]" in data. Read until ]
        byte[] remainingData = Arrays.copyOfRange(data,startPos, data.length);
        int stopPos = kmpmatch.indexOf(remainingData,stopByte);
        // if not found
        if (stopPos == -1) {
            return "";
        }
        byte[] xy = Arrays.copyOfRange(data,startPos,startPos+stopPos);
        String xyStr = new String(xy); // convert byte array to a string
        return xyStr.substring(xyStr.indexOf("[")+1);
    }


    // OPEN PDF Activity VIEW, IMPORT INTO DATABASE
   /* public void importMap(int pos,PDFMap pdfMap){
        // Scan the pdf for lat/long bounds, margins, page size, and thumbnail.
        // Write the pdf file path and all info to the database.
        // Then load the map.

        ProgressBar pb=null;// = findViewById(R.id.loadProgress);
        try {
            String path = pdfMap.getPath();
            // SCAN THE PDF FOR INFO

            //pb.setProgress(0);
            File file = new File(path);
            pb.setProgress(10);
            data=readPDF(file);
            pb.setProgress(30);
            String bounds = getBoundingBox(file);
            if (bounds.equals("")) {
                Toast.makeText(c, "The file is NOT a Geospatial PDF.", Toast.LENGTH_LONG).show();
                pb.setVisibility(View.INVISIBLE);
                removeItem(pos);
                return;
            }
            pb.setProgress(50);
            String mediaBox = getMediaBox(file);
            if (mediaBox.equals("")){
                Toast.makeText(c, "The file is NOT a Geospatial PDF.", Toast.LENGTH_LONG).show();
                pb.setVisibility(View.INVISIBLE);
                removeItem(pos);
                return;
            }
            pb.setProgress(60);
            String viewPort  = getViewPort(file);
            if (viewPort.equals("")) {
                Toast.makeText(c, "The file is NOT a Geospatial PDF.", Toast.LENGTH_LONG).show();
                pb.setVisibility(View.INVISIBLE);
                removeItem(pos);
                return;
            }
            boolean bestQuality = false;

            // get thumbnail image
            pb.setProgress(75);
            byte[] thumbnail;
            int pageNum = 0;
            PdfiumCore pdfiumCore = new PdfiumCore(c);

            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            pdfiumCore.openPage(pdfDocument, pageNum);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

            // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
            // RGB_565 - little worse quality, twice less memory usage
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                    width, height);
            //if you need to render annotations and form fields, you can use
            //the same method above adding 'true' as last param

            // convert from bitmap to byte array
            pb.setProgress(80);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            thumbnail = stream.toByteArray();

            // Get Map Name
            String name = file.getName();

            // convert from byte array to bitmap
            //public static Bitmap getImage(byte[] image) {
            //    return BitmapFactory.decodeByteArray(image, 0, image.length);
            //}

            // IMPORT INTO the DATABASE
            pdfMap.setName(name);
            pdfMap.setThumbnail(thumbnail);
            pdfMap.setViewport(viewPort);
            pdfMap.setMediabox(mediaBox);
            pdfMap.setBounds(bounds);
            pb.setProgress(90);
            DBHandler db = DBHandler.getInstance(c);
            db.updateMap(pdfMap);

            // LOAD THE MAP????
            pb.setProgress(100);
            pb.setVisibility(View.INVISIBLE);
        }
        catch(IOException ex) {
            Toast.makeText(c, "Could not get thumbnail. Failed to load Geospatial PDF.", Toast.LENGTH_LONG).show();
            pb.setVisibility(View.INVISIBLE);
            ex.printStackTrace();
        }
    }*/
}