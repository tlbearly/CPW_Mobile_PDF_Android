package com.example.tammy.mapviewer;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Created by tammy on 12/23/2017.
 * Display progress bar on import map
 */

public class ImportMapTask extends AsyncTask<Integer,Integer,String> {
    ProgressBar progressBar;
    String path;

    public void setPath(String path){
        this.path = path;
    }

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
        // show progress bar
     }

     @Override
     protected String doInBackground(Integer... params){
        // preform background computation
         int progress=0;
        publishProgress(progress); // calls onProgressUpdate
        return "Task Completed.";
    }

    @Override
    protected void onProgressUpdate(Integer... progress){
        super.onProgressUpdate(progress);
         // animate progress bar
        progressBar.setProgress(10);
    }

    @Override
    protected void onPostExecute(String result){
        // result of background computation is sent here
        progressBar.setVisibility(View.GONE);
    }
}
