package com.example.tammy.pocketmaps;

import java.io.File;
import java.util.Comparator;

/**
 * Created by tammy on 12/11/2017.
 *  Imported Maps
 */

public class PDFMap {
    private String path, bounds, mediabox, viewport, name, fileSize, distToMap;
    private int id;
    private Double miles;
    private String thumbnail; // image filename

    public PDFMap(){}

    public PDFMap(String path, String bounds, String mediabox, String viewport, String thumbnail, String name, String fileSize, String distToMap){
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
        this.fileSize = fileSize;
        this.distToMap = distToMap;
        if (distToMap.equals(""))
            this.miles = 0.0;
        else
            this.miles = Double.parseDouble(distToMap);
    }

    public PDFMap(int id, String path, String bounds, String mediabox, String viewport, String thumbnail, String name, String fileSize, String distToMap){
        this.id = id;
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
        this.fileSize = fileSize;
        this.distToMap = distToMap;
        if (distToMap.equals(""))
            this.miles = 0.0;
        else
            this.miles = Double.parseDouble(distToMap);
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

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileSize() { return fileSize; }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getDistToMap() { return distToMap; }

    public void setDistToMap(String distToMap) {
        this.distToMap = distToMap;
    }

    public Double getMiles() { return miles; }

    public void setMiles(Double miles) { this.miles = miles; }

    // Sort arraylist of PDFMaps by pdf file name a-z
    public static Comparator<PDFMap> NameComparator = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            String name1 = m1.getName().toLowerCase();
            String name2 = m2.getName().toLowerCase();
            // Return ascending order
            return name1.compareTo(name2);
        }
    };

    // Sort arraylist of PDFMaps by pdf file name reverse z-a
    public static Comparator<PDFMap> NameComparatorReverse = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            String name1 = m1.getName().toLowerCase();
            String name2 = m2.getName().toLowerCase();
            // Return ascending order
            return name2.compareTo(name1);
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
            else if (firstDate > secondDate) return -1;
            else return 0;
        }
    };

    // Sort arraylist of PDFMaps by pdf file modification date reversed
    public static Comparator<PDFMap> DateComparatorReverse = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            File file1 = new File(m1.getPath());
            File file2 = new File(m2.getPath());
            long firstDate = file1.lastModified();
            long secondDate = file2.lastModified();
            // Return ascending order
            if (firstDate > secondDate) return 1;
            else if (firstDate < secondDate) return -1;
            else return 0;
        }
    };

    // Sort arraylist of PDFMaps by proximity to the user's current location, closest first
    public static Comparator<PDFMap> ProximityComparator = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            // Sort closest map at top
            Double miles1 = m1.getMiles();
            Double miles2 = m2.getMiles();
            if (miles1 > miles2) return 1;
            else if (miles1 < miles2) return -1;
            else return 0;
        }
    };

    // Sort arraylist of PDFMaps by proximity to the user's current location, closest last
    public static Comparator<PDFMap> ProximityComparatorReverse = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            // Sort closest map at top
            Double miles1 = m1.getMiles();
            Double miles2 = m2.getMiles();
            if (miles1 < miles2) return 1;
            else if (miles1 > miles2) return -1;
            else return 0;
        }
    };

    // Sort arraylist of PDFMaps by pdf file size largest first
    public static Comparator<PDFMap> SizeComparator = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            String m1Num = m1.getFileSize();
            if (m1Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
            m1Num = m1Num.substring(0,m1Num.length()-3);
            Float size1 = Float.parseFloat(m1Num);

            if (m1.getFileSize().contains("Mb")){
                size1 = size1 * 1000;
            }
            else if (m1.getFileSize().contains("Gb")){
                size1 = size1 * 1000000;
            }
            String m2Num = m2.getFileSize();
            if (m2Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
            m2Num = m2Num.substring(0,m2Num.length()-3);
            Float size2 = Float.parseFloat(m2Num);
            if (m2.getFileSize().contains("Mb")){
                size2 = size2 * 1000;
            }
            else if (m2.getFileSize().contains("Gb")){
                size2 = size2 * 1000000;
            }
            // Return largest to smallest
            if (size1 < size2) return 1;
            else if (size1 > size2) return -1;
            else return 0;
        }
    };

    // Sort arraylist of PDFMaps by pdf file size, smallest first
    public static Comparator<PDFMap> SizeComparatorReverse = new Comparator<PDFMap>() {
        @Override
        public int compare(PDFMap m1, PDFMap m2) {
            String m1Num = m1.getFileSize();
            if (m1Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
            m1Num = m1Num.substring(0,m1Num.length()-3);
            Float size1 = Float.parseFloat(m1Num);

            if (m1.getFileSize().contains("Mb")){
                size1 = size1 * 1000;
            }
            else if (m1.getFileSize().contains("Gb")){
                size1 = size1 * 1000000;
            }
            String m2Num = m2.getFileSize();
            if (m2Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
            m2Num = m2Num.substring(0,m2Num.length()-3);
            Float size2 = Float.parseFloat(m2Num);
            if (m2.getFileSize().contains("Mb")){
                size2 = size2 * 1000;
            }
            else if (m2.getFileSize().contains("Gb")){
                size2 = size2 * 1000000;
            }
            // Return largest to smallest
            if (size1 > size2) return 1;
            else if (size1 < size2) return -1;
            else return 0;
        }
    };
}