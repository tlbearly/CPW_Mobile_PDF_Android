package com.example.tammy.mapviewer;

/**
 * Created by tammy on 12/11/2017.
 */

public class PDFMap {
    private String path, bounds, mediabox, viewport,name;
    private int id;
    private byte[] thumbnail;

    public  PDFMap(){}

    public PDFMap(String path, String bounds, String mediabox, String viewport, byte[] thumbnail, String name){
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;

    }
    public PDFMap(int id, String path,String bounds,String mediabox,String viewport, byte[] thumbnail, String name){
        this.id = id;
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
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
}
