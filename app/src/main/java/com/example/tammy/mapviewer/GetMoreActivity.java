package com.example.tammy.mapviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import static android.content.Intent.ACTION_VIEW;

public class GetMoreActivity extends AppCompatActivity {
    public static FileObserver observer;
    public final String TAG = "DEBUG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_more);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);



        // Call a website with .html to load the aspx website using JavaScript

        Button cpwBtn = (Button) findViewById(R.id.CPWBtn);
        cpwBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent browser1 = new Intent(GetMoreActivity.this,WebActivity.class);
                browser1.putExtra("URL","http://cpw.state.co.us/learn/Pages/Maps-Library.aspx");
                startActivity(browser1);
            }
        });

        Button huntBtn = (Button) findViewById(R.id.huntBtn);
        huntBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Intent browser2 = new Intent(ACTION_VIEW, Uri.parse("https://ndismaps.nrel.colostate.edu/indexM.html?app=HuntingAtlas"));
                Intent browser2 = new Intent(GetMoreActivity.this,WebActivity.class);
                browser2.putExtra("URL","https://ndismaps.nrel.colostate.edu/indexM.html?app=HuntingAtlas");
                startActivity(browser2);
            }
        });

        Button fishBtn = (Button) findViewById(R.id.fishBtn);
        fishBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent browser1 = new Intent(ACTION_VIEW, Uri.parse("https://ndismaps.nrel.colostate.edu/indexM.html?app=FishingAtlas"));
                startActivity(browser1);
            }
        });

        Button downloadsBtn = (Button) findViewById(R.id.downloadsBtn);
        downloadsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i=new Intent(GetMoreActivity.this,LoadFileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                GetMoreActivity.this.startActivity(i);
                //finish();
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

}
