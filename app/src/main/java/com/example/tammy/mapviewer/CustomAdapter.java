package com.example.tammy.mapviewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfString;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by tammy on 9/7/2017.
 * List of Geo PDF Maps that have been scanned for geo bounds, margins, page size in pixels.
 * This info is stored in a database. The list is displayed in MainActivity.
 * On click displays a selected Geo PDF Map in PDFActivity.
 * Uses itextpdf and pdfium to read the PDF.
 */

public class CustomAdapter extends BaseAdapter {
    private final Context c;
    ArrayList<PDFMap> pdfMaps;
    //private int vis, hide;
    // EditText renameTxt;
    TextView nameTxt;
    TextView fileSizeTxt;
    TextView distToMapTxt;
    ImageView locIcon;
    Double latNow, longNow;
    static String viewport, mediabox, bounds;
    private Boolean loading = false;
    //final private String TAG = "CustomAdapter"; // debug logs


    public CustomAdapter(Context c, ArrayList<PDFMap> pdfMaps) {
        this.c = c;
        this.pdfMaps = pdfMaps;
    }

    public void SortByName(){
        // Sort array list pdfMaps of objects of type pdfMap alphabetically by name.
        Collections.sort((this.pdfMaps), PDFMap.NameComparator);
    }

    public void SortByNameReverse(){
        // Sort array list pdfMaps of objects of type pdfMap alphabetically by name.
        Collections.sort((this.pdfMaps), PDFMap.NameComparatorReverse);
    }

    public void SortByDate(){
        // Sort array list pdfMaps of objects of type pdfMap file modification date.
        Collections.sort((this.pdfMaps), PDFMap.DateComparator);
    }

    public void SortByDateReverse(){
        // Sort array list pdfMaps of objects of type pdfMap file modification date.
        Collections.sort((this.pdfMaps), PDFMap.DateComparatorReverse);
    }

    public void SortBySize(){
        // Sort array list pdfMaps of objects of type pdfMap file size.
        Collections.sort((this.pdfMaps), PDFMap.SizeComparator);
    }

    public void SortBySizeReverse(){
        // Sort array list pdfMaps of objects of type pdfMap file size.
        Collections.sort((this.pdfMaps), PDFMap.SizeComparatorReverse);
    }

    public void SortByProximity(){
        // Sort array list pdfMaps of objects of type pdfMap by proximity.
        Collections.sort((this.pdfMaps), PDFMap.ProximityComparator);
    }

    public void SortByProximityReverse(){
        // Sort array list pdfMaps of objects of type pdfMap by proximity.
        Collections.sort((this.pdfMaps), PDFMap.ProximityComparatorReverse);
    }

    public void setLocation(Location location){
        latNow = location.getLatitude();
        longNow = location.getLongitude();
    }

    private static class ImportMapTask extends AsyncTask<Integer, Integer, String> {
        private final WeakReference<CustomAdapter> customAdapterRef;
        ProgressBar progressBar;
        String filePath;
        PDFMap pdfMap;

        ImportMapTask(CustomAdapter context, PDFMap pdfMap, ProgressBar pb) {
            // calls onPreExecute
            // only retain a weak reference to the CustomAdapter class
            customAdapterRef = new WeakReference<>(context);
            this.pdfMap = pdfMap;
            this.progressBar = pb;
            this.filePath = pdfMap.getPath();
        }

        //protected ImportMapTask(Context c, PDFMap pdfMap, ProgressBar pb) {
            //this.c = c;
            //this.pdfMap = pdfMap;
            //this.progressBar = pb;
            //this.path = pdfMap.getPath();
            // calls onPreExecute
        //}

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // get a reference to the CustomAdapter if it is still there
            CustomAdapter caRef = customAdapterRef.get();
            Activity activity = (Activity) caRef.c;
            if (activity.isFinishing()) return;

            caRef.loading = true;
            // calls doInBackground
            // show progress bar
            progressBar.setVisibility(View.VISIBLE);
        }

       /* private boolean isInteger(String s) {
            // Given a string, return true if it only contains integers.
            boolean isValidInteger = false;
            try
            {
                parseInt(s);
                // s is a valid integer
                isValidInteger = true;
            }
            catch (NumberFormatException ex)
            {
                // s is not an integer
            }
            return isValidInteger;
        }*/

