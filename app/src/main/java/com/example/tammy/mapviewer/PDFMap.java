package com.example.tammy.mapviewer;

import java.io.File;
import java.util.Comparator;

/**
 * Created by tammy on 12/11/2017.
 *  Imported Maps
 */

public class PDFMap {
    private String path, bounds, mediabox, viewport, name;
    private int id;
    private String thumbnail; // image filename

    public  PDFMap(){}

    public PDFMap(String path, String bounds, String mediabox, String viewport, String thumbnail, String name){
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
    }

    public PDFMap(int id, String path,String bounds,String mediabox,String viewport, String thumbnail, String name){
        this.id = id;
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBounds() {
        return bounds;
    }

    public void setBounds(String bounds) {
        this.bounds = bounds;
    }

    public String getMediabox() {
        return mediabox;
    }

    public void setMediabox(String mediabox) {
        this.mediabox = mediabox;
    }

    public String getViewport() {
        return viewport;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Sort arraylist of PDFMaps by pdf file name
    public static Comparator<PDFMap> NameComparator = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            String name1 = m1.getName().toLowerCase();
            String name2 = m2.getName().toLowerCase();
            // Return ascending order
            return name1.compareTo(name2);
        }
    };

    // Sort arraylist of PDFMaps by pdf file modification date
    public static Comparator<PDFMap> DateComparator = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            File file1 = new File(m1.getPath());
            File file2 = new File(m2.getPath());
            long firstDate = file1.lastModified();
            long secondDate = file2.lastModified();
            // Return ascending order
            if (firstDate < secondDate) return 1;
            else
                return -1;
        }
    };
}
