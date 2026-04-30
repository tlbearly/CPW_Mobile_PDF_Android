package com.dnrcpw.cpwmobilepdf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
public class Tracks {
    // Array of Track objects for a given map
    private final ArrayList<Track> tracks;

    public Tracks(String mapName){
        this.tracks = new ArrayList<>();
    }
    public Track add (String name, String desc, String colorName, List<TrackSegment> trackSegment){
        // Add a waypoint to the list.
        Track obj = new Track(name, desc, colorName, trackSegment);
        tracks.add(obj);
        return obj;
    }

    public void add (int id, String name, String desc, String colorName, String time, String trackSegment){
        // Add a track to the database
        Track obj = new Track(id, name, desc, colorName, time, trackSegment);
        tracks.add(obj);
    }

    public void removeAllFromMap(String mapName){
        // Remove all tracks in a map
        for (int i=0; i<tracks.size(); i++){
            // used to be == mapName))
            if ((tracks.get(i).getMapName().equals(mapName))) {
                tracks.remove(i);
            }
        }
    }
    public void remove(Track track){
        // Remove a track from tracks
        int id = track.getId();
        for (int i=0; i<tracks.size(); i++){
            // used to be == mapName))
            if ((tracks.get(i).getId() == id)) {
                tracks.remove(i);
                break;
            }
        }
    }
    public void removeAll(){
        tracks.clear();
    }

    public Track get(int i){
        return tracks.get(i);
    }

    public int size(){
        return tracks.size();
    }
}

