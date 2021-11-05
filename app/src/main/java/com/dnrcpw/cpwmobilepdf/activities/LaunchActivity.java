package com.dnrcpw.cpwmobilepdf.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Send user to MainActivity as soon as this activity loads
        Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
        startActivity(intent);

        // remove this activity from the stack
        finish();
    }
}