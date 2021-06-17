package com.example.tammy.pocketmaps.activities;

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

import com.example.tammy.pocketmaps.R;
import com.example.tammy.pocketmaps.data.DBWayPtHandler;
import com.example.tammy.pocketmaps.model.WayPt;
import com.example.tammy.pocketmaps.model.WayPts;

public class EditWayPointActivity extends AppCompatActivity {
    EditText editTxt;
    TextView timeStamp;
    TextView location;
    ImageView pin;
    RadioGroup pinColorGrp;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbWayPtHandler = new DBWayPtHandler(this);
        // Read the way point id that was clicked on and the map name
        Intent i = this.getIntent();
        if (i.getExtras() == null){
            Toast.makeText(EditWayPointActivity.this, "Failed to get map values. Try again.",Toast.LENGTH_LONG).show();
            return;
        }
        id = i.getExtras().getInt("CLICKED");
        mapName = i.getExtras().getString("NAME");
        path = i.getExtras().getString("PATH");
        bounds = i.getExtras().getString("BOUNDS");
        //String mediaBox = i.getExtras().getString("MEDIABOX");
        viewPort = i.getExtras().getString("VIEWPORT");
        wayPts = dbWayPtHandler.getWayPts(mapName);
        wayPts.SortPts();
        wayPt = wayPts.get(id);

        setContentView(R.layout.activity_way_pt);
        editTxt = findViewById(R.id.waypt);
        editTxt.setText(wayPt.getDesc());
        pin = findViewById(R.id.pushPin);
        timeStamp = findViewById(R.id.wayTime);
        timeStamp.setText(wayPt.getTime());
        location = findViewById(R.id.wayLocation);
        location.setText(wayPt.getLocation());
        pinColorGrp = findViewById(R.id.pinColor);
        String pinColor = wayPt.getColorName();
        cyanBtn = findViewById(R.id.cyanPin);
        redBtn = findViewById(R.id.redPin);
        blueBtn = findViewById(R.id.bluePin);
        if (pinColor.equals("cyan")){
            cyanBtn.setChecked(true);
            pin.setImageResource(R.mipmap.ic_cyan_pin2);
    }
        else if (pinColor.equals("red")){
            redBtn.setChecked(true);
            pin.setImageResource(R.mipmap.ic_red_pin);
        }
        else {
            blueBtn.setChecked(true);
            pin.setImageResource(R.mipmap.ic_blue_pin);
        }
        ImageButton clearBtn = findViewById(R.id.clear_waypt);

        // Clear way point name
        clearBtn.setOnClickListener(view -> editTxt.setText(""));

        // Listeners for Pin color radio buttons
        pinColorGrp.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == R.id.cyanPin){
                wayPt.setColorName("cyan");
                pin.setImageResource(R.mipmap.ic_cyan_pin2);
            }
            else if  (checkedId == R.id.redPin){
                wayPt.setColorName("red");
                pin.setImageResource(R.mipmap.ic_red_pin);
            }
            else if  (checkedId == R.id.bluePin){
                wayPt.setColorName("blue");
                pin.setImageResource(R.mipmap.ic_blue_pin);
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
            //case R.id.delete_map:
            // display alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(EditWayPointActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete this way point?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL", dialogClickListener).show();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            //case android.R.id.home:
                // rename map
                String name = editTxt.getText().toString();
                if (name.equals("")) {
                    Toast.makeText(EditWayPointActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
                } else {
                    wayPt.setDesc(name);
                    dbWayPtHandler.updateWayPt(wayPts.get(id));
                    // Return to PDFActivity
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