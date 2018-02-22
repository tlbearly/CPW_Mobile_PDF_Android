package com.example.tammy.mapviewer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class WebActivity extends AppCompatActivity {
// Load a CPW Website in an activity window
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        Intent i = this.getIntent();
        String url="";

        //  GET URL to Load
        try {
            url = i.getExtras().getString("URL");
        } catch (Exception e) {
            Toast.makeText(WebActivity.this, "Trouble reading the web address. Read: " + url, Toast.LENGTH_LONG).show();
        }
        WebView myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        // NOTE: Side note: you'll be warned for a security issue (because JavaScript is evil!). Simply add the SuppressWarning annotation to the onCreate() method
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportMultipleWindows();
        myWebView.loadUrl(url);
        Toast.makeText(WebActivity.this, "Loading website "+url, Toast.LENGTH_LONG).show();
    }
}
