package com.dnrcpw.cpwmobilepdf.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.BuildConfig;
import com.dnrcpw.cpwmobilepdf.R;

/**
 * Display help
 */
public class HelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        String version = versionName + " ("+ versionCode +")";
        TextView versionTV = findViewById(R.id.version);
        versionTV.setText(version);
    }
}
