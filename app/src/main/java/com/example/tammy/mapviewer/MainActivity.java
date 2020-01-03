package com.example.tammy.mapviewer;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
// Displays list of imported pdf maps and an add more button. When an item is clicked, it loads the map.
    private ListView lv;
    private CustomAdapter myAdapter; // list of imported pdf maps
    private  DBHandler db;
    private String TAG = "MainActivity";
    boolean sortFlag=true;
    Toolbar toolbar;
    Integer selectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DBHandler.getInstance(MainActivity.this);
        // top menu with ... button
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // GET THE LIST FROM THE DATABASE
        myAdapter = new CustomAdapter(MainActivity.this, db.getAllMaps());
        lv = (ListView) findViewById(R.id.lv);
        lv.setAdapter(myAdapter);
        registerForContextMenu(lv); // set up edit/trash context menu

        // Make sure all the maps in the database still exist
        myAdapter.checkIfExists();

        // Display note if no records found
        if (myAdapter.pdfMaps.size() == 0){
            TextView msg = (TextView) findViewById(R.id.txtMessage);
            msg.setText("No maps have been imported.\nUse the + button to import a map.");
        }

        // IMPORT NEW MAP INTO LIST
        // CustomAdapter calls MapImportTask class in CustomAdapter.java
        final Intent i = this.getIntent();
        if (i.getExtras()!=null && !i.getExtras().isEmpty()) {
            if (i.getExtras().containsKey("IMPORT_MAP") && i.getExtras().containsKey("PATH")) {
                Boolean import_map = i.getExtras().getBoolean("IMPORT_MAP");
                // IMPORT MAP SELECTED
                if (import_map) {
                    sortFlag=false; // hold off on sorting.
                    // Scroll down to last item. The one just added.
                   // String name = new File(i.getExtras().getString("PATH")).getName();
                   // int pos = myAdapter.findName();
                    int pos = myAdapter.getCount()-1;
                    if (pos > -1) lv.setSelection(pos);
                }
                i.removeExtra("PATH");
                i.removeExtra("IMPORT_MAP");
            }
            // RENAME MAP
            else if (i.getExtras().containsKey("RENAME") && i.getExtras().containsKey("NAME") && i.getExtras().containsKey("ID")) {
                // Renamed map, update with new name
                String name = i.getExtras().getString("NAME");
                Integer id = i.getExtras().getInt("ID");
                myAdapter.rename(id, name);
                //Toast.makeText(MainActivity.this, "Map renamed to: " + name, Toast.LENGTH_LONG).show();
                i.removeExtra("NAME");
                i.removeExtra("ID");
                i.removeExtra("RENAME");
            }
            // DELETE MAP
            else if (i.getExtras().containsKey("DELETE") && i.getExtras().containsKey("ID")) {
                // Delete map, remove from Imported Maps list, delete from database
                selectedId = i.getExtras().getInt("ID");
                myAdapter.removeItem(selectedId);
                Toast.makeText(MainActivity.this, "Map removed", Toast.LENGTH_LONG).show();
                i.removeExtra("ID");
                i.removeExtra("DELETE");
                // Display note if no records found
                if (myAdapter.pdfMaps.size() == 0){
                    TextView msg = (TextView) findViewById(R.id.txtMessage);
                    msg.setText("No maps have been imported.\nUse the + button to import a map.");
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
                 startActivity(i);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


    // ...................
    //     ... MENU
    // ...................
    MenuItem nameItem;
    MenuItem dateItem;
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //DELETE all imported maps clicked
                    //db.deleteTable(MainActivity.this);
                    myAdapter.removeAll();
                    // Display note if no records found
                    if (myAdapter.pdfMaps.size() == 0){
                        TextView msg = (TextView) findViewById(R.id.txtMessage);
                        msg.setText("No maps have been imported.\nUse the + button to import a map.");
                    }
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
        nameItem = menu.findItem(R.id.action_sort_by_name);
        dateItem = menu.findItem(R.id.action_sort_by_date);
        // read Settings table in the database and get user preferences
        String sort = db.getMapSort();
        if (sortFlag && sort.equals("name")){
            myAdapter.SortByName();
            lv.setAdapter(myAdapter);
            nameItem.setChecked(true);
            dateItem.setChecked(false);
        }
        else if (sortFlag && sort.equals("date")){
            myAdapter.SortByDate();
            lv.setAdapter(myAdapter);
            dateItem.setChecked(true);
            nameItem.setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Found in res/menu/menu_main.xml
        int id = item.getItemId();

        // Delete all imported maps
        if (id == R.id.action_deleteAll){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete all imported maps?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL",dialogClickListener).show();
            return true;
        }
        // Sort map list by name
        else if (id == R.id.action_sort_by_name){
            myAdapter.SortByName();
            lv.setAdapter(myAdapter);
            item.setChecked(true);
            dateItem.setChecked(false);
            try {
                db.setMapSort("name");
            }
            catch (SQLException e){
                Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        // Sort map list by date
        else if (id == R.id.action_sort_by_date){
            myAdapter.SortByDate();
            lv.setAdapter(myAdapter);
            item.setChecked(true);
            nameItem.setChecked(false);
            try {
                db.setMapSort("date");
            }
            catch (SQLException e){
                Toast.makeText(getApplicationContext(), "Error writing to app database: "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        // This is not visible in main activity
        else if (id == R.id.action_open){
            lv.setAdapter(new CustomAdapter(MainActivity.this,db.getAllMaps()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}