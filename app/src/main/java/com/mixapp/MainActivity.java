package com.mixapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity provides the UI and controls for the Audio Mixer app with playlist support.
 * Handles playlist management, file selection, playback controls, and volume adjustments.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PICK_AUDIO = 1001;
    private static final int REQUEST_CODE_PICK_ANNOUNCEMENT = 1002;
    private static final int PERMISSION_REQUEST_CODE = 2001;
    
    private AudioMixer audioMixer;
    private PlaylistManager playlistManager;
    private Playlist currentPlaylist;
    
    // UI Components
    private ImageButton btnMenu;
    private Button btnAddTrack;
    private Button btnAddAnnouncement;
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    private SeekBar seekMainVolume;
    private SeekBar seekAnnouncementVolume;
    private SeekBar seekAnnouncementInterval;
    private CheckBox checkPlayAtEnd;
    private TextView tvTracks;
    private TextView tvStatus;
    private TextView tvIntervalValue;
    private TextView tvSelectedPlaylist;
    private RecyclerView recyclerAnnouncements;
    
    private Handler mainHandler;
    private AnnouncementAdapter announcementAdapter;
    private boolean isAddingTrack = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize managers
        playlistManager = new PlaylistManager(this);
        audioMixer = new AudioMixer();
        audioMixer.initialize();
        
        // Initialize UI components
        initializeViews();
        setupListeners();
        setupRecyclerView();
        
        // Request permissions
        requestPermissions();
        
        updateUI();
    }
    
    /**
     * Initialize all view references
     */
    private void initializeViews() {
        btnMenu = findViewById(R.id.btnMenu);
        btnAddTrack = findViewById(R.id.btnAddTrack);
        btnAddAnnouncement = findViewById(R.id.btnAddAnnouncement);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        seekMainVolume = findViewById(R.id.seekMainVolume);
        seekAnnouncementVolume = findViewById(R.id.seekAnnouncementVolume);
        seekAnnouncementInterval = findViewById(R.id.seekAnnouncementInterval);
        checkPlayAtEnd = findViewById(R.id.checkPlayAtEnd);
        tvTracks = findViewById(R.id.tvTracks);
        tvStatus = findViewById(R.id.tvStatus);
        tvIntervalValue = findViewById(R.id.tvIntervalValue);
        tvSelectedPlaylist = findViewById(R.id.tvSelectedPlaylist);
        recyclerAnnouncements = findViewById(R.id.recyclerAnnouncements);
    }
    
    /**
     * Setup RecyclerView for announcements with drag-and-drop
     */
    private void setupRecyclerView() {
        announcementAdapter = new AnnouncementAdapter(
            currentPlaylist != null ? currentPlaylist.getAnnouncements() : null,
            (fromPosition, toPosition) -> {
                if (currentPlaylist != null && audioMixer != null) {
                    // The adapter has already moved the item in its internal list
                    // Get the current order from adapter
                    List<AudioMixer.AnnouncementData> adapterList = announcementAdapter.getAnnouncements();
                    if (adapterList != null && !adapterList.isEmpty()) {
                        // CRITICAL: Update playlist to match adapter order
                        // Create completely new list to break any references
                        List<AudioMixer.AnnouncementData> newList = new ArrayList<>();
                        for (AudioMixer.AnnouncementData ann : adapterList) {
                            newList.add(ann); // Add in the new order from adapter
                        }
                        
                        // Replace playlist announcements with new ordered list
                        currentPlaylist.getAnnouncements().clear();
                        currentPlaylist.getAnnouncements().addAll(newList);
                        
                        // Force reload playlist to mixer - this will completely replace the announcements
                        // Stop any current playback first
                        boolean wasPlaying = audioMixer.isPlaying();
                        if (wasPlaying) {
                            audioMixer.stop();
                        }
                        
                        // Reload with new order
                        audioMixer.loadPlaylist(currentPlaylist);
                        
                        // If it was playing, restart it
                        if (wasPlaying) {
                            audioMixer.play();
                        }
                    }
                }
            }
        );
        
        recyclerAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        recyclerAnnouncements.setAdapter(announcementAdapter);
        
        // Enable drag-and-drop with long press
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(announcementAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerAnnouncements);
    }
    
    /**
     * Setup event listeners for all controls
     */
    private void setupListeners() {
        // Menu button
        btnMenu.setOnClickListener(v -> showPlaylistMenu());
        
        // Add Track button - show playlist selection
        btnAddTrack.setOnClickListener(v -> {
            isAddingTrack = true;
            if (currentPlaylist == null) {
                showPlaylistSelectionForAdd(true);
            } else {
                pickAudioFile(REQUEST_CODE_PICK_AUDIO);
            }
        });
        
        // Add Announcement button - show playlist selection
        btnAddAnnouncement.setOnClickListener(v -> {
            isAddingTrack = false;
            if (currentPlaylist == null) {
                showPlaylistSelectionForAdd(false);
            } else {
                pickAudioFile(REQUEST_CODE_PICK_ANNOUNCEMENT);
            }
        });
        
        // Playback controls
        btnPlay.setOnClickListener(v -> {
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                audioMixer.play();
                updateStatus("Playing");
            }
        });
        
        btnPause.setOnClickListener(v -> {
            audioMixer.pause();
            updateStatus("Paused");
        });
        
        btnStop.setOnClickListener(v -> {
            audioMixer.stop();
            updateStatus("Stopped");
        });
        
        // Volume sliders
        seekMainVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentPlaylist != null) {
                    float volume = progress / 100.0f;
                    audioMixer.setMainVolume(volume);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekAnnouncementVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentPlaylist != null) {
                    float volume = progress / 100.0f;
                    audioMixer.setAnnouncementVolume(volume);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Announcement interval slider
        seekAnnouncementInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentPlaylist != null) {
                    audioMixer.setAnnouncementInterval(progress);
                    tvIntervalValue.setText(progress + " seconds");
                    
                    // Auto-set announcement volume to 75% if interval > 0 and fade enabled
                    if (progress > 0) {
                        seekAnnouncementVolume.setProgress(75);
                        audioMixer.setAnnouncementVolume(0.75f);
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Play at end checkbox
        checkPlayAtEnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentPlaylist != null) {
                audioMixer.setPlayAtEndOnly(isChecked);
            }
        });
        
        // Click on selected playlist to change it
        tvSelectedPlaylist.setOnClickListener(v -> showPlaylistSelectionDialog(true));
    }
    
    /**
     * Show playlist menu (create, select, manage)
     */
    private void showPlaylistMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenu().add("Create New Playlist");
        popup.getMenu().add("Select Playlist");
        popup.getMenu().add("Manage Playlists");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Create New Playlist")) {
                showCreatePlaylistDialog();
            } else if (title.equals("Select Playlist")) {
                showPlaylistSelectionDialog(true);
            } else if (title.equals("Manage Playlists")) {
                showManagePlaylistsDialog();
            }
            return true;
        });
        popup.show();
    }
    
    /**
     * Show dialog to create a new playlist
     */
    private void showCreatePlaylistDialog() {
        showCreatePlaylistDialog(false);
    }
    
    /**
     * Show dialog to create a new playlist, optionally pick file after
     */
    private void showCreatePlaylistDialog(boolean pickFileAfter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Playlist");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter playlist name");
        builder.setView(input);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Playlist playlist = playlistManager.createPlaylist(name);
                selectPlaylist(playlist);
                Toast.makeText(this, "Playlist created: " + name, Toast.LENGTH_SHORT).show();
                if (pickFileAfter) {
                    // Pick file after creating playlist
                    pickAudioFile(isAddingTrack ? REQUEST_CODE_PICK_AUDIO : REQUEST_CODE_PICK_ANNOUNCEMENT);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to select a playlist for adding tracks/announcements
     */
    private void showPlaylistSelectionForAdd(boolean isTrack) {
        List<Playlist> playlists = playlistManager.getAllPlaylists();
        
        if (playlists.isEmpty()) {
            // Create new playlist first
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Playlists");
            builder.setMessage("Create a new playlist first to add " + (isTrack ? "tracks" : "announcements"));
            builder.setPositiveButton("Create", (d, w) -> {
                showCreatePlaylistDialog();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
            return;
        }
        
        String[] names = new String[playlists.size() + 1];
        names[0] = "Create New Playlist";
        for (int i = 0; i < playlists.size(); i++) {
            names[i + 1] = playlists.get(i).getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Playlist");
        builder.setItems(names, (dialog, which) -> {
            if (which == 0) {
                // Create new playlist, then add file
                showCreatePlaylistDialog(true);
            } else {
                Playlist playlist = playlists.get(which - 1);
                selectPlaylist(playlist);
                // After selecting, pick the file
                pickAudioFile(isTrack ? REQUEST_CODE_PICK_AUDIO : REQUEST_CODE_PICK_ANNOUNCEMENT);
            }
        });
        builder.show();
    }
    
    /**
     * Show dialog to select a playlist
     */
    private void showPlaylistSelectionDialog(boolean forAdding) {
        List<Playlist> playlists = playlistManager.getAllPlaylists();
        
        if (playlists.isEmpty()) {
            showCreatePlaylistDialog();
            return;
        }
        
        String[] names = new String[playlists.size() + 1];
        names[0] = "Create New Playlist";
        for (int i = 0; i < playlists.size(); i++) {
            names[i + 1] = playlists.get(i).getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Playlist");
        builder.setItems(names, (dialog, which) -> {
            if (which == 0) {
                showCreatePlaylistDialog();
            } else {
                Playlist playlist = playlists.get(which - 1);
                selectPlaylist(playlist);
            }
        });
        builder.show();
    }
    
    /**
     * Show dialog to manage playlists (rename, delete)
     */
    private void showManagePlaylistsDialog() {
        List<Playlist> playlists = playlistManager.getAllPlaylists();
        
        if (playlists.isEmpty()) {
            Toast.makeText(this, "No playlists to manage", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Playlists");
        builder.setItems(names, (dialog, which) -> {
            Playlist playlist = playlists.get(which);
            showPlaylistActionsDialog(playlist);
        });
        builder.show();
    }
    
    /**
     * Show actions for a specific playlist
     */
    private void showPlaylistActionsDialog(Playlist playlist) {
        String[] actions = {"Select", "Rename", "Delete"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playlist: " + playlist.getName());
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                selectPlaylist(playlist);
            } else if (which == 1) {
                showRenamePlaylistDialog(playlist);
            } else if (which == 2) {
                showDeletePlaylistDialog(playlist);
            }
        });
        builder.show();
    }
    
    /**
     * Show dialog to rename a playlist
     */
    private void showRenamePlaylistDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Playlist");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playlist.getName());
        builder.setView(input);
        
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playlistManager.updatePlaylistName(playlist.getId(), name);
                playlist.setName(name);
                updateUI();
                Toast.makeText(this, "Playlist renamed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to delete a playlist
     */
    private void showDeletePlaylistDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Playlist");
        builder.setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            playlistManager.deletePlaylist(playlist.getId());
            if (currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId())) {
                currentPlaylist = null;
                audioMixer.clearAll();
            }
            updateUI();
            Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Select a playlist and load it into the mixer
     */
    private void selectPlaylist(Playlist playlist) {
        currentPlaylist = playlist;
        audioMixer.loadPlaylist(playlist);
        updateUI();
        updateStatus("Playlist selected: " + playlist.getName());
    }
    
    /**
     * Request necessary permissions for file access
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        PERMISSION_REQUEST_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. File access may be limited.", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Open file picker to select an audio file
     */
    private void pickAudioFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        Intent chooser = Intent.createChooser(intent, getString(R.string.select_audio_file));
        startActivityForResult(chooser, requestCode);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && currentPlaylist != null) {
                loadAudioFile(uri, requestCode == REQUEST_CODE_PICK_AUDIO);
            }
        }
    }
    
    /**
     * Load an audio file from URI
     */
    private void loadAudioFile(Uri uri, boolean isMainTrack) {
        if (currentPlaylist == null) {
            Toast.makeText(this, "Please select a playlist first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateStatus("Loading audio file...");
        
        new Thread(() -> {
            try {
                File tempFile = File.createTempFile("audio_", ".mp3", getCacheDir());
                tempFile.deleteOnExit();
                
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("Could not open file stream");
                }
                
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                
                if (isMainTrack) {
                    AudioMixer.TrackData track = audioMixer.loadAudioFile(tempFile);
                    currentPlaylist.addTrack(track);
                    mainHandler.post(() -> {
                        updateStatus("Track added: " + track.name);
                        updateUI();
                        Toast.makeText(this, "Track added to " + currentPlaylist.getName(), 
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    AudioMixer.TrackData track = audioMixer.loadAudioFile(tempFile);
                    AudioMixer.AnnouncementData announcement = 
                        new AudioMixer.AnnouncementData(track.name, track.pcmData);
                    currentPlaylist.addAnnouncement(announcement);
                    mainHandler.post(() -> {
                        updateStatus("Announcement added: " + announcement.name);
                        updateUI();
                        Toast.makeText(this, "Announcement added to " + currentPlaylist.getName(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
                
                // Reload playlist to mixer
                audioMixer.loadPlaylist(currentPlaylist);
                tempFile.delete();
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    updateStatus("Error loading file");
                    Toast.makeText(this, "Error loading file: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * Update UI based on current playlist state
     */
    private void updateUI() {
        boolean hasPlaylist = (currentPlaylist != null);
        boolean canPlay = hasPlaylist && !currentPlaylist.isEmpty();
        
        // Enable/disable controls
        btnPlay.setEnabled(canPlay);
        btnPause.setEnabled(hasPlaylist);
        btnStop.setEnabled(hasPlaylist);
        seekMainVolume.setEnabled(hasPlaylist);
        seekAnnouncementVolume.setEnabled(hasPlaylist);
        seekAnnouncementInterval.setEnabled(hasPlaylist);
        checkPlayAtEnd.setEnabled(hasPlaylist);
        
        // Update playlist display
        if (currentPlaylist != null) {
            tvSelectedPlaylist.setText(getString(R.string.selected_playlist, currentPlaylist.getName()));
            updateTrackList();
            updateAnnouncementList();
        } else {
            tvSelectedPlaylist.setText(getString(R.string.no_playlist_selected));
            tvTracks.setText(getString(R.string.no_tracks));
            announcementAdapter.updateList(null);
        }
    }
    
    /**
     * Update the track list display
     */
    private void updateTrackList() {
        if (currentPlaylist == null) {
            tvTracks.setText(getString(R.string.no_tracks));
            return;
        }
        
        List<AudioMixer.TrackData> tracks = currentPlaylist.getTracks();
        StringBuilder sb = new StringBuilder();
        
        if (tracks.isEmpty()) {
            sb.append(getString(R.string.no_tracks));
        } else {
            for (int i = 0; i < tracks.size(); i++) {
                sb.append((i + 1)).append(". ").append(tracks.get(i).name);
                if (i < tracks.size() - 1) sb.append("\n");
            }
        }
        
        tvTracks.setText(sb.toString());
    }
    
    /**
     * Update the announcement list in RecyclerView
     */
    private void updateAnnouncementList() {
        if (currentPlaylist != null) {
            announcementAdapter.updateList(currentPlaylist.getAnnouncements());
        }
    }
    
    /**
     * Update status text
     */
    private void updateStatus(String status) {
        tvStatus.setText(status);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMixer != null) {
            audioMixer.release();
        }
    }
}
