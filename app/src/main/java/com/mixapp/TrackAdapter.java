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
 * Adapter for displaying tracks in a RecyclerView with drag-and-drop support
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<AudioMixer.TrackData> tracks;
    private OnItemMoveListener moveListener;
    private OnItemDeleteListener deleteListener;
    
    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }
    
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }
    
    public TrackAdapter(List<AudioMixer.TrackData> tracks, OnItemMoveListener moveListener, OnItemDeleteListener deleteListener) {
        this.tracks = tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
        this.moveListener = moveListener;
        this.deleteListener = deleteListener;
    }
    
    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        AudioMixer.TrackData track = tracks.get(position);
        holder.tvName.setText((position + 1) + ". " + track.name);
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tracks.size();
    }
    
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                java.util.Collections.swap(tracks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                java.util.Collections.swap(tracks, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }
    
    public void removeItem(int position) {
        if (position >= 0 && position < tracks.size()) {
            tracks.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tracks.size());
        }
    }
    
    public void updateList(List<AudioMixer.TrackData> newTracks) {
        this.tracks = newTracks != null ? new ArrayList<>(newTracks) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<AudioMixer.TrackData> getTracks() {
        return new ArrayList<>(tracks);
    }
    
    static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageButton btnDelete;
        
        TrackViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTrackName);
            btnDelete = itemView.findViewById(R.id.btnDeleteTrack);
        }
    }
}

