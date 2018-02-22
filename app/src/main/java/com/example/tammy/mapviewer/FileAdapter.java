package com.example.tammy.mapviewer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by tammy on 10/23/2017.
 * List of pdf files from Downloads directory
 */

public class FileAdapter extends BaseAdapter {
    Context c;
    ArrayList<PDFDoc> pdfDocs;
    //static SQLiteDatabase db;

    public FileAdapter(Context c, ArrayList<PDFDoc> pdfDocs){
        this.c = c;
        this.pdfDocs = pdfDocs;
    }
    @Override
    public int getCount() {
        return pdfDocs.size();
    }

    @Override
    public Object getItem(int i){
        return pdfDocs.get(i);
    }

    @Override
    public long getItemId(int i){
        return i;
    }

    @Override
    public View getView(int i, View view, final ViewGroup viewGroup){
        if(view==null) {
            // INFLATE CUSTOM LAYOUT
            view = LayoutInflater.from(c).inflate(R.layout.model,viewGroup,false);
        }
        final PDFDoc pdfDoc = (PDFDoc)this.getItem(i);

        TextView nameTxt = (TextView) view.findViewById(R.id.nameTxt);
        ImageView img = (ImageView) view.findViewById(R.id.pdfImage);

        // BIND DATA fill each item in the downloads directory list
        nameTxt.setText(pdfDoc.getName());
        img.setImageResource(R.drawable.pdf_icon);

        // VIEW ITEM CLICK
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importMap(pdfDoc.getPath());
            }
        });

        return view;
    }

    // OPEN Main Activity VIEW, IMPORT INTO DATABASE, show loading progress bar
    private void importMap(String path){
        // IMPORT a BLANK MAP INTO DATABASE
        PDFMap map = new PDFMap(path, "", "", "", null, "Loading...");
        DBHandler db = new DBHandler(c);
        db.addMap(map);

        // CALL MAIN ACTIVITY TO DISPLAY LIST OF IMPORTED MAPS WITH FLAG TO IMPORT THE NEW MAP
        Intent i=new Intent(c,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("IMPORT_MAP",true);
        i.putExtra("PATH",path);
        c.startActivity(i);
        //((Activity)c).finish();
    }
}