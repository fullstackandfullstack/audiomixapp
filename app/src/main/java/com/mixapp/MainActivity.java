package com.mixapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
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
    private SeekBar seekTrackProgress;
    private SeekBar seekPlaylistProgress;
    private TextView tvTrackProgress;
    private TextView tvTrackDuration;
    private TextView tvPlaylistProgress;
    private TextView tvPlaylistDuration;
    private CheckBox checkPlayAtEnd;
    private TextView tvIntervalValue;
    private TextView tvSelectedPlaylist;
    private TextView tvMenuPath;
    private RecyclerView recyclerTracks;
    private RecyclerView recyclerAnnouncements;
    
    private Handler mainHandler;
    private AnnouncementAdapter announcementAdapter;
    private TrackAdapter trackAdapter;
    private PlaylistAdapter playlistAdapter;
    private boolean isAddingTrack = false;
    private boolean isHomeScreen = true;
    private View homeView;
    private RecyclerView recyclerPlaylists;
    private TextView tvEmptyState;
    private ProgressDialog loadingProgressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize managers
        playlistManager = new PlaylistManager(this);
        audioMixer = new AudioMixer();
        audioMixer.initialize();
        
        // Load all playlists on startup
        loadAllPlaylists();
        
        // Show home screen first
        showHomeScreen();
        
        // Request permissions
        requestPermissions();
    }
    
    /**
     * Show the home screen with list of playlists
     */
    private void showHomeScreen() {
        isHomeScreen = true;
        setContentView(R.layout.activity_home);
        
        tvMenuPath = findViewById(R.id.tvMenuPath);
        recyclerPlaylists = findViewById(R.id.recyclerPlaylists);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        
        setClickableBreadcrumb("Home", null);
        
        // Setup playlist RecyclerView
        playlistAdapter = new PlaylistAdapter(
            playlistManager.getAllPlaylists(),
            playlist -> {
                // Open playlist detail screen
                showPlaylistDetail(playlist);
            },
            playlist -> {
                // Delete playlist from home screen
                showDeletePlaylistDialog(playlist);
            }
        );
        recyclerPlaylists.setLayoutManager(new LinearLayoutManager(this));
        recyclerPlaylists.setAdapter(playlistAdapter);
        
        // Menu button
        btnMenu.setOnClickListener(v -> showHomeMenu());
        
        updateHomeScreen();
    }
    
    /**
     * Show playlist detail screen
     */
    private void showPlaylistDetail(Playlist playlist) {
        isHomeScreen = false;
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        initializeViews();
        setupListeners();
        setupRecyclerView();
        
        // Select the playlist
        selectPlaylist(playlist);
    }
    
    /**
     * Show home menu
     */
    private void showHomeMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.btnMenu));
        popup.getMenu().add("Create New Playlist");
        popup.getMenu().add("Lists");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Create New Playlist")) {
                showCreatePlaylistDialog();
            } else if (title.equals("Lists")) {
                // Already on lists screen
            }
            return true;
        });
        popup.show();
    }
    
    /**
     * Update home screen display
     */
    private void updateHomeScreen() {
        List<Playlist> playlists = playlistManager.getAllPlaylists();
        // Reload all playlists to ensure accurate track/announcement counts
        List<Playlist> reloadedPlaylists = new ArrayList<>();
        for (Playlist playlist : playlists) {
            Playlist loaded = playlistManager.loadPlaylist(playlist.getId());
            if (loaded != null) {
                reloadedPlaylists.add(loaded);
            } else {
                reloadedPlaylists.add(playlist);
            }
        }
        
        if (reloadedPlaylists.isEmpty()) {
            recyclerPlaylists.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerPlaylists.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
            playlistAdapter.updateList(reloadedPlaylists);
        }
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
        seekTrackProgress = findViewById(R.id.seekTrackProgress);
        seekPlaylistProgress = findViewById(R.id.seekPlaylistProgress);
        tvTrackProgress = findViewById(R.id.tvTrackProgress);
        tvTrackDuration = findViewById(R.id.tvTrackDuration);
        tvPlaylistProgress = findViewById(R.id.tvPlaylistProgress);
        tvPlaylistDuration = findViewById(R.id.tvPlaylistDuration);
        checkPlayAtEnd = findViewById(R.id.checkPlayAtEnd);
        tvIntervalValue = findViewById(R.id.tvIntervalValue);
        tvSelectedPlaylist = findViewById(R.id.tvSelectedPlaylist);
        tvMenuPath = findViewById(R.id.tvMenuPath);
        recyclerTracks = findViewById(R.id.recyclerTracks);
        recyclerAnnouncements = findViewById(R.id.recyclerAnnouncements);
    }
    
    /**
     * Setup RecyclerView for tracks and announcements with drag-and-drop
     */
    private void setupRecyclerView() {
        // Setup tracks RecyclerView
        trackAdapter = new TrackAdapter(
            currentPlaylist != null ? currentPlaylist.getTracks() : null,
            (fromPosition, toPosition) -> {
                if (currentPlaylist != null && audioMixer != null) {
                    // Get the current order from adapter
                    List<AudioMixer.TrackData> adapterList = trackAdapter.getTracks();
                    if (adapterList != null && !adapterList.isEmpty()) {
                        // Update playlist to match adapter order
                        List<AudioMixer.TrackData> newList = new ArrayList<>();
                        for (AudioMixer.TrackData track : adapterList) {
                            newList.add(track);
                        }
                        
                        // Replace playlist tracks with new ordered list
                        currentPlaylist.getTracks().clear();
                        currentPlaylist.getTracks().addAll(newList);
                        
                        // Save and reload
                        playlistManager.savePlaylist(currentPlaylist);
                        boolean wasPlaying = audioMixer.isPlaying();
                        if (wasPlaying) {
                            audioMixer.stop();
                        }
                        audioMixer.loadPlaylist(currentPlaylist);
                        if (wasPlaying) {
                            audioMixer.play();
                        }
                    }
                }
            },
            (position) -> {
                // Delete track with confirmation
                if (currentPlaylist != null && position >= 0 && position < currentPlaylist.getTracks().size()) {
                    AudioMixer.TrackData track = currentPlaylist.getTracks().get(position);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Delete Track");
                    builder.setMessage("Are you sure you want to delete \"" + track.name + "\"?");
                    builder.setPositiveButton("Delete", (dialog, which) -> {
                        currentPlaylist.getTracks().remove(position);
                        trackAdapter.removeItem(position);
                        playlistManager.savePlaylist(currentPlaylist);
                        boolean wasPlaying = audioMixer.isPlaying();
                        if (wasPlaying) {
                            audioMixer.stop();
                        }
                        audioMixer.loadPlaylist(currentPlaylist);
                        if (wasPlaying && !currentPlaylist.getTracks().isEmpty()) {
                            audioMixer.play();
                        }
                        updateTrackList();
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }
            }
        );
        
        if (recyclerTracks != null) {
            recyclerTracks.setLayoutManager(new LinearLayoutManager(this));
            recyclerTracks.setAdapter(trackAdapter);
            
            // Enable drag-and-drop for tracks
            ItemTouchHelper.Callback trackCallback = new ItemTouchHelperCallback(trackAdapter);
            ItemTouchHelper trackTouchHelper = new ItemTouchHelper(trackCallback);
            trackTouchHelper.attachToRecyclerView(recyclerTracks);
        }
        
        // Setup announcements RecyclerView
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
            },
            (position) -> {
                // Delete announcement with confirmation
                if (currentPlaylist != null && position >= 0 && position < currentPlaylist.getAnnouncements().size()) {
                    AudioMixer.AnnouncementData announcement = currentPlaylist.getAnnouncements().get(position);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Delete Announcement");
                    builder.setMessage("Are you sure you want to delete \"" + announcement.name + "\"?");
                    builder.setPositiveButton("Delete", (dialog, which) -> {
                        currentPlaylist.getAnnouncements().remove(position);
                        announcementAdapter.removeItem(position);
                        playlistManager.savePlaylist(currentPlaylist);
                        boolean wasPlaying = audioMixer.isPlaying();
                        if (wasPlaying) {
                            audioMixer.stop();
                        }
                        audioMixer.loadPlaylist(currentPlaylist);
                        if (wasPlaying) {
                            audioMixer.play();
                        }
                        updateAnnouncementList();
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
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
                    tvIntervalValue.setText(progress + "s");
                    
                    // If interval > 0, automatically uncheck "Play at End Only"
                    if (progress > 0) {
                        if (checkPlayAtEnd.isChecked()) {
                            checkPlayAtEnd.setChecked(false);
                        }
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
                
                // If "Play at End Only" is checked, set interval to 0
                if (isChecked) {
                    seekAnnouncementInterval.setProgress(0);
                    tvIntervalValue.setText("0s");
                    audioMixer.setAnnouncementInterval(0);
                }
            }
        });
        
        // Click on selected playlist to change it
        tvSelectedPlaylist.setOnClickListener(v -> showPlaylistSelectionDialog(true));
        
        // Track progress seekbar
        seekTrackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean wasPlaying = false;
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentPlaylist != null) {
                    // Update time display while seeking
                    long trackDuration = audioMixer.getCurrentTrackDuration();
                    if (trackDuration > 0) {
                        long seekPosition = (long) (progress / 100.0 * trackDuration);
                        tvTrackProgress.setText(formatTime(seekPosition));
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                wasPlaying = audioMixer.isPlaying();
                if (wasPlaying) {
                    audioMixer.pause();
                }
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (currentPlaylist != null) {
                    int progress = seekBar.getProgress();
                    long trackDuration = audioMixer.getCurrentTrackDuration();
                    if (trackDuration > 0) {
                        long seekPosition = (long) (progress / 100.0 * trackDuration);
                        audioMixer.seekToTrackPosition(seekPosition);
                        if (wasPlaying) {
                            audioMixer.play();
                        }
                    }
                }
            }
        });
        
        // Playlist progress seekbar
        seekPlaylistProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean wasPlaying = false;
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentPlaylist != null) {
                    // Update time display while seeking
                    long playlistDuration = audioMixer.getPlaylistDuration();
                    if (playlistDuration > 0) {
                        long seekPosition = (long) (progress / 100.0 * playlistDuration);
                        tvPlaylistProgress.setText(formatTime(seekPosition));
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                wasPlaying = audioMixer.isPlaying();
                if (wasPlaying) {
                    audioMixer.pause();
                }
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (currentPlaylist != null) {
                    int progress = seekBar.getProgress();
                    long playlistDuration = audioMixer.getPlaylistDuration();
                    if (playlistDuration > 0) {
                        long seekPosition = (long) (progress / 100.0 * playlistDuration);
                        audioMixer.seekToPlaylistPosition(seekPosition);
                        if (wasPlaying) {
                            audioMixer.play();
                        }
                    }
                }
            }
        });
        
        // Start progress update handler
        startProgressUpdater();
    }
    
    /**
     * Format milliseconds to MM:SS
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Start updating progress bars during playback
     */
    private void startProgressUpdater() {
        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (audioMixer != null && currentPlaylist != null) {
                    if (audioMixer.isPlaying()) {
                        // Update track progress
                        long trackPosition = audioMixer.getCurrentTrackPosition();
                        long trackDuration = audioMixer.getCurrentTrackDuration();
                        if (trackDuration > 0) {
                            int trackProgress = (int) (trackPosition * 100 / trackDuration);
                            seekTrackProgress.setProgress(trackProgress);
                            tvTrackProgress.setText(formatTime(trackPosition));
                            tvTrackDuration.setText(formatTime(trackDuration));
                        }
                        
                        // Update playlist progress
                        long playlistPosition = audioMixer.getCurrentPlaylistPosition();
                        long playlistDuration = audioMixer.getPlaylistDuration();
                        if (playlistDuration > 0) {
                            int playlistProgress = (int) (playlistPosition * 100 / playlistDuration);
                            seekPlaylistProgress.setProgress(playlistProgress);
                            tvPlaylistProgress.setText(formatTime(playlistPosition));
                            tvPlaylistDuration.setText(formatTime(playlistDuration));
                        }
                    }
                }
                progressHandler.postDelayed(this, 100); // Update every 100ms
            }
        };
        progressHandler.post(progressRunnable);
    }
    
    /**
     * Show playlist menu (create, select, manage)
     */
    private void showPlaylistMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenu().add("Home");
        popup.getMenu().add("Create New Playlist");
        popup.getMenu().add("Select Playlist");
        popup.getMenu().add("Manage Playlists");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Home")) {
                showHomeScreen();
            } else if (title.equals("Create New Playlist")) {
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
                if (isHomeScreen) {
                    // If on home screen, update it
                    updateHomeScreen();
                    // Then show playlist detail
                    showPlaylistDetail(playlist);
                } else {
                    selectPlaylist(playlist);
                }
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
                if (isHomeScreen) {
                    updateHomeScreen();
                } else {
                    updateUI();
                }
                Toast.makeText(this, "Playlist renamed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show dialog to delete a playlist with confirmation
     */
    private void showDeletePlaylistDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Playlist");
        builder.setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?\n\n" +
                          "This will permanently delete the playlist and all its tracks and announcements.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            playlistManager.deletePlaylist(playlist.getId());
            if (currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId())) {
                currentPlaylist = null;
                audioMixer.clearAll();
                // Go back to home screen
                showHomeScreen();
            } else if (isHomeScreen) {
                updateHomeScreen();
            } else {
                updateUI();
            }
            Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Load all playlists from storage
     */
    private void loadAllPlaylists() {
        List<Playlist> savedPlaylists = playlistManager.getAllPlaylists();
        for (Playlist playlist : savedPlaylists) {
            // Playlists are already loaded by PlaylistManager
            Log.d(TAG, "Loaded playlist: " + playlist.getName() + 
                  " (" + playlist.getTracks().size() + " tracks, " + 
                  playlist.getAnnouncements().size() + " announcements)");
        }
    }
    
    /**
     * Select a playlist and load it into the mixer
     */
    private void selectPlaylist(Playlist playlist) {
        // Reload playlist from storage to ensure we have latest data
        Playlist loadedPlaylist = playlistManager.loadPlaylist(playlist.getId());
        if (loadedPlaylist != null) {
            currentPlaylist = loadedPlaylist;
        } else {
            currentPlaylist = playlist;
        }
        audioMixer.loadPlaylist(currentPlaylist);
        
        // Set default interval to 0
        seekAnnouncementInterval.setProgress(0);
        tvIntervalValue.setText("0s");
        audioMixer.setAnnouncementInterval(0);
        
        updateUI();
        updateStatus("Playlist selected: " + currentPlaylist.getName());
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
     * Open file picker to select an audio file (supports multiple selection)
     */
    private void pickAudioFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        
        Intent chooser = Intent.createChooser(intent, getString(R.string.select_audio_file));
        startActivityForResult(chooser, requestCode);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (currentPlaylist == null) {
                Toast.makeText(this, "Please select a playlist first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Handle multiple file selection
            if (data.getClipData() != null) {
                // Multiple files selected
                android.content.ClipData clipData = data.getClipData();
                int count = clipData.getItemCount();
                int maxFiles = 10;
                
                if (count > maxFiles) {
                    Toast.makeText(this, "Maximum " + maxFiles + " files allowed. Selecting first " + maxFiles + " files.", 
                            Toast.LENGTH_LONG).show();
                    count = maxFiles;
                }
                
                // Check individual file sizes (allow up to 200MB per file)
                // Note: Large files will take longer to decode, but are supported
                List<Uri> urisToLoad = new ArrayList<>();
                final long maxFileSizeMB = 200; // 200MB per file limit
                final int[] skippedCount = {0};
                
                for (int i = 0; i < count; i++) {
                    final int fileIndex = i + 1;
                    Uri uri = clipData.getItemAt(i).getUri();
                    try {
                        android.content.ContentResolver resolver = getContentResolver();
                        android.database.Cursor cursor = resolver.query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                            if (sizeIndex >= 0) {
                                long fileSize = cursor.getLong(sizeIndex);
                                final long fileSizeMB = fileSize / (1024 * 1024);
                                
                                if (fileSizeMB <= maxFileSizeMB) {
                                    urisToLoad.add(uri);
                                } else {
                                    skippedCount[0]++;
                                    final long finalFileSizeMB = fileSizeMB;
                                    mainHandler.post(() -> {
                                        Toast.makeText(this, "File " + fileIndex + " skipped: " + finalFileSizeMB + " MB exceeds " + maxFileSizeMB + " MB limit", 
                                                Toast.LENGTH_LONG).show();
                                    });
                                }
                            } else {
                                // Size not available, try to load it anyway
                                urisToLoad.add(uri);
                            }
                            cursor.close();
                        } else {
                            // Can't query file info, try to load it anyway
                            urisToLoad.add(uri);
                        }
                    } catch (Exception e) {
                        // If we can't check size, try to load it anyway
                        urisToLoad.add(uri);
                    }
                }
                
                if (skippedCount[0] > 0) {
                    final int finalSkippedCount = skippedCount[0];
                    mainHandler.post(() -> {
                        Toast.makeText(this, finalSkippedCount + " file(s) skipped due to size limit (" + maxFileSizeMB + " MB per file)", 
                                Toast.LENGTH_LONG).show();
                    });
                }
                
                // Load files sequentially
                loadMultipleAudioFiles(urisToLoad, requestCode == REQUEST_CODE_PICK_AUDIO);
            } else if (data.getData() != null) {
                // Single file selected
                Uri uri = data.getData();
                loadAudioFile(uri, requestCode == REQUEST_CODE_PICK_AUDIO);
            }
        }
    }
    
    /**
     * Load multiple audio files sequentially
     */
    private void loadMultipleAudioFiles(List<Uri> uris, boolean isMainTrack) {
        if (uris.isEmpty()) {
            return;
        }
        
        // Load first file immediately, then queue the rest
        loadAudioFile(uris.get(0), isMainTrack);
        
        // Load remaining files after a short delay (to avoid overwhelming the system)
        if (uris.size() > 1) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                for (int i = 1; i < uris.size(); i++) {
                    final int index = i;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        loadAudioFile(uris.get(index), isMainTrack);
                    }, i * 500); // 500ms delay between each file
                }
            }, 1000); // Wait 1 second before starting next batch
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
        
        // Show progress dialog for large file loading
        mainHandler.post(() -> {
            if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                loadingProgressDialog.dismiss();
            }
            loadingProgressDialog = new ProgressDialog(this);
            loadingProgressDialog.setMessage("Loading audio file...\nThis may take a while for large files.");
            loadingProgressDialog.setIndeterminate(true);
            loadingProgressDialog.setCancelable(false);
            loadingProgressDialog.show();
        });
        
        updateStatus("Loading audio file...");
        
        new Thread(() -> {
            File tempFile = null;
            try {
                // Extract original filename from URI
                String fileName = getFileNameFromUri(uri);
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "audio_" + System.currentTimeMillis();
                }
                
                // Determine file extension
                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                    extension = fileName.substring(lastDot);
                }
                
                // Create temp file with original extension
                tempFile = File.createTempFile("audio_", extension, getCacheDir());
                tempFile.deleteOnExit();
                
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("Could not open file stream");
                }
                
                // Copy file and check size
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                long fileSize = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    fileSize += bytesRead;
                }
                outputStream.close();
                inputStream.close();
                
                // Check file size - warn if very large (over 100MB), but allow up to 200MB
                long fileSizeMB = fileSize / (1024 * 1024);
                if (fileSizeMB > 200) {
                    Log.w(TAG, "Very large file detected: " + fileSizeMB + " MB");
                    mainHandler.post(() -> {
                        Toast.makeText(this, "File is very large (" + fileSizeMB + " MB). This may take a long time to decode or may fail due to memory limits.", 
                                Toast.LENGTH_LONG).show();
                    });
                } else if (fileSizeMB > 100) {
                    Log.w(TAG, "Large file detected: " + fileSizeMB + " MB");
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Large file detected (" + fileSizeMB + " MB). Decoding may take a while...", 
                                Toast.LENGTH_LONG).show();
                    });
                }
                
                // Update progress message
                mainHandler.post(() -> {
                    if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                        loadingProgressDialog.setMessage("Decoding audio file...\nFile size: " + fileSizeMB + " MB\nThis may take a while...");
                    }
                });
                
                // Create PCM output file path
                File playlistDir = new File(getFilesDir(), "playlists");
                if (!playlistDir.exists()) {
                    playlistDir.mkdirs();
                }
                
                // Generate unique filename for PCM data
                String pcmFileName;
                if (isMainTrack) {
                    int trackIndex = currentPlaylist.getTracks().size();
                    pcmFileName = currentPlaylist.getId() + "_track_" + trackIndex + ".pcm";
                } else {
                    int annIndex = currentPlaylist.getAnnouncements().size();
                    pcmFileName = currentPlaylist.getId() + "_ann_" + annIndex + ".pcm";
                }
                File pcmOutputFile = new File(playlistDir, pcmFileName);
                
                // Decode audio file and save PCM to disk
                AudioMixer.TrackData track;
                try {
                    track = audioMixer.loadAudioFile(tempFile, pcmOutputFile, fileName);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Out of memory while decoding file", e);
                    throw new Exception("File is too large to load. Please use a smaller file or split it into smaller parts.");
                }
                
                // Add to playlist
                if (isMainTrack) {
                    currentPlaylist.addTrack(track);
                    playlistManager.savePlaylist(currentPlaylist); // Save after adding
                    mainHandler.post(() -> {
                        if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                            loadingProgressDialog.dismiss();
                        }
                        updateStatus("Track added: " + track.name);
                        updateUI();
                        // Update home screen if we're on it
                        if (isHomeScreen) {
                            updateHomeScreen();
                        }
                        Toast.makeText(this, "Track added to " + currentPlaylist.getName(), 
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // Create announcement with file reference (same file as track)
                    AudioMixer.AnnouncementData announcement = 
                        new AudioMixer.AnnouncementData(track.name, track.pcmFile, track.sampleCount);
                    currentPlaylist.addAnnouncement(announcement);
                    playlistManager.savePlaylist(currentPlaylist); // Save after adding
                    mainHandler.post(() -> {
                        if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                            loadingProgressDialog.dismiss();
                        }
                        updateStatus("Announcement added: " + announcement.name);
                        updateUI();
                        // Update home screen if we're on it
                        if (isHomeScreen) {
                            updateHomeScreen();
                        }
                        Toast.makeText(this, "Announcement added to " + currentPlaylist.getName(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
                
                // Save playlist after adding track/announcement
                playlistManager.savePlaylist(currentPlaylist);
                
                // Reload playlist to mixer
                audioMixer.loadPlaylist(currentPlaylist);
                
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory error", e);
                mainHandler.post(() -> {
                    if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                        loadingProgressDialog.dismiss();
                    }
                    updateStatus("Error: File too large");
                    Toast.makeText(this, "File is too large to load. The app ran out of memory.\nPlease use a smaller file.", 
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading file", e);
                mainHandler.post(() -> {
                    if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                        loadingProgressDialog.dismiss();
                    }
                    updateStatus("Error loading file");
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Unknown error occurred";
                    }
                    Toast.makeText(this, "Error loading file: " + errorMsg, 
                            Toast.LENGTH_LONG).show();
                });
            } catch (Throwable t) {
                // Catch any other errors (including OutOfMemoryError if not caught above)
                Log.e(TAG, "Unexpected error loading file", t);
                mainHandler.post(() -> {
                    if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                        loadingProgressDialog.dismiss();
                    }
                    updateStatus("Error loading file");
                    Toast.makeText(this, "Unexpected error: " + t.getClass().getSimpleName() + "\n" + 
                            (t.getMessage() != null ? t.getMessage() : ""), 
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                // Clean up temp file
                if (tempFile != null && tempFile.exists()) {
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        Log.w(TAG, "Could not delete temp file", e);
                    }
                }
                // Ensure progress dialog is dismissed
                mainHandler.post(() -> {
                    if (loadingProgressDialog != null && loadingProgressDialog.isShowing()) {
                        loadingProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }
    
    /**
     * Extract filename from URI
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        
        // Try to get filename from ContentResolver
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // Fallback: try to extract from URI path
        if (fileName == null || fileName.isEmpty()) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }
        
        return fileName;
    }
    
    /**
     * Set clickable breadcrumb navigation text
     */
    private void setClickableBreadcrumb(String text, String playlistName) {
        SpannableString spannable = new SpannableString(text);
        
        // Make "Home" clickable
        int homeStart = text.indexOf("Home");
        if (homeStart >= 0) {
            int homeEnd = homeStart + "Home".length();
            ClickableSpan homeClickable = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    showHomeScreen();
                }
            };
            spannable.setSpan(homeClickable, homeStart, homeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(0xFFFFFFFF), homeStart, homeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // Make "Lists" clickable
        int listsStart = text.indexOf("Lists");
        if (listsStart >= 0) {
            int listsEnd = listsStart + "Lists".length();
            ClickableSpan listsClickable = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (!isHomeScreen) {
                        // Show playlist selection dialog
                        showPlaylistSelectionDialog(false);
                    }
                }
            };
            spannable.setSpan(listsClickable, listsStart, listsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(0xFFFFFFFF), listsStart, listsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvMenuPath.setText(spannable);
        tvMenuPath.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
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
        seekTrackProgress.setEnabled(hasPlaylist);
        seekPlaylistProgress.setEnabled(hasPlaylist);
        checkPlayAtEnd.setEnabled(hasPlaylist);
        
        // Update playlist display
        if (currentPlaylist != null) {
            tvSelectedPlaylist.setText(getString(R.string.selected_playlist, currentPlaylist.getName()));
            if (tvMenuPath != null) {
                setClickableBreadcrumb("Home > Lists > " + currentPlaylist.getName(), currentPlaylist.getName());
            }
            updateTrackList();
            updateAnnouncementList();
        } else {
            tvSelectedPlaylist.setText(getString(R.string.no_playlist_selected));
            if (tvMenuPath != null) {
                setClickableBreadcrumb("Home > Lists >", null);
            }
            announcementAdapter.updateList(null);
        }
    }
    
    /**
     * Update the track list display
     */
    private void updateTrackList() {
        if (currentPlaylist == null) {
            if (trackAdapter != null) {
                trackAdapter.updateList(null);
            }
            return;
        }
        
        List<AudioMixer.TrackData> tracks = currentPlaylist.getTracks();
        
        if (recyclerTracks != null && trackAdapter != null) {
            // Use RecyclerView
            trackAdapter.updateList(tracks);
            recyclerTracks.setVisibility(tracks.isEmpty() ? View.GONE : View.VISIBLE);
        }
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
        // Status display removed - can use Toast if needed
        // Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioMixer != null) {
            audioMixer.release();
        }
    }
}
