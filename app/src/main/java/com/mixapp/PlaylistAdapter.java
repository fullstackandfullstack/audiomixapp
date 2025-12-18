package com.mixapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }
    
    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists != null ? new ArrayList<>(playlists) : new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
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
        
        PlaylistViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
            tvInfo = itemView.findViewById(android.R.id.text2);
        }
    }
}

