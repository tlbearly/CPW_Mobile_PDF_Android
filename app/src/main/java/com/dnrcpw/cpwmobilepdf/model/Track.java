package com.dnrcpw.cpwmobilepdf.model;

import com.dnrcpw.cpwmobilepdf.activities.PDFActivity;
import com.dnrcpw.cpwmobilepdf.data.DBTrackHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Track {
    // Array of Track objects for a given map
    private long id;
    public String mapName; // filename minus the path
    public String desc; // label or description
    public String colorName; // cyan, blue, red
    // track is an array list of each line segment (x1,y1,x2,y2) for one path
    private List<TrackSegment> trackSegments = new ArrayList<>();
    private String time;
    public Track(){}

    public Track(String mapName, String desc, String colorName, List<TrackSegment> trackSegments){
        this.mapName = mapName;
        this.desc = desc;
        this.colorName = colorName;
        this.trackSegments = trackSegments;
        Calendar cal = Calendar.getInstance();
        java.util.Date date = cal.getTime();
        DateFormat formattedDate = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
        this.time = formattedDate.format(date);
    }

    public Track(int id, String mapName, String desc, String trackSegments, String colorName, String time){
        // Called by Tracks/add and DBTrackHandler/getTracks to read the tracks from the database and fill the tracks ArrayList
        if (trackSegments.equals("")){
            this.trackSegments = null;
        }else {
            List<String> segments = Arrays.asList(trackSegments.split(","));
            for (int i = 0; i < segments.size(); i += 4) {
                float x1 = Float.parseFloat(segments.get(i));
                float y1 = Float.parseFloat(segments.get(i + 1));
                float x2 = Float.parseFloat(segments.get(i + 2));
                float y2 = Float.parseFloat(segments.get(i + 3));
                TrackSegment thisSegment = new TrackSegment(x1, y1, x2, y2);
                this.trackSegments.add(thisSegment);
            }
        }
        this.id = id;
        this.mapName = mapName;
        this.desc = desc;
        this.colorName = colorName;
        this.time = time;
    }
    public String getLineSegments(){
        // DBTrackHandler calls this to store it in the database as a string of comma-delimited lat, long points
        if (trackSegments == null) return "";
        String lineSegments = "";
        for (int i=0; i< trackSegments.size(); i++){
            if (lineSegments != "") lineSegments += ",";
            lineSegments += trackSegments.get(i).x1 + "," + trackSegments.get(i).y1 + "," + trackSegments.get(i).x2 + "," + trackSegments.get(i).y2;
        }
        return lineSegments;
    }
    public void addTrackSegment(float x1, float y1, float x2, float y2){
        TrackSegment trackSegment = new TrackSegment(x1,y1, x2, y2);
        this.trackSegments.add(trackSegment);
    }
    public List<TrackSegment> getTrackSegments(){
        return this.trackSegments;
    }
    public void setTrackSegments(List<TrackSegment> trackSegments){
        this.trackSegments = trackSegments;
    }

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getMapName(){
        return this.mapName;
    }
    public void setMapName(String mapName){
        this.mapName = mapName;
    }
    public void setColorName(String colorName){
        this.colorName = colorName;
    }
    public String getColorName(){
        return this.colorName;
    }
    // Return description of the track
    public String getDesc(){
        return this.desc;
    }
    public void setDesc(String desc) {this.desc = desc;}
    public String getTime() {
        return time;
    }
}
