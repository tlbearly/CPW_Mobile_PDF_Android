package com.dnrcpw.cpwmobilepdf.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // return flag UPDATES, to tell MainActivity to not show
        // "you need to update" dialog again!
        if (item.getItemId() == android.R.id.home){
            // back button pressed, return
            Intent mainIntent = new Intent(HelpActivity.this, MainActivity.class);
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        } else {
            //default:
            return super.onOptionsItemSelected(item);
        }
    }
}
