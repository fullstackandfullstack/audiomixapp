package com.mixapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying playlists in a RecyclerView
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;
    private OnPlaylistDeleteListener deleteListener;
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }
    
    public interface OnPlaylistDeleteListener {
        void onPlaylistDelete(Playlist playlist);
    }
    
    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener, OnPlaylistDeleteListener deleteListener) {
        this.playlists = playlists != null ? new ArrayList<>(playlists) : new ArrayList<>();
        this.listener = listener;
        this.deleteListener = deleteListener;
    }
    
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvName.setText(playlist.getName());
        holder.tvInfo.setText(playlist.getTracks().size() + " tracks, " + 
                             playlist.getAnnouncements().size() + " announcements");
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onPlaylistDelete(playlist);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return playlists.size();
    }
    
    public void updateList(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists != null ? new ArrayList<>(newPlaylists) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvInfo;
        ImageButton btnDelete;
        
        PlaylistViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvInfo = itemView.findViewById(R.id.tvPlaylistInfo);
            btnDelete = itemView.findViewById(R.id.btnDeletePlaylist);
        }
    }
}

