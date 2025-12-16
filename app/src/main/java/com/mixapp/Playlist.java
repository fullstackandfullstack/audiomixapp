package com.mixapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a playlist containing tracks and announcements
 */
public class Playlist {
    private String name;
    private String id;
    private List<AudioMixer.TrackData> tracks;
    private List<AudioMixer.AnnouncementData> announcements;
    
    public Playlist(String name, String id) {
        this.name = name;
        this.id = id;
        this.tracks = new ArrayList<>();
        this.announcements = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getId() {
        return id;
    }
    
    public List<AudioMixer.TrackData> getTracks() {
        return tracks;
    }
    
    public List<AudioMixer.AnnouncementData> getAnnouncements() {
        return announcements;
    }
    
    public void addTrack(AudioMixer.TrackData track) {
        tracks.add(track);
    }
    
    public void removeTrack(AudioMixer.TrackData track) {
        tracks.remove(track);
    }
    
    public void addAnnouncement(AudioMixer.AnnouncementData announcement) {
        announcements.add(announcement);
    }
    
    public void removeAnnouncement(AudioMixer.AnnouncementData announcement) {
        announcements.remove(announcement);
    }
    
    /**
     * Move announcement from one position to another
     */
    public void moveAnnouncement(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && fromPosition < announcements.size() &&
            toPosition >= 0 && toPosition < announcements.size()) {
            AudioMixer.AnnouncementData item = announcements.remove(fromPosition);
            announcements.add(toPosition, item);
        }
    }
    
    public boolean isEmpty() {
        return tracks.isEmpty() && announcements.isEmpty();
    }
}

