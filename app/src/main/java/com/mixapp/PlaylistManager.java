package com.mixapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages playlists - creation, storage, and retrieval
 */
public class PlaylistManager {
    private static final String TAG = "PlaylistManager";
    private static final String PREFS_NAME = "PlaylistManager";
    private static final String KEY_PLAYLISTS = "playlists";
    
    private Context context;
    private List<Playlist> playlists;
    private SharedPreferences prefs;
    
    public PlaylistManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.playlists = new ArrayList<>();
        loadPlaylists();
    }
    
    /**
     * Create a new playlist
     */
    public Playlist createPlaylist(String name) {
        String id = UUID.randomUUID().toString();
        Playlist playlist = new Playlist(name, id);
        playlists.add(playlist);
        savePlaylist(playlist);
        Log.d(TAG, "Created playlist: " + name);
        return playlist;
    }
    
    /**
     * Get all playlists
     */
    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }
    
    /**
     * Get playlist by ID
     */
    public Playlist getPlaylistById(String id) {
        for (Playlist playlist : playlists) {
            if (playlist.getId().equals(id)) {
                return playlist;
            }
        }
        return null;
    }
    
    /**
     * Delete a playlist
     */
    public boolean deletePlaylist(String id) {
        Playlist playlist = getPlaylistById(id);
        if (playlist != null) {
            playlists.remove(playlist);
            deletePlaylistFiles(id);
            savePlaylistList();
            Log.d(TAG, "Deleted playlist: " + playlist.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Update playlist name
     */
    public boolean updatePlaylistName(String id, String newName) {
        Playlist playlist = getPlaylistById(id);
        if (playlist != null) {
            playlist.setName(newName);
            savePlaylist(playlist);
            return true;
        }
        return false;
    }
    
    /**
     * Save a playlist with all its data
     */
    public void savePlaylist(Playlist playlist) {
        try {
            File playlistDir = getPlaylistDirectory();
            if (!playlistDir.exists()) {
                playlistDir.mkdirs();
            }
            
            File playlistFile = new File(playlistDir, playlist.getId() + ".json");
            JSONObject json = new JSONObject();
            json.put("id", playlist.getId());
            json.put("name", playlist.getName());
            
            // Save tracks (just save file references - PCM files already exist on disk)
            JSONArray tracksArray = new JSONArray();
            for (int i = 0; i < playlist.getTracks().size(); i++) {
                AudioMixer.TrackData track = playlist.getTracks().get(i);
                JSONObject trackJson = new JSONObject();
                trackJson.put("name", track.name);
                trackJson.put("index", i);
                // Get filename from the PCM file path
                String trackFileName = track.pcmFile.getName();
                trackJson.put("dataFile", trackFileName);
                tracksArray.put(trackJson);
            }
            json.put("tracks", tracksArray);
            
            // Save announcements (just save file references - PCM files already exist on disk)
            JSONArray announcementsArray = new JSONArray();
            for (int i = 0; i < playlist.getAnnouncements().size(); i++) {
                AudioMixer.AnnouncementData ann = playlist.getAnnouncements().get(i);
                JSONObject annJson = new JSONObject();
                annJson.put("name", ann.name);
                annJson.put("index", i);
                // Get filename from the PCM file path
                String annFileName = ann.pcmFile.getName();
                annJson.put("dataFile", annFileName);
                announcementsArray.put(annJson);
            }
            json.put("announcements", announcementsArray);
            
            // Write JSON file
            FileOutputStream fos = new FileOutputStream(playlistFile);
            fos.write(json.toString().getBytes());
            fos.close();
            
            // Update playlist list
            savePlaylistList();
            
            Log.d(TAG, "Saved playlist: " + playlist.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlist", e);
        }
    }
    
    /**
     * Load a playlist with all its data
     */
    public Playlist loadPlaylist(String playlistId) {
        try {
            File playlistDir = getPlaylistDirectory();
            File playlistFile = new File(playlistDir, playlistId + ".json");
            
            if (!playlistFile.exists()) {
                return null;
            }
            
            // Read JSON file
            FileInputStream fis = new FileInputStream(playlistFile);
            byte[] buffer = new byte[(int) playlistFile.length()];
            fis.read(buffer);
            fis.close();
            
            JSONObject json = new JSONObject(new String(buffer));
            String id = json.getString("id");
            String name = json.getString("name");
            Playlist playlist = new Playlist(name, id);
            
            // Load tracks (file references only, not loading PCM data into memory)
            JSONArray tracksArray = json.getJSONArray("tracks");
            for (int i = 0; i < tracksArray.length(); i++) {
                JSONObject trackJson = tracksArray.getJSONObject(i);
                String trackName = trackJson.getString("name");
                String dataFile = trackJson.getString("dataFile");
                File pcmFile = new File(getPlaylistDirectory(), dataFile);
                
                // Calculate sample count from file size (16-bit stereo = 4 bytes per sample)
                long fileSizeBytes = pcmFile.exists() ? pcmFile.length() : 0;
                long sampleCount = fileSizeBytes / (2 * 2); // 2 bytes per sample * 2 channels
                
                AudioMixer.TrackData track = new AudioMixer.TrackData(trackName, pcmFile, sampleCount);
                playlist.addTrack(track);
            }
            
            // Load announcements (file references only, not loading PCM data into memory)
            JSONArray announcementsArray = json.getJSONArray("announcements");
            for (int i = 0; i < announcementsArray.length(); i++) {
                JSONObject annJson = announcementsArray.getJSONObject(i);
                String annName = annJson.getString("name");
                String dataFile = annJson.getString("dataFile");
                File pcmFile = new File(getPlaylistDirectory(), dataFile);
                
                // Calculate sample count from file size (16-bit stereo = 4 bytes per sample)
                long fileSizeBytes = pcmFile.exists() ? pcmFile.length() : 0;
                long sampleCount = fileSizeBytes / (2 * 2); // 2 bytes per sample * 2 channels
                
                AudioMixer.AnnouncementData ann = new AudioMixer.AnnouncementData(annName, pcmFile, sampleCount);
                playlist.addAnnouncement(ann);
            }
            
            Log.d(TAG, "Loaded playlist: " + name + " (" + playlist.getTracks().size() + " tracks, " + 
                  playlist.getAnnouncements().size() + " announcements)");
            return playlist;
        } catch (Exception e) {
            Log.e(TAG, "Error loading playlist", e);
            return null;
        }
    }
    
    /**
     * Save PCM data to file
     */
    private void savePCMData(short[] pcmData, String fileName) throws IOException {
        File playlistDir = getPlaylistDirectory();
        File dataFile = new File(playlistDir, fileName);
        FileOutputStream fos = new FileOutputStream(dataFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(pcmData);
        oos.close();
        fos.close();
    }
    
    /**
     * Load PCM data from file
     */
    private short[] loadPCMData(String fileName) throws IOException, ClassNotFoundException {
        File playlistDir = getPlaylistDirectory();
        File dataFile = new File(playlistDir, fileName);
        FileInputStream fis = new FileInputStream(dataFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        short[] pcmData = (short[]) ois.readObject();
        ois.close();
        fis.close();
        return pcmData;
    }
    
    /**
     * Get playlist storage directory
     */
    private File getPlaylistDirectory() {
        File appDir = context.getFilesDir();
        return new File(appDir, "playlists");
    }
    
    /**
     * Save playlist list (metadata only)
     */
    private void savePlaylistList() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Playlist playlist : playlists) {
                JSONObject json = new JSONObject();
                json.put("id", playlist.getId());
                json.put("name", playlist.getName());
                jsonArray.put(json);
            }
            prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving playlist list", e);
        }
    }
    
    /**
     * Load playlists from SharedPreferences (metadata only, actual data loaded on demand)
     */
    private void loadPlaylists() {
        try {
            String jsonString = prefs.getString(KEY_PLAYLISTS, "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            playlists.clear();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String id = json.getString("id");
                String name = json.getString("name");
                // Load full playlist data
                Playlist playlist = loadPlaylist(id);
                if (playlist == null) {
                    // If loading fails, create empty playlist
                    playlist = new Playlist(name, id);
                }
                playlists.add(playlist);
            }
            
            Log.d(TAG, "Loaded " + playlists.size() + " playlists");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading playlists", e);
            playlists.clear();
        }
    }
    
    /**
     * Delete playlist files
     */
    public void deletePlaylistFiles(String playlistId) {
        try {
            File playlistDir = getPlaylistDirectory();
            File[] files = playlistDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith(playlistId + "_")) {
                        file.delete();
                    }
                }
            }
            File playlistFile = new File(playlistDir, playlistId + ".json");
            if (playlistFile.exists()) {
                playlistFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting playlist files", e);
        }
    }
}

