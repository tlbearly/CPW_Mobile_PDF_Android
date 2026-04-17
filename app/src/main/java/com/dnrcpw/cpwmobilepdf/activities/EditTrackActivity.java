package com.dnrcpw.cpwmobilepdf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBWayPtHandler;
import com.dnrcpw.cpwmobilepdf.model.WayPt;
import com.dnrcpw.cpwmobilepdf.model.WayPts;

public class EditTrackActivity extends AppCompatActivity{
    EditText editTxt;
    TextView timeStamp;
    TextView location;
    ImageView trackImg;
    RadioGroup trackColorGrp;
    RadioButton cyanBtn, redBtn, blueBtn;
    private WayPts wayPts = null;
    String mapName;
    String path;
    String bounds;
    String viewPort;
    //private DBWayPtHandler db = DBWayPtHandler.getInstance(this);
    private DBWayPtHandler dbWayPtHandler;
    private int id;
    WayPt wayPt;
    //boolean landscape;
    boolean changed=false;
    Boolean showAllWayPts;
    String prevName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbWayPtHandler = new DBWayPtHandler(this);
        // Read the waypoint id that was clicked on and the map name
        Intent i = this.getIntent();
        if (i.getExtras() == null){
            Toast.makeText(EditTrackActivity.this, "Failed to get map values. Try again.",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        id = i.getExtras().getInt("CLICKED");
        mapName = i.getExtras().getString("NAME");
        path = i.getExtras().getString("PATH");
        bounds = i.getExtras().getString("BOUNDS");
        //String mediaBox = i.getExtras().getString("MEDIABOX");
        viewPort = i.getExtras().getString("VIEWPORT");
        //landscape = i.getExtras().getBoolean("LANDSCAPE");

        showAllWayPts = i.getExtras().getBoolean("SHOWALLWAYPTS");
        // if the map was locked in landscape, show this also in landscape
        /*if (landscape){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }*/
        wayPts = dbWayPtHandler.getWayPts(mapName);
        wayPts.SortPts();
        wayPt = wayPts.get(id);

        setContentView(R.layout.activity_track);
        prevName = wayPt.getDesc();
        editTxt = findViewById(R.id.trackName);
        editTxt.setText(wayPt.getDesc());
        trackImg = findViewById(R.id.track_img);
        trackColorGrp = findViewById(R.id.trackColor);
        String trackColor = wayPt.getColorName();
        cyanBtn = findViewById(R.id.cyanTrack);
        redBtn = findViewById(R.id.redTrack);
        blueBtn = findViewById(R.id.blueTrack);
        if (trackColor.equals("cyan")){
            cyanBtn.setChecked(true);
            trackImg.setImageResource(R.drawable.ic_cyan_track);
        }
        else if (trackColor.equals("red")){
            redBtn.setChecked(true);
            trackImg.setImageResource(R.drawable.ic_red_track);
        }
        else {
            blueBtn.setChecked(true);
            trackImg.setImageResource(R.drawable.ic_blue_track);
        }
        ImageButton clearBtn = findViewById(R.id.clear_track);

        // Clear track name
        clearBtn.setOnClickListener(view -> editTxt.setText(""));

        // Listeners for track color radio buttons
        trackColorGrp.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == R.id.cyanPin){
                wayPt.setColorName("cyan");
                trackImg.setImageResource(R.mipmap.ic_cyan_pin2);
                changed = true;
            }
            else if  (checkedId == R.id.redPin){
                wayPt.setColorName("red");
                trackImg.setImageResource(R.mipmap.ic_red_pin);
                changed = true;
            }
            else if  (checkedId == R.id.bluePin){
                wayPt.setColorName("blue");
                trackImg.setImageResource(R.mipmap.ic_blue_pin);
                changed = true;
            }
        });
    }

    // Remove Imported Map dialog
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove map from imported maps
                    dbWayPtHandler.deleteWayPt(wayPt);

                    // Return to PDFActivity
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    break;
            }
        }
    };

    // not saved
    DialogInterface.OnClickListener dialogClickListener2 = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'SAVE' button clicked, save then exit
                    String name = editTxt.getText().toString();
                    if (name.equals("")) {
                        Toast.makeText(EditTrackActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
                    } else {
                        wayPt.setDesc(name);
                        dbWayPtHandler.updateWayPt(wayPts.get(id));
                        // Return to PDFActivity
                        finish();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'BACK' button clicked, do nothing
                    finish();
                    break;
            }
        }
    };

    // EDIT MENU
    // -------------
    //   ... Menu
    // -------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        //menu.findItem(R.id.action_quality).setChecked(bestQuality);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.delete_map) {
            // display alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(EditTrackActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete this waypoint?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL", dialogClickListener).show();
            return true;
        } else if (item.getItemId() == R.id.save) {
            // rename map
            String name = editTxt.getText().toString();
            if (name.equals("")) {
                Toast.makeText(EditTrackActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
            } else {
                wayPt.setDesc(name);
                dbWayPtHandler.updateWayPt(wayPts.get(id));
                // Return to PDFActivity
                finish();
            }
            return true;
        } else if (item.getItemId() == android.R.id.home){
            // show dialog if not saved
            if (changed || !prevName.equals(editTxt.getText().toString())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditTrackActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Changes are not saved.").setPositiveButton("SAVE", dialogClickListener2)
                        .setNegativeButton("DON'T SAVE", dialogClickListener2).show();
            }
            else {
                finish();
            }
            return true;
        } else {
            //default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        dbWayPtHandler.close();
    }
}
