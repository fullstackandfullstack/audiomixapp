package com.mixapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
        savePlaylists();
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
            savePlaylists();
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
            savePlaylists();
            return true;
        }
        return false;
    }
    
    /**
     * Save playlists to SharedPreferences
     * Note: Only saves playlist metadata, not audio data
     */
    private void savePlaylists() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Playlist playlist : playlists) {
                JSONObject json = new JSONObject();
                json.put("id", playlist.getId());
                json.put("name", playlist.getName());
                json.put("trackCount", playlist.getTracks().size());
                json.put("announcementCount", playlist.getAnnouncements().size());
                jsonArray.put(json);
            }
            prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving playlists", e);
        }
    }
    
    /**
     * Load playlists from SharedPreferences
     * Note: Only loads metadata, tracks/announcements need to be loaded separately
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
                Playlist playlist = new Playlist(name, id);
                playlists.add(playlist);
            }
            
            Log.d(TAG, "Loaded " + playlists.size() + " playlists");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading playlists", e);
            playlists.clear();
        }
    }
}