        @Override
        protected String doInBackground(Integer... params) {
            // get a reference to the CustomAdapter if it is still there
            CustomAdapter caRef = customAdapterRef.get();
            Activity activity = (Activity) caRef.c;
            if (activity.isFinishing()) return "";
            Context c = caRef.c;

            // preform background computation
            publishProgress(10); // calls onProgressUpdate
            // Get PDF
            File file = new File(filePath);

            // Use iText 5 to read margins, page size, and lat long. iText 7 works with Android 26 and above. iText 5 works with older versions.
            // PDF ISO standard.

            // iText does this for us:
            // Go to the end of the pdf file, back up 1mb and search for startxref. Read the offset to the first xref table.
            // startxref
            // offset number to first xref table
            //%%EOF
            // Read xref table containing offsets to each object in the file. This is the format:
            // xref
            // 0 104
            // 0000000000 65535 f
            // 0000000010 00000 n
            // 0000973188 00000 n
            // ...
            // trailer
            // <<
            // /Size 104
            // /Info 103 0 R
            // /Root 102 0 R         Pointer to object 102. Points to /Pages --> /Kids --> /Contents /BBox /MediaBox
            // /Prev 2874274         May contain /Prev if there is another xref table.
            // >>
            // Read the trailer. /Root points to an object number, "5 0 R"  is the object ID, 0 is the generation number, R = Reference to object.
            // /Root 5 0 R --> .../Pages 1 0 R --> /Kids[15 0 R  16 0 R] --> /Contents.../MediaBox[...].../BBox[...].../Measure 79 0 R --> /Bounds[ ].../GPTS[lat long]/GCS 80 0 R --> GPS projection, ,UNIT["Degree",0.01745]
            // trailer
            //<</Size 82/Root 5 0 R/Info 3 0 R/ID[<481274B989C1D7419BA9E71CBA227123><D6AEE54D32AC354E980F653350D6C962>]/Prev 2874274>>
            try {
                PdfReader reader = new PdfReader(filePath);
                //if (reader == null) return ("Import Failed");

                //int numPages = reader.getNumberOfPages();

                PdfDictionary page = reader.getPageN(1);
                if (page == null) return ("Import Failed");
                PdfArray vp = page.getAsArray(PdfName.VP);
                // If this is PDF ISO standard file and not a GeoPDF check the version number
                if (vp != null){
                    if (Character.getNumericValue(reader.getPdfVersion()) < 6) {
                        // Version less than PDF 1.6
                        return("Not Georeferenced");
                    }
                }

                //--------------------------
                // Get MediaBox page size
                //--------------------------
                mediabox = page.getAsArray(PdfName.MEDIABOX).toString(); // works [ 0 0 792 1224]
                //if (mediabox == null) return ("Import Failed");
                mediabox = mediabox.substring(1,mediabox.length()-1).trim();
                mediabox = mediabox.replaceAll(",","");
                publishProgress(20);

                // ---------------------------------------------------------------------------
                // Get viewport, margins for ISO Standard if vp does not exist it is a GeoPDF
                //----------------------------------------------------------------------------
                if (vp != null) {
                    // Find largest image it should be the map
                    String[] units;
                    int id = 0;
                    double max = 0.0;
                    for (int i = 0; i < vp.size(); i++) {
                        PdfArray b = vp.getAsDict(i).getAsArray(PdfName.BBOX);
                        String parse = b.toString().trim();
                        parse = parse.substring(1, parse.length() - 1);
                        parse = parse.replaceAll(",", "");
                        int pos = parse.indexOf(" ");
                       // double bBoxX1 = Double.parseDouble(parse.substring(0, pos));
                        // FIND bBoxY1
                        parse = parse.substring(pos + 1); // strip off 'bBoxX1 '
                        pos = parse.indexOf(" ");
                        double bBoxY1 = Double.parseDouble(parse.substring(0, pos));
                        // FIND bBoxX2
                        parse = parse.substring(pos + 1); // strip off 'bBoxY1 '
                        pos = parse.indexOf(" ");
                        //double bBoxX2 = Double.parseDouble(parse.substring(0, pos));
                        // FIND bBoxY2
                        parse = parse.substring(pos + 1); // strip off 'bBoxX2 '
                        pos = parse.indexOf(" ");
                        double bBoxY2 = Double.parseDouble(parse.substring(pos + 1));
                        double h;
                        if (bBoxY1 > bBoxY2) h = bBoxY1 - bBoxY2;
                        else h = bBoxY2 - bBoxY1;
                        if (max < h) {
                            max = h;
                            id = i;
                        }
                    }
                    PdfDictionary vpDict = vp.getAsDict(id);
                    if (vpDict == null) return ("Import Failed");
                    PdfArray bbox = vpDict.getAsArray(PdfName.BBOX);
                    if (bbox == null) return ("Import Failed");
                    viewport = bbox.toString().trim();
                    viewport = viewport.substring(1, viewport.length() - 1);
                    viewport = viewport.replaceAll(",", "");
                    publishProgress(30);

                    String[] bboxArr = viewport.split(" ");
                    //---------------------------------------------
                    // Get Lat Long from /Measure dictionary /GPTS
                    //---------------------------------------------
                    PdfDictionary measure = vpDict.getAsDict(PdfName.MEASURE);
                    // get unit box correction for BBox if [0,1, 0,0, 1,0, 1,1] needs no correction
                    String unitBox = measure.get(PdfName.BOUNDS).toString();
                    unitBox = unitBox.trim();
                    unitBox = unitBox.substring(1,unitBox.length() -1);
                    unitBox = unitBox.replaceAll(",", ""); // remove commas
                    units = unitBox.split(" ");


                    // adjust bbox by unitBox
                    // top-left
                   /* Double bboxX1 = Double.valueOf(bboxArr[0]) + (Double.valueOf(bboxArr[2]) - Double.valueOf(bboxArr[0])) * (Double.valueOf(units[6]) - 1);
                    Double bboxY1 = Double.valueOf(bboxArr[1]) + (Double.valueOf(bboxArr[3]) - Double.valueOf(bboxArr[1])) * Double.valueOf(units[5]);
                    // bottom-left map boundary * unit square
                    Double bboxX2 = Double.valueOf(bboxArr[2]) + (Double.valueOf(bboxArr[2]) - Double.valueOf(bboxArr[0])) * Double.valueOf(units[2]);
                    Double bboxY2 = Double.valueOf(bboxArr[3]) + (Double.valueOf(bboxArr[3]) - Double.valueOf(bboxArr[1])) * (Double.valueOf(units[1]) - 1);*/
                    double bboxX1 = Double.parseDouble(bboxArr[0]) + ((Double.parseDouble(units[0]) - 0) * Double.parseDouble(bboxArr[0]));
                    double bboxY1 = Double.parseDouble(bboxArr[1]) + ((Double.parseDouble(units[1]) - 1) * Double.parseDouble(bboxArr[1]));
                    // bottom-right map boundary * unit square
                    double bboxX2 = Double.parseDouble(bboxArr[2]) + ((Double.parseDouble(units[4]) - 1) * Double.parseDouble(bboxArr[2]));
                    double bboxY2 = Double.parseDouble(bboxArr[3]) + ((Double.parseDouble(units[5]) - 0) * Double.parseDouble(bboxArr[3]));
                    Log.d("viewport", viewport);
                    // Adjust view port by unit box???????
                    viewport = bboxX1+" "+bboxY1+" "+bboxX2+" "+bboxY2;
                    Log.d("adjusted viewport", viewport);




                    bounds = measure.get(PdfName.GPTS).toString();
                    //if (bounds == null) return ("Import Failed");
                    bounds = bounds.trim();
                    bounds = bounds.substring(1, bounds.length() - 1);
                    bounds = bounds.replaceAll(",", "");
                    String[] latlong;
                    latlong = bounds.split(" ");
                    // adjusted with the unit square - gives the correct lat long for BBox
                    // bottom-left lat/long (given long + (height or width in decimal degrees) * unit square value
                    double lat1 = Double.parseDouble(latlong[0]) + (Double.parseDouble(latlong[2]) - Double.parseDouble(latlong[0])) * Double.parseDouble(units[0]);
                    double long1 = Double.parseDouble(latlong[1]) + (Double.parseDouble(latlong[7]) - Double.parseDouble(latlong[1])) * (Double.parseDouble(units[1]) - 1);
                    // top-left lat/long (given lat + (height or width in decimal degrees) * unit square value
                    double lat2 = Double.parseDouble(latlong[2]) + (Double.parseDouble(latlong[2]) - Double.parseDouble(latlong[0])) * Double.parseDouble(units[2]);
                    double long2 = Double.parseDouble(latlong[3]) + (Double.parseDouble(latlong[5]) - Double.parseDouble(latlong[3])) * Double.parseDouble(units[3]);

                    // top-right lat/long (given lat + (height or width in decimal degrees) * unit square value
                    double lat3 = Double.parseDouble(latlong[4]) + (Double.parseDouble(latlong[4]) - Double.parseDouble(latlong[6])) * Double.parseDouble(units[4]);
                    double long3 = Double.parseDouble(latlong[5]) + (Double.parseDouble(latlong[5]) - Double.parseDouble(latlong[3])) * Double.parseDouble(units[5]);
                    // bottom-right lat/long (given lat + (height or width in decimal degrees) * unit square value
                    double lat4 = Double.parseDouble(latlong[6]) + (Double.parseDouble(latlong[4]) - Double.parseDouble(latlong[4])) * (Double.parseDouble(units[6]) - 1);
                    double long4 = Double.parseDouble(latlong[7]) + (Double.parseDouble(latlong[1]) - Double.parseDouble(latlong[5])) * Double.parseDouble(units[7]);
                    Log.d("adjusted lat/long", lat1+", "+long1+ "  "+lat2+", "+long2+"  "+lat3+", "+long3+"  "+lat4+", "+long4);
                }
                // View port not found geoPDF
                else{
                    // /MediaBox [0,0,1638,2088]
                    // /LGIDict array of dictionaries
                    //      /CTM [a (scale), b, c, d (scale), H (horizontal map distance in meters), V (vertical map distance in meters)]
                    //      /Neatline  array of page margins in points 1/72 of an inch [h2, v1, h1, v1, h1, v2, h2, v2, h2, v1]
                    //      /Display dictionary of /Projection (optional but will have /Projection instead)
                    //          /Projection dictionary
                    //              /ProjectionType "UT" UTM works need zone
                    //              /Zone "12" or "13"
                    //              /Units "m"
                    //      /Projection dictionary of /Projection (optional but will have /Display instead)
                    //          /Projection dictionary
                    //              /ProjectionType "UT" UTM works need zone
                    //              /Zone "12" or "13"
                    //              /Units "m"
                    PdfName lgiDict = new PdfName("LGIDict");
                    PdfName ctm = new PdfName("CTM");
                    //PdfName desc = new PdfName("Description");
                    PdfName display = new PdfName("Display");
                    PdfName projection = new PdfName("Projection");
                    PdfName projectionType = new PdfName("ProjectionType");
                    PdfName PdfNameZone = new PdfName("Zone");
                    PdfName PdfNameUnits = new PdfName("Units");
                    //PdfName lgitinfo = new PdfName("LGIT:Info");
                    PdfName neatline = new PdfName("Neatline");
                    PdfArray lgiDictArray = page.getAsArray(lgiDict);
                    if (lgiDictArray == null)
                        return "Import Failed - no LGIDict dictionary";

                    int max=0;
                    int id=0;
                    PdfDictionary lgiDictionary;
                    // Select LGIDict dictionary with largest vertical area
                    for (int i=0; i<lgiDictArray.size(); i++) {
                        lgiDictionary = lgiDictArray.getAsDict(i);
                        PdfArray neatArray = lgiDictionary.getAsArray(neatline);
                        int v1 = Math.round(Float.parseFloat(neatArray.getAsString(1).toString()));
                        int v2 = Math.round(Float.parseFloat(neatArray.getAsString(3).toString()));
                        if (v1 == v2) v2 = Math.round(Float.parseFloat(neatArray.getAsString(5).toString()));
                        if (v1 < v2){
                            int tmp;
                            tmp = v1;
                            v1 = v2;
                            v2 = tmp;
                        }
                        int thisMax = v1 - v2;
                        if (thisMax > max){
                            max = thisMax;
                            id = i;
                        }
                    }
                    lgiDictionary = lgiDictArray.getAsDict(id);

                    PdfDictionary displayDict = lgiDictionary.getAsDict(display);
                    PdfString projType;
                    PdfDictionary projDict;
                    int zone=13;
                    PdfString units;
                    // Working for projection type UTM, units meters
                    if (displayDict == null){
                        projDict = lgiDictionary.getAsDict(projection);
                        projType = projDict.getAsString(projectionType);
                        if (!projType.toString().toLowerCase().equals("ut")) return "Import Failed - unhandled projection "+projType.toString();
                        units = projDict.getAsString(PdfNameUnits);
                        if (!units.toString().toLowerCase().equals("m")) return "Import Failed - unknown unit "+units.toString();
                        PdfNumber z = projDict.getAsNumber(PdfNameZone);
                        if (z != null)
                            zone = Integer.parseInt(z.toString());
                    }
                    else{
                        projType = displayDict.getAsString(projectionType);
                        if (!projType.toString().toLowerCase().equals("ut")) return "Import Failed - unhandled projection "+projType.toString();
                        units = displayDict.getAsString(PdfNameUnits);
                        if (!units.toString().toLowerCase().equals("m")) return "Import Failed - unknown unit "+units.toString();
                        PdfNumber z = displayDict.getAsNumber(PdfNameZone);
                        if (z != null)
                            zone = Integer.parseInt(z.toString());
                    }

                    // Get View Port
                    // This works for projType == UT (for UTM) units == m
                    PdfArray neatArray = lgiDictionary.getAsArray(neatline);
                    int h1 = Math.round(Float.parseFloat(neatArray.getAsString(0).toString()));
                    int v1 = Math.round(Float.parseFloat(neatArray.getAsString(1).toString()));
                    int h2 = Math.round(Float.parseFloat(neatArray.getAsString(2).toString()));
                    int v2 = Math.round(Float.parseFloat(neatArray.getAsString(3).toString()));
                    if (h1 == h2) h2 = Math.round(Float.parseFloat(neatArray.getAsString(4).toString()));
                    if (v1 == v2) v2 = Math.round(Float.parseFloat(neatArray.getAsString(5).toString()));
                    if (h2 < h1){
                        int tmp;
                        tmp = h1;
                        h1 = h2;
                        h2 = tmp;
                    }
                    if (v1 < v2){
                        int tmp;
                        tmp = v1;
                        v1 = v2;
                        v2 = tmp;
                    }
                    viewport = h1 + " " + v1 + " " + h2 + " " + v2;
                    publishProgress(20);

                    // Get Latitude/Longitude Bounds = lat1 long1 lat2 long1 lat2 long2 lat1 long2
                    PdfArray ctmArray = lgiDictionary.getAsArray(ctm);
                    double a = Double.parseDouble(ctmArray.getAsString(0).toString()); // scale (x2 - x1) / (h2 - h1)
                    double H = Double.parseDouble(ctmArray.getAsString(4).toString()); // H = x1 - a * h1
                    double V = Double.parseDouble(ctmArray.getAsString(5).toString()); // V = y1 - a * v1
                    double x1 = H + a * h1; // meters
                    double y1 = V + a * v1; // meters
                    double x2 = H + a * h2; // meters
                    double y2 = V + a * v2; // meters
                    double [] latlong1 = UTMtoLL(y1,x1,zone);
                    double [] latlong2 = UTMtoLL(y2,x2,zone);
                    bounds = String.format("%s %s %s %s %s %s", latlong2[1], latlong1[0], latlong1[1], latlong1[0], latlong1[1], latlong2[0]);
                    // this one does not work!!! Can't add way points
                    //bounds = String.valueOf(latlong1[1]) +" "+ String.valueOf(latlong1[0]) +" "+ String.valueOf(latlong2[1]) +" "+ String.valueOf(latlong1[0]) +" "+ String.valueOf(latlong2[1]) +" "+ String.valueOf(latlong2[0]);

                    //String description = lgiDictionary.getAsString(desc).toString();
                }
                publishProgress(40);
                reader.close();

            }catch(IOException ex){
                //Toast.makeText(c, "Trouble reading PDF. " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return ("Import Failed");
            }

            // ---------------------
            //  get thumbnail image
            // --------------------
            byte[] thumbnail;
            int pageNum = 0;
            PdfiumCore pdfiumCore = new PdfiumCore(c);
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                publishProgress(50);
                pdfiumCore.openPage(pdfDocument, pageNum);

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

                // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
                // RGB_565 - little worse quality, twice less memory usage
                Bitmap bitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.RGB_565);
                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                        width, height);
                publishProgress(60);

                // scale it down
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,100, (int) Math.round(100.0 * (double)height/(double)width),false);
                //if you need to render annotations and form fields, you can use
                //the same method above adding 'true' as last param
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0, stream);
                publishProgress(70);
                thumbnail = stream.toByteArray();
                bitmap.recycle(); // free memory
                scaled.recycle();

                // Save thumbnail to a file in app directory (/data/data/com.example.tammy.pdfsample/files), save the path to it.
                File img;
                try {
                    publishProgress(80);
                    // Save thumbnail image in local storage where this App is.
                    String path = c.getFilesDir().getAbsolutePath();
                    img = new File(path + "/CPWthumbnail" + pdfMap.getId() + ".png");
                    if (!img.exists()) {
                            boolean fileDoesNotExist = img.createNewFile();
                            if (!fileDoesNotExist)
                                Toast.makeText(c, "Trouble saving map thumbnail. Could not create new file.", Toast.LENGTH_LONG).show();
                    }
                    FileOutputStream fos = new FileOutputStream(img);
                    fos.write(thumbnail);
                    fos.close();
                    pdfMap.setThumbnail(path + "/CPWthumbnail" + pdfMap.getId() + ".png");
                } catch (IOException e){
                    Toast.makeText(c, "Trouble saving map thumbnail. Disk full?" + e.getMessage(), Toast.LENGTH_LONG).show();
                    pdfMap.setThumbnail(null);
                } catch (SecurityException e){
                    Toast.makeText(c, "Trouble saving map thumbnail. Security exception." + e.getMessage(), Toast.LENGTH_LONG).show();
                    pdfMap.setThumbnail(null);
                } catch (Exception e) {
                    Toast.makeText(c, "Trouble saving map thumbnail. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    pdfMap.setThumbnail(null);
                }
                publishProgress(60);

                // Get Map Name
                String name = file.getName();
                publishProgress(80);

                // Get File Size
                String fileSize;
                double size = file.length() / 1024.0; // Get size and convert bytes into Kb.
                if (size >= 1024) {
                    fileSize = String.format(Locale.ENGLISH,"%.1f %s", (size / 1024.0), c.getResources().getString(R.string.Mb));
                } else {
                    fileSize = String.format("%.0f %s", size, c.getResources().getString(R.string.Kb));
                }

                // IMPORT INTO the DATABASE
                pdfMap.setName(name);
                pdfMap.setFileSize(fileSize);
                //pdfMap.setThumbnail(thumbnail);
                pdfMap.setViewport(viewport);
                pdfMap.setMediabox(mediabox);
                pdfMap.setBounds(bounds);
                publishProgress(90);
                DBHandler db = DBHandler.getInstance(c);
                db.updateMap(pdfMap);
                db.close();
                publishProgress(100);
                fd.close(); // thumbnail file descriptor
            } catch (IOException ex) {
                ex.printStackTrace();
                pdfMap.setName("deleting...");
                return "Import Failed " + ex.getMessage();
            }
            return "Import Done";
        }

        // 10-10-18 Test getting thumbnail from iText
       /* private List<BufferedImage> FindImages(PdfReader reader, PdfDictionary pdfPage) throws IOException
        {
            List<BufferedImage> result = new ArrayList<>();
            Iterable<PdfObject> imgPdfObject = FindImageInPDFDictionary(pdfPage);
            for (PdfObject image : imgPdfObject)
            {
                int xrefIndex = ((PRIndirectReference)image).getNumber();
                PdfObject stream = reader.getPdfObject(xrefIndex);
                // Exception occurs here :
                PdfImageObject pdfImage = new PdfImageObject((PRStream)stream);
                BufferedImage img = pdfImage.getBufferedImage();

                // Do something with the image
                result.add(img);
            }
            return result;
        }*/

       private double[] UTMtoLL(double f, double f1, int j) {
           // Convert UTM to Lat Long return [lat, long]
           // UTM=f,f1 Zone=j Colorado is mostly 13 and a little 12
            double d = 0.99960000000000004;
            double d1 = 6378137;
            double d2 = 0.0066943799999999998;

            double d4 = (1 - Math.sqrt(1-d2))/(1 + Math.sqrt(1 - d2));
            double d15 = f1 - 500000;
            //double d16 = f;
            double d11 = ((j - 1) * 6 - 180) + 3;

            double d3 = d2/(1 - d2);
            double d10 = f / d;
            double d12 = d10 / (d1 * (1 - d2/4 - (3 * d2 *d2)/64 - (5 * Math.pow(d2,3))/256));
            double d14 = d12 + ((3*d4)/2 - (27*Math.pow(d4,3))/32) * Math.sin(2*d12) + ((21*d4*d4)/16 - (55 * Math.pow(d4,4))/32) * Math.sin(4*d12) + ((151 * Math.pow(d4,3))/96) * Math.sin(6*d12);
            //double d13 = d14 * 180 / Math.PI;
            double d13a = 1 - d2 * Math.sin(d14) * Math.sin(d14); // tlb store since it is used twice
            double d5 = d1 / Math.sqrt(d13a);
            double d6 = Math.tan(d14)*Math.tan(d14);
            double d7 = d3 * Math.cos(d14) * Math.cos(d14);
            double d8 = (d1 * (1 - d2))/Math.pow(d13a,1.5);

            double d9 = d15/(d5 * d);
            double d17 = d14 - ((d5 * Math.tan(d14))/d8)*(((d9*d9)/2-(((5 + 3*d6 + 10*d7) - 4*d7*d7-9*d3)*Math.pow(d9,4))/24) + (((61 +90*d6 + 298*d7 + 45*d6*d6) - 252*d3 -3 * d7 *d7) * Math.pow(d9,6))/720);
            d17 = d17 * 180 / Math.PI;
            double d18 = ((d9 - ((1 + 2 * d6 + d7) * Math.pow(d9,3))/6) + (((((5 - 2 * d7) + 28*d6) - 3 * d7 * d7) + 8 * d3 + 24 * d6 * d6) * Math.pow(d9,5))/120)/Math.cos(d14);
            d18 = d11 + d18 * 180 / Math.PI;
           return new double[]{d18,d17};
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // update the progress bar. The value you pass in publishProgress
            // is passed in the values parameter of this method
            super.onProgressUpdate(progress);
            progressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(String result) {
            // result of background computation is sent here
            // get a reference to the CustomAdapter if it is still there
            CustomAdapter caRef = customAdapterRef.get();
            Activity activity = (Activity) caRef.c;
            if (activity.isFinishing()) return;

            progressBar.setVisibility(View.GONE);
            caRef.loading = false;
            // Map Import Failed
            if (!result.equals("Import Done")) {
                Toast.makeText(caRef.c, result, Toast.LENGTH_LONG).show();
                caRef.removeItem(pdfMap.getId());
            }
            // Map Import Success
            else {
                // Display message and load map
                //Toast.makeText(c, "Map copied to App folder.", Toast.LENGTH_LONG).show();
                //notifyDataSetChanged(); // Refresh list of pdf maps
                caRef.openPDFView(pdfMap.getPath(), pdfMap.getName(), pdfMap.getBounds(), pdfMap.getMediabox(), pdfMap.getViewport());
            }
        }
    }

    public void checkIfExists() {
        // Check if the pdf exists in App directory. If not remove it from the database. Called by MainActivity.
        try {
            DBHandler db = DBHandler.getInstance(c);
            DBWayPtHandler wpdb = DBWayPtHandler.getInstance(c);
            for (int i = 0; i < pdfMaps.size(); i++) {
                PDFMap map = pdfMaps.get(i);
                File file = new File(map.getPath());
                if (!file.exists()) {
                    Toast.makeText(c, "File, " + map.getName() + " no longer exists. Updating database...", Toast.LENGTH_LONG).show();
                    File f = new File(map.getPath());
                    if (f.exists()) {
                        boolean result = f.delete();
                        if (!result){
                            Toast.makeText(c, "Problem removing map.", Toast.LENGTH_LONG).show();
                        }
                    }
                    db.deleteMap(map);
                    wpdb.deleteWayPt(pdfMaps.get(i).getName());
                    pdfMaps.remove(i);
                    // delete thumbnail image also
                    if (map.getThumbnail() != null) {
                        File img = new File(map.getThumbnail());
                        if (img.exists()) {
                            boolean result2 = img.delete();
                            if (!result2)
                                Toast.makeText(c, "Problem removing thumbnail.", Toast.LENGTH_LONG).show();
                        }
                    }
                    i--;
                }
            }
            notifyDataSetChanged();
            db.close();
            wpdb.close();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(c, "Problem removing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void getDistToMap() {
        // calculate updated distances for all cells
        // Called from MainActivity onLocationResult when user location has changed by 1/10 of a mile
        // Or when latBefore is 0.0.
        if (latNow == null || longNow == null) {
            return;
        }
        for (int i=0; i<pdfMaps.size(); i++) {
            PDFMap map = pdfMaps.get(i);

            String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
            if (bounds == null || bounds.length() == 0)
                return; // it will be 0 length if is importing
            bounds = bounds.trim(); // remove leading and trailing spaces
            int pos = bounds.indexOf(" ");
            double lat1 = Double.parseDouble(bounds.substring(0, pos));
            bounds = bounds.substring(pos + 1); // strip off 'lat1 '
            pos = bounds.indexOf(" ");
            double long1 = Double.parseDouble(bounds.substring(0, pos));
            // FIND LAT2
            bounds = bounds.substring(pos + 1); // strip off 'long1 '
            pos = bounds.indexOf(" ");
            double lat2 = Double.parseDouble(bounds.substring(0, pos));
            // FIND LONG2
            pos = bounds.lastIndexOf(" ");
            double long2 = Double.parseDouble(bounds.substring(pos + 1));

            // Is on map?
            // On map
            if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2) {
                map.setMiles(0.0);
                map.setDistToMap("");
               // Log.d(TAG, "updateDistToMap: " + map.getName() + " on map");
            }
            // Off map, calculate distance away
            else {
                String direction;
                double dist;
                if (latNow > lat1) direction = "S";
                else if (latNow > lat2) direction = "";
                else direction = "N";
                if (longNow < long1) direction += "E";
                else if (longNow > long2) direction += "W";

                float[] results = new float[1];
                switch (direction) {
                    case "S":
                        Location.distanceBetween(latNow, longNow, lat2, longNow, results);
                        break;
                    case "N":
                        Location.distanceBetween(latNow, longNow, lat1, longNow, results);
                        break;
                    case "E":
                        Location.distanceBetween(latNow, longNow, latNow, long2, results);
                        break;
                    case "W":
                        Location.distanceBetween(latNow, longNow, latNow, long1, results);
                        break;
                    case "SE":
                        Location.distanceBetween(latNow, longNow, lat2, long2, results);
                        break;
                    case "SW":
                        Location.distanceBetween(latNow, longNow, lat1, long2, results);
                        break;
                    case "NE":
                        Location.distanceBetween(latNow, longNow, lat2, long1, results);
                        break;
                    case "NW":
                        Location.distanceBetween(latNow, longNow, lat1, long1, results);
                        break;
                }
                dist = results[0] * 0.00062137119;
                map.setMiles(dist);
                String str = "    ";

                String distStr = String.format(Locale.ENGLISH,"%s %.1f %s %s", str, dist, c.getResources().getString(R.string.miles), direction);
                map.setDistToMap(distStr);

               // Log.d(TAG, "updateDistToMap: " + map.getName() + " " + map.getDistToMap());
            }
        }
    }

    @Override
    public int getCount() {
        return pdfMaps.size();
    }

    @Override
    public Object getItem(int i) {
        return pdfMaps.get(i);
    }

    public void add(PDFMap pdfMap) {
        pdfMaps.add(pdfMap);
    }

    public void rename(int id, String name) {
        // Rename an imported map
        try {
            DBHandler db = DBHandler.getInstance(c);
            PDFMap map;
            for (int i = 0; i < pdfMaps.size(); i++) {
                if (pdfMaps.get(i).getId() == id) {
                    map = pdfMaps.get(i);
                    try {
                        // Check if name has change. If not return
                        if (name.equals(map.getName())) return;
                        File sdcard = c.getFilesDir();
                        File file = new File(map.getPath());
                        String fileName = name;
                        if (!name.endsWith(".pdf"))
                            fileName = name + ".pdf";
                        File newName = new File(sdcard, fileName);
                        boolean result = file.renameTo(newName);
                        if (!result)
                            Toast.makeText(c, "Can't rename to: " + name, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(c, "Map renamed to: " + name, Toast.LENGTH_LONG).show();
                        map.setName(name);
                        map.setPath(sdcard + "/" + fileName);
                        db.updateMap(map);
                    } catch (Exception e) {
                        Toast.makeText(c, "Error renaming: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    notifyDataSetChanged();
                    break;
                }
            }
            db.close();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(c, "Problem renaming map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public long getItemId(int i) {
        return i;
    }

    public void removeAll() {
        // remove all maps from the list and the database
        try {
            DBHandler db = DBHandler.getInstance(c);
            for (int i = pdfMaps.size()-1; i > -1;  i--) {
                PDFMap map = pdfMaps.get(i);
                Toast.makeText(c,"Deleting: "+map.getName(), Toast.LENGTH_LONG).show();
                File f = new File(map.getPath());
                if (f.exists()) {
                    boolean deleted = f.delete();
                    if (!deleted) {
                        Toast.makeText(c, c.getResources().getString(R.string.deleteFile), Toast.LENGTH_LONG).show();
                    }
                }
                db.deleteMap(map);
                pdfMaps.remove(i);
                // delete thumbnail image also
                File img = new File(map.getThumbnail());
                if (img.exists()) {
                    boolean deleted = img.delete();
                    if (!deleted) {
                        Toast.makeText(c, c.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                    }
                }
            }
            notifyDataSetChanged();
            db.close();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(c, "Problem removing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void removeItem(int id) {
        // remove item i from the list and the database
        try {
            DBHandler db = DBHandler.getInstance(c);
            DBWayPtHandler dbwaypt = DBWayPtHandler.getInstance(c);
            for (int i = 0; i < this.pdfMaps.size(); i++) {
                if (pdfMaps.get(i).getId() == id) {
                    PDFMap map = pdfMaps.get(i);
                    File f = new File(map.getPath());
                    if (f.exists()) {
                        boolean deleted = f.delete();
                        if (!deleted) {
                            Toast.makeText(c, c.getResources().getString(R.string.deleteFile), Toast.LENGTH_LONG).show();
                        }
                    }
                    //Toast.makeText(c,"Deleting: "+map.getName(), Toast.LENGTH_LONG).show();
                    db.deleteMap(map);
                    dbwaypt.deleteWayPts(map.getName());
                    pdfMaps.remove(i);
                    // delete thumbnail image also
                    String imgPath = map.getThumbnail();
                    if (imgPath != null) {
                        File img = new File(imgPath);
                        if (img.exists()) {
                            boolean deleted = img.delete();
                            if (!deleted) {
                                Toast.makeText(c, c.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    notifyDataSetChanged();
                    break;
                }
            }
            db.close();
            dbwaypt.close();
        } catch (IndexOutOfBoundsException | SQLException e) {
            Toast.makeText(c, c.getResources().getString(R.string.problemRemovingMap) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            // INFLATE CUSTOM LAYOUT
            view = LayoutInflater.from(c).inflate(R.layout.model, viewGroup, false);
        }
        view.setBackgroundResource(android.R.color.transparent);
        view.setClickable(true);
        view.setLongClickable(true);
        final PDFMap pdfMap = (PDFMap) this.getItem(i);

        // renameTxt = (EditText) view.findViewById(R.id.rename);
        //vis = View.VISIBLE;
        //hide = View.GONE;
        nameTxt = view.findViewById(R.id.nameTxt);
        fileSizeTxt = view.findViewById(R.id.fileSizeTxt);
        distToMapTxt = view.findViewById(R.id.distToMapTxt);
        locIcon = view.findViewById(R.id.locationIcon);
        locIcon.setVisibility(View.GONE);
        ImageView img = view.findViewById(R.id.pdfImage);
        ProgressBar pb = view.findViewById(R.id.loadProgress);
        pb.setVisibility(View.GONE);

        //  load thumbnail from file
        String path = pdfMap.getThumbnail();
        if (path == null) img.setImageResource(R.drawable.pdf_icon);
        else {
            try {
                File imgFile = new File(pdfMap.getThumbnail());
                Bitmap myBitmap;
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (myBitmap != null)
                    img.setImageBitmap(myBitmap);
                else
                    img.setImageResource(R.drawable.pdf_icon);
            }catch (Exception ex){
                Toast.makeText(c, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                img.setImageResource(R.drawable.pdf_icon);
            }

        }
        // BIND DATA
        // Read lat/long bounds, margins, thumbnail and write to database

        if (pdfMap.getName().equals(c.getResources().getString(R.string.loading))) {
            nameTxt.setText(c.getResources().getString(R.string.loading));
            // call AsyncTask to read pdf binary and update progress bar and import into database
            if (!loading) {
                ImportMapTask importMap = new ImportMapTask(this, pdfMap, pb);
                importMap.execute();
            }
        } else {
            nameTxt.setText(pdfMap.getName());
            fileSizeTxt.setText(pdfMap.getFileSize());
            // getDistToMap sets miles also
            distToMapTxt.setText(pdfMap.getDistToMap());
            if (latNow != null) {
                if (!pdfMap.getDistToMap().equals(""))
                    locIcon.setVisibility(View.GONE);
                else
                    locIcon.setVisibility(View.VISIBLE);
            } else
                locIcon.setVisibility(View.GONE);
        }

        // VIEW ITEM CLICK
        view.setOnClickListener(view1 -> {
            // Display the map
            String bounds = pdfMap.getBounds();
            if (bounds == null) {
                Toast.makeText(c, "This file is not a Geo PDF. Missing GPTS Bounds.", Toast.LENGTH_LONG).show();
                return;
            }
            String mediaBox = pdfMap.getMediabox();
            if (mediaBox == null) {
                Toast.makeText(c, "This file is not a Geo PDF. Missing MediaBox.", Toast.LENGTH_LONG).show();
                return;
            }
            String viewPort = pdfMap.getViewport();
            if (viewPort == null) {
                Toast.makeText(c, "This file is not a Geo PDF. Missing Viewport.", Toast.LENGTH_LONG).show();
                return;
            }
            openPDFView(pdfMap.getPath(), pdfMap.getName(), bounds, mediaBox, viewPort);
        });

        // ITEM LONG CLICK - show menu delete item, rename item
        view.setOnLongClickListener(view12 -> {
            // Open edit activity
            Intent i1 = new Intent(c, EditMapNameActivity.class);
            i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i1.putExtra("PATH", pdfMap.getPath());
            i1.putExtra("NAME", pdfMap.getName());
            i1.putExtra("BOUNDS", pdfMap.getBounds());
            i1.putExtra("ID", pdfMap.getId());
            i1.putExtra("IMG", pdfMap.getThumbnail());
            c.startActivity(i1);
            return true;
        });

        return view;
    }

    // EDIT MENU
 /*   private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
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
    */


    // OPEN PDF VIEW - load the map
    private void openPDFView(String path, String name, String bounds, String mediaBox, String viewPort) {
        Intent i = new Intent(c, PDFActivity.class);
        //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("PATH", path);
        i.putExtra("NAME", name);
        i.putExtra("BOUNDS", bounds);
        i.putExtra("MEDIABOX", mediaBox);
        i.putExtra("VIEWPORT", viewPort);
        c.startActivity(i);
    }
}