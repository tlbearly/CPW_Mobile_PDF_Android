package com.example.tammy.mapviewer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
// Displays list of imported pdf maps and an add more button. When an item is clicked, it loads the map.
    private ListView lv;
    //private byte[] data;
    private boolean bestQuality=false;
    private CustomAdapter myAdapter;
    private  DBHandler db = DBHandler.getInstance(this);
    private String TAG = "MainActivity";
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // top menu with ... button
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // GET THE LIST FROM THE DATABASE
        myAdapter = (CustomAdapter) new CustomAdapter(MainActivity.this, db.getAllMaps());
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(myAdapter);
        //lv.setItemsCanFocus(true); //does not make selected item highlight
        registerForContextMenu(lv); // set up edit/trash context menu

        // Display note if no records found
        if (myAdapter.pdfMaps.size() == 0){
            TextView msg = (TextView) findViewById(R.id.txtMessage);
            msg.setText("No maps have been imported."); //\n\nUse the + button to import a map.");
        }

        // IMPORT NEW MAP INTO LIST
        // FileAdapter adds a database record. Sets name to "Loading..." CustomAdapter calls MapImportTask class in CustomAdapter.java
        final Intent i = this.getIntent();
        if (i.getExtras()!=null && !i.getExtras().isEmpty()) {
            if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                Boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED in FileAdapter.java Downloads directory
                if (import_map) {
                    // Scroll down to last item. The one just added.
                    int pos = lv.getCount() - 1;
                    if (pos > -1) lv.setSelection(pos);
                }
            }
        }

        // FLOATING ACTION BUTTON CLICK
         FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
         fab.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent i=new Intent(MainActivity.this,GetMoreActivity.class);
                 i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                 MainActivity.this.startActivity(i);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    // ...................
    // ...    MENU    ....
    // ...................
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //DELETE button clicked
                    db.deleteTable(MainActivity.this);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //CANCEL button clicked
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_quality) {
            if (item.isChecked()) {
                item.setChecked(false);
                bestQuality=false;
                myAdapter.setBestQuality(false);
                lv.setAdapter(myAdapter);
            }
            else {
                item.setChecked(true);
                bestQuality=true;
                myAdapter.setBestQuality(true);
                lv.setAdapter(myAdapter);
            }
            return true;
        }
        // Delete all imported maps
        else if (id == R.id.action_deleteAll){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete all imported maps?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL",dialogClickListener).show();
            return true;
        }
        else if (id == R.id.action_open){
            lv.setAdapter(new CustomAdapter(MainActivity.this,db.getAllMaps()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}