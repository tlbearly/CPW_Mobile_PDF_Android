package com.dnrcpw.cpwmobilepdf.activities;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.dnrcpw.cpwmobilepdf.model.PDFMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by tammy on 9/7/2017.
 * List of Geo PDF Maps that have been scanned for geo bounds, margins, page size in pixels.
 * This info is stored in a database. The list is displayed in MainActivity.
 * On click displays a selected Geo PDF Map in PDFActivity.
 * Uses itextpdf and pdfium to read the PDF.
 *
 *  TODO: Remove deprecated AsyncTask in ImportMapTask
 */

public class CustomAdapter extends BaseAdapter {
    private final Context c;
    ArrayList<PDFMap> pdfMaps;
    Double latNow, longNow;
    boolean editing = false;

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

    public Boolean getEditing() {
        return this.editing;
    }
    public void setEditing(Boolean value) {
        this.editing = value;
    }

    public void setLocation(Location location){
        latNow = location.getLatitude();
        longNow = location.getLongitude();
    }

    public void checkIfExists() {
        // Check if the pdf exists in App directory. If not remove it from the database. Called by MainActivity.
        try {
            DBHandler db = new DBHandler(c);
            DBWayPtHandler wpdb = new DBWayPtHandler(c);
            for (int i = 0; i < pdfMaps.size(); i++) {
                PDFMap map = pdfMaps.get(i);
                File file = new File(map.getPath());
                if (!file.exists()) {
                    //Log.d("CustomAdapter",c.getResources().getString(R.string.file) + map.getName() +  c.getResources().getString(R.string.noLongerExists));
                    File f = new File(map.getPath());
                    if (f.exists()) {
                        boolean result = f.delete();
                        if (!result){
                            Toast.makeText(c, c.getResources().getString(R.string.problemRemovingMap), Toast.LENGTH_LONG).show();
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
                                Toast.makeText(c, c.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                        }
                    }
                    i--;
                }
            }
            notifyDataSetChanged();
            db.close();
            wpdb.close();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(c,  c.getResources().getString(R.string.problemRemovingMap) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void getDistToMap() {
        // calculate updated distances for all cells
        // Called from MainActivity onLocationResult when user location has changed by 1/10 of a mile
        // Or when latBefore is 0.0.
        try {
            if (latNow == null || longNow == null) {
                return;
            }
            for (int i = 0; i < pdfMaps.size(); i++) {
                PDFMap map = pdfMaps.get(i);

                String bounds = map.getBounds(); // lat1 long1 lat2 long1 lat2 long2 lat1 long2
                if (bounds == null || bounds.length() == 0)
                    return; // it will be 0 length if is importing
                bounds = bounds.trim(); // remove leading and trailing spaces
           /* int pos = bounds.indexOf(" ");
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
            double long2 = Double.parseDouble(bounds.substring(pos + 1));*/

                //String strBounds = bounds;
                // Get Latitude, Longitude bounds.
                // lat1 and long1 are the smallest values SW corner
                // lat2 and long2 are the largest NE corner
                String[] arrLatLong = bounds.split(" ");
                // convert strings to double
                Double[] LatLong = new Double[arrLatLong.length];
                for (int l = 0; l < arrLatLong.length; l++) {
                    LatLong[l] = Double.parseDouble(arrLatLong[l]);
                }
                // Find the smallest and largest values
                double lat1 = LatLong[0];
                double long1 = LatLong[1];
                double lat2 = LatLong[0];
                double long2 = LatLong[1];
                for (int l = 0; l < LatLong.length; l = l + 2) {
                    if (LatLong[l] < lat1) lat1 = LatLong[l];
                    if (LatLong[l] > lat2) lat2 = LatLong[l];
                    if (LatLong[l + 1] < long1) long1 = LatLong[l + 1];
                    if (LatLong[l + 1] > long2) long2 = LatLong[l + 1];
                }

                // Is on map?
                // On map
                if (latNow >= lat1 && latNow <= lat2 && longNow >= long1 && longNow <= long2) {
                    map.setMiles(0.0);
                    map.setDistToMap("onmap");
                    // Log.d(TAG, "updateDistToMap: " + map.getName() + " on map");
                }
                // Off map, calculate distance away
                else {
                    String direction = "";
                    double dist;
                    if (latNow < lat1) direction = "N";
                    else if (latNow > lat2) direction = "S";
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
                            Location.distanceBetween(latNow, longNow, latNow, long1, results);
                            break;
                        case "W":
                            Location.distanceBetween(latNow, longNow, latNow, long2, results);
                            break;
                        case "SE":
                            Location.distanceBetween(latNow, longNow, lat2, long1, results);
                            break;
                        case "SW":
                            Location.distanceBetween(latNow, longNow, lat2, long2, results);
                            break;
                        case "NE":
                            Location.distanceBetween(latNow, longNow, lat1, long1, results);
                            break;
                        case "NW":
                            Location.distanceBetween(latNow, longNow, lat1, long2, results);
                            break;
                    }
                    String str = "    ";
                    String distStr;
                    dist = results[0] * 0.00062137119;
                    // use 2 decimal points
                    if (dist < 0.1){
                        map.setMiles(dist);
                        distStr = String.format(Locale.US, "%s %.2f %s %s", str, dist, c.getResources().getString(R.string.miles), direction);
                        map.setDistToMap(distStr);
                    }else {
                        // use 1 decimal point
                        map.setMiles(dist);
                        distStr = String.format(Locale.US, "%s %.1f %s %s", str, dist, c.getResources().getString(R.string.miles), direction);
                        map.setDistToMap(distStr);
                    }
                    //Log.d("CustomAdapter", "updateDistToMap: " + map.getName() + " " + map.getDistToMap());
                }
            }
        } catch (Exception e){
            Toast.makeText(c,  "Problem getting distance to map. " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    // return array of pdfMaps so that we do not need to get DBHandler or DBWayPtHander here 5-27-21
    //public ArrayList<PDFMap> getPdfMaps() {
    //    return pdfMaps;
    //}

    public void add(PDFMap pdfMap) {
        pdfMaps.add(pdfMap);
    }

    public void rename(int id, String name) {
        // Rename an imported map
        try {
            DBHandler db = new DBHandler(c);
            PDFMap map;
            for (int i = 0; i < pdfMaps.size(); i++) {
                if (pdfMaps.get(i).getId() == id) {
                    map = pdfMaps.get(i);
                    try {
                        // Check if name has change. If not return
                        //Log.d("CustomAdapter","Rename "+map.getName()+" to "+name);
                       // if (name.equals(map.getName())) return;
                        File sdcard = c.getFilesDir();
                        File file = new File(map.getPath());
                        String fileName = name;
                        if (!name.endsWith(".pdf"))
                            fileName = name + ".pdf";
                        File newName = new File(sdcard, fileName);
                        boolean result = file.renameTo(newName);
                        if (!result)
                            Toast.makeText(c, "Can't rename file.", Toast.LENGTH_LONG).show();
                        //Log.d("CustomAdapter", "Can't rename to: " + name);
                        //else
                        //Log.d("CustomAdapter", "Map renamed to: " + name);
                        map.setName(name);
                        map.setRename(name);
                        map.setPath(sdcard + "/" + fileName);
                        db.updateMap(map);
                    } catch (Exception e) {
                        db.close();
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
            //DBHandler db = DBHandler.getInstance(c);
            DBHandler db = new DBHandler(c);
            DBWayPtHandler wpdb = new DBWayPtHandler(c);
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
                wpdb.deleteWayPt(map.getName());// delete waypoints
                pdfMaps.remove(i);
                // delete thumbnail image also
                if (map.getThumbnail() != null) {
                    File img = new File(map.getThumbnail());
                    if (img.exists()) {
                        boolean deleted = img.delete();
                        if (!deleted) {
                            Toast.makeText(c, c.getResources().getString(R.string.deleteThumbnail), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
            notifyDataSetChanged();
            db.close();
            wpdb.close();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(c, "Problem removing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void removeItem(int id) {
        // remove item i from the list and the database
        try {
            DBHandler db = new DBHandler(c);
            DBWayPtHandler dbwaypt = new DBWayPtHandler(c);
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

    static class ViewHolder {
        CardView row_item;
        EditText renameTxt;
        TextView nameTxt;
        TextView fileSizeTxt;
        TextView distToMapTxt;
        ImageView locIcon;
        ImageView img;
        ImageView selectedIcon;
        ImageView editBtn;
        ImageView saveBtn;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View rowView = view;
        if (rowView == null) {
            ViewHolder viewHolder = new ViewHolder();
            // INFLATE CUSTOM LAYOUT
            rowView = LayoutInflater.from(c).inflate(R.layout.model, viewGroup, false);
            //holder.deleteBtn = view.findViewById(R.id.ic_delete); // hidden at start
            viewHolder.row_item = rowView.findViewById(R.id.row_item);
            viewHolder.nameTxt = rowView.findViewById(R.id.nameTxt);
            viewHolder.renameTxt = rowView.findViewById(R.id.renameTxt);
            viewHolder.editBtn = rowView.findViewById(R.id.rename_edit);
            viewHolder.saveBtn = rowView.findViewById(R.id.rename_save);
            viewHolder.fileSizeTxt = rowView.findViewById(R.id.fileSizeTxt);
            viewHolder.distToMapTxt = rowView.findViewById(R.id.distToMapTxt);
            viewHolder.locIcon = rowView.findViewById(R.id.locationIcon);
            viewHolder.locIcon.setVisibility(View.GONE);
            viewHolder.img = rowView.findViewById(R.id.pdfImage);
            viewHolder.selectedIcon = rowView.findViewById((R.id.ic_selected_icon));
            rowView.setClickable(true);
            rowView.setLongClickable(true);

            // VIEW ITEM CLICK
            rowView.setOnClickListener(view1 -> {
                int j;
                TextView nameTxt = view1.findViewById(R.id.nameTxt);
                EditText renameTxt = view1.findViewById(R.id.renameTxt);
                ImageView saveBtn = view1.findViewById(R.id.rename_save);
                ImageView img = view1.findViewById(R.id.pdfImage);
                ImageView selectedIcon = view1.findViewById((R.id.ic_selected_icon));
                CardView row_item = view1.findViewById(R.id.row_item);

                // find the correct record
                for (j=0; j<pdfMaps.size();j++){
                    if (pdfMaps.get(j).getName().equals((nameTxt.getText().toString()))){
                        break;
                    }
                }
                if (j >= pdfMaps.size())return; // clicked save button
                PDFMap map = pdfMaps.get(j);
                // if editing do not open the map
                if (editing) {
                    if (map.getSelected()) {
                        // unselect it
                        map.setSelected(false);
                        row_item.setCardBackgroundColor(c.getResources().getColor(android.R.color.transparent));
                        selectedIcon.setVisibility(View.GONE); // check mark
                        img.setVisibility(View.VISIBLE);       // pdf thumbnail
                        renameTxt.setVisibility(View.GONE);
                        saveBtn.setVisibility(View.GONE);
                        nameTxt.setVisibility(View.VISIBLE);
                        map.setRename(map.getName());// didn't save it reset it
                    } else {
                        // select it
                        map.setSelected(true);
                        row_item.setCardBackgroundColor(0xffefefef);
                        selectedIcon.setVisibility(View.VISIBLE);
                        img.setVisibility(View.GONE);
                        renameTxt.setVisibility(View.VISIBLE);
                        saveBtn.setVisibility(View.GONE);
                        nameTxt.setVisibility(View.GONE);
                        map.setRename(map.getName());
                        renameTxt.setText(map.getName());
                    }

                    /*if (deleteBtn.getVisibility() == View.VISIBLE) {
                        deleteBtn.setVisibility(View.GONE);
                        editing = false;
                        return;
                    };*/
                }
                // open map
                else {
                    // Display the map
                    String bounds = map.getBounds();
                    if (bounds == null) {
                        Toast.makeText(c, "This file is not a Geo PDF. Missing GPTS Bounds.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String mediaBox = map.getMediabox();
                    if (mediaBox == null) {
                        Toast.makeText(c, "This file is not a Geo PDF. Missing MediaBox.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String viewPort = map.getViewport();
                    if (viewPort == null) {
                        Toast.makeText(c, "This file is not a Geo PDF. Missing Viewport.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    openPDFView(map.getPath(), map.getName(), bounds, mediaBox, viewPort);
                }
            });

            // ITEM LONG CLICK - show menu delete item, rename item
            rowView.setOnLongClickListener(view12 -> {
                TextView nameTxt = view12.findViewById(R.id.nameTxt);
                EditText renameTxt = view12.findViewById(R.id.renameTxt);
                ImageView saveBtn = view12.findViewById(R.id.rename_save);
                ImageView img = view12.findViewById(R.id.pdfImage);
                ImageView selectedIcon = view12.findViewById((R.id.ic_selected_icon));
                CardView row_item = view12.findViewById(R.id.row_item);
                // find the correct record
                int j;
                for (j=0; j<pdfMaps.size();j++){
                    if (pdfMaps.get(j).getName().equals((nameTxt.getText().toString()))){
                        break;
                    }
                }
                if (j >= pdfMaps.size())return false; // clicked save button
                PDFMap map = pdfMaps.get(j);

                editing = true;
                // unselect it
                if (map.getSelected()) {
                    map.setSelected(false);
                    row_item.setCardBackgroundColor(c.getResources().getColor(android.R.color.transparent));
                    img.setVisibility(View.GONE);
                    selectedIcon.setVisibility(View.VISIBLE);
                    nameTxt.setVisibility(View.VISIBLE);
                    renameTxt.setVisibility(View.GONE);
                    saveBtn.setVisibility(View.GONE);
                    map.setRename(map.getName());
                }
                // select it
                else {
                    map.setSelected(true);
                    row_item.setCardBackgroundColor(0xffefefef);
                    img.setVisibility(View.GONE);
                    selectedIcon.setVisibility(View.VISIBLE);
                    nameTxt.setVisibility(View.GONE);
                    renameTxt.setVisibility(View.VISIBLE);
                    saveBtn.setVisibility(View.GONE);
                    map.setRename(map.getName());
                    renameTxt.setText(map.getName());

                    //deleteBtn = view.findViewById(R.id.ic_delete); // hidden at start
                    //deleteBtn.setVisibility(View.VISIBLE);
                }

                // Open edit activity
            /*Intent i1 = new Intent(c, EditMapNameActivity.class);
            i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i1.putExtra("PATH", pdfMap.getPath());
            i1.putExtra("NAME", pdfMap.getName());
            i1.putExtra("BOUNDS", pdfMap.getBounds());
            i1.putExtra("ID", pdfMap.getId());
            i1.putExtra("IMG", pdfMap.getThumbnail());
            c.startActivity(i1);*/
                return false;
            });

            // ITEM LOST FOCUS
            /*rowView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    TextView nameTxt = v.findViewById(R.id.nameTxt);
                    EditText renameTxt = v.findViewById(R.id.renameTxt);
                    ImageView editBtn = v.findViewById(R.id.rename_edit);
                    ImageView saveBtn = v.findViewById(R.id.rename_save);
                    ImageView selectedBtn = v.findViewById((R.id.ic_selected_icon));
                    if (!hasFocus) {
                        renameTxt.setVisibility(View.GONE);
                        nameTxt.setVisibility(View.VISIBLE);
                        saveBtn.setVisibility(View.GONE);
                        if (selectedBtn.getVisibility() == View.VISIBLE){
                            editBtn.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (selectedBtn.getVisibility() == View.VISIBLE){
                            editBtn.setVisibility(View.GONE);
                            renameTxt.setVisibility(View.VISIBLE);
                            nameTxt.setVisibility(View.GONE);
                            saveBtn.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });*/

            /*viewHolder.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View parent = (View) v.getParent();
                    EditText renameTxt = parent.findViewById(R.id.renameTxt);
                    TextView nameTxt = parent.findViewById(R.id.nameTxt);
                    renameTxt.setVisibility(View.VISIBLE);
                    nameTxt.setVisibility(View.GONE);
                }
            });*/
            viewHolder.renameTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    /*
                     * When focus is lost save the entered value for
                     * later use. Works if hit DONE or NEXT on keyboard or click on another row.
                     * Does not work if change edittext and then hit back or save from menu. TODO!!!!!!
                     *  it gets called after it has saved the old name!!!
                     */
                    LinearLayout layout = (LinearLayout) v.getParent();
                    ImageView saveBtn = (ImageView) layout.getChildAt(1);
                    if (!hasFocus) {
                        saveBtn.setVisibility(View.GONE);
                        /*EditText renameTxt = v.findViewById(R.id.renameTxt);
                        //renameTxt.setVisibility(View.GONE);
                        LinearLayout layout = (LinearLayout)(((LinearLayout) v.getParent().getParent()).getChildAt(0));
                        TextView nameTxt = (TextView) layout.getChildAt(0);
                        String name = nameTxt.getText().toString();

                        // find the correct record
                        int j;
                        for (j = 0; j < pdfMaps.size(); j++) {
                            if (pdfMaps.get(j).getName().equals((name))) {
                                break;
                            }
                        }
                        // already been saved
                        if (j == pdfMaps.size()) return;
                        PDFMap map = pdfMaps.get(j);
                        if (map.getSelected()) {
                            String rename = renameTxt.getText().toString();
                            map.setRename(rename);
                        }*/
                    } else {
                        saveBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
            viewHolder.saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout editLayout = ((LinearLayout) v.getParent());
                    EditText renameTxt = (EditText) (editLayout.getChildAt(0));

                    LinearLayout layout = (LinearLayout)(((LinearLayout) v.getParent().getParent()).getChildAt(0));
                    TextView nameTxt = (TextView) layout.getChildAt(0);
                    String name = nameTxt.getText().toString();

                    // find the correct record
                    int j;
                    for (j = 0; j < pdfMaps.size(); j++) {
                        if (pdfMaps.get(j).getName().equals((name))) {
                            break;
                        }
                    }
                    // already been saved
                    if (j == pdfMaps.size()) return;
                    PDFMap map = pdfMaps.get(j);
                    String newName = renameTxt.getText().toString();
                    map.setRename(newName);
                    map.setName(newName);
                    pdfMaps.set(j,map);
                    nameTxt.setText(newName);
                    v.setVisibility(View.GONE); // hide save button
                    //c.myAdapter.rename(j,map);
                }
            });
            /* rowView.setOnDragListener((v, event) -> {
            v.setBackgroundColor(0xFFFF0000);
            return false;
        });*/

            rowView.setTag(viewHolder);
        }
        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();

        final PDFMap pdfMap = (PDFMap) this.getItem(i);

        // Selected row
       if (pdfMap.getSelected()) {
            holder.selectedIcon.setVisibility(View.VISIBLE); // checkmark
            holder.img.setVisibility(View.GONE); // pdf thumbnail
            holder.renameTxt.setVisibility(View.VISIBLE); // edit text box
            //holder.editBtn.setVisibility(View.VISIBLE); // edit text box
            holder.nameTxt.setVisibility(View.GONE);
            //if (pdfMap.getRename() == null || pdfMap.getRename().equals(""))
            //    holder.renameTxt.setText(pdfMap.getName());
            //else holder.renameTxt.setText(pdfMap.getRename());
            // first byte is alpha FF for opaque
            holder.row_item.setCardBackgroundColor(0xffefefef);
            //rowView.setBackgroundColor(0xFFCCCCCC);
            rowView.setActivated(true);

        }
        // Unselected row
        else if (!pdfMap.getSelected()) {
            holder.selectedIcon.setVisibility(View.GONE); // check mark
            holder.img.setVisibility(View.VISIBLE);       // pdf thumbnail
            holder.renameTxt.setVisibility(View.GONE);
            holder.nameTxt.setVisibility(View.VISIBLE);
            //holder.renameTxt.setText(pdfMap.getName());
            //rowView.setBackgroundColor(0xFFFFFFFF);
            rowView.setActivated(false);
            holder.row_item.setCardBackgroundColor(c.getResources().getColor(android.R.color.transparent));
           // rowView.setBackgroundResource(android.R.color.transparent);
        }
        // Update the map name
        /*holder.renameTxt.addTextChangedListener(new TextWatcher(){// add text watcher to update your data after editing
            public void afterTextChanged(Editable s) {
                // find the correct record
                int j;
                for (j=0; j<pdfMaps.size();j++){
                    if (pdfMaps.get(j).getName().equals((holder.nameTxt.getText().toString()))){
                        break;
                    }
                }
                PDFMap map = pdfMaps.get(j);
                if (map.getSelected()) {
                    map.setRename(holder.renameTxt.getText().toString());
                    //pdfMaps.set(i, pdfMaps.get(i)); // update your pdfMaps ArrayList
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });*/

        //  load thumbnail from file
        String path = pdfMap.getThumbnail();
        if (path == null) holder.img.setImageResource(R.drawable.pdf_icon);
        else {
            try {
                File imgFile = new File(pdfMap.getThumbnail());
                Bitmap myBitmap;
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (myBitmap != null)
                    holder.img.setImageBitmap(myBitmap);
                else
                    holder.img.setImageResource(R.drawable.pdf_icon);
            } catch (Exception ex) {
                Toast.makeText(c, "Problem reading thumbnail.", Toast.LENGTH_LONG).show();
                holder.img.setImageResource(R.drawable.pdf_icon);
            }
        }

        // Failed to Load - remove it. GetMoreActivity calls PDFMap.importMap
        if (pdfMap.getName().equals(c.getResources().getString(R.string.loading))) {
            removeItem(pdfMap.getId());
        } else {
            holder.nameTxt.setText(pdfMap.getName());
            if (pdfMap.getRename() == null) {
                pdfMap.setRename(pdfMap.getName());
                holder.renameTxt.setText(pdfMap.getName());
            }
            else
                holder.renameTxt.setText(pdfMap.getRename());
            holder.fileSizeTxt.setText(pdfMap.getFileSize());
            // getDistToMap sets miles also
            String dist = pdfMap.getDistToMap();
            if (latNow != null) {
                if (!dist.equals("onmap")) {
                    holder.locIcon.setVisibility(View.GONE);
                    holder.distToMapTxt.setText(dist);
                } else {
                    holder.locIcon.setVisibility(View.VISIBLE);
                    holder.distToMapTxt.setText("");
                }
            } else
                holder.locIcon.setVisibility(View.GONE);
        }

        // DELETE MAP
       /* deleteBtn.setOnClickListener(view2 -> {
           // removeItem(pdfMap.getId());
            Log.d("CustomAdapter",pdfMap.getName());
            Toast.makeText(c, "Map removed", Toast.LENGTH_LONG).show();
        });*/

        return rowView;
    }

    // EDIT MENU
    /*private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
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
                    nameTxt.setVisibility(View.GONE);
                    renameTxt.setVisibility(View.VISIBLE);
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
    };*/






    /*private void openMainView() {
        Intent i = new Intent(c, MainActivity.class);
        c.startActivity(i);
    }*/
    // OPEN PDF VIEW - load the map
    public void openPDFView(String path, String name, String bounds, String mediaBox, String viewPort) {
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