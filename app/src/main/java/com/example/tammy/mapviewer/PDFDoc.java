package com.example.tammy.mapviewer;

/**
 * Created by tammy on 9/7/2017.
 */

public class PDFDoc {
    private String name,path,bounds=null,mediaBox=null,viewPort=null;
    private byte[] thumbnail = null;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getPath(){
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getThumbnail(){
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail){
        this.thumbnail = thumbnail;
    }

    public void setBounds(String bounds){
        this.bounds = bounds;
    }

    public String getBounds() {return bounds;}

    public void setMediaBox(String mediaBox){
        this.mediaBox = mediaBox;
    }

    public String getMediaBox() {return mediaBox;}

    public void setViewPort(String viewPort){
        this.viewPort = viewPort;
    }

    public String getViewPort() {return viewPort;}
}