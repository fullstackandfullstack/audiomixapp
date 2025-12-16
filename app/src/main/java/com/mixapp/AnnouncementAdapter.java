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
 * Adapter for displaying announcements in a RecyclerView with drag-and-drop reordering
 */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<AudioMixer.AnnouncementData> announcements;
    private OnItemMoveListener moveListener;
    
    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }
    
    public AnnouncementAdapter(List<AudioMixer.AnnouncementData> announcements, OnItemMoveListener moveListener) {
        // Create a new list to avoid reference issues
        this.announcements = announcements != null ? new ArrayList<>(announcements) : new ArrayList<>();
        this.moveListener = moveListener;
    }
    
    /**
     * Get the current announcements list
     */
    public List<AudioMixer.AnnouncementData> getAnnouncements() {
        return announcements;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (announcements != null && position < announcements.size()) {
            AudioMixer.AnnouncementData ann = announcements.get(position);
            holder.textView.setText((position + 1) + ". " + ann.name);
            // Make item view draggable
            holder.itemView.setTag(position);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            // Partial update - just update the position number
            if (announcements != null && position < announcements.size()) {
                holder.textView.setText((position + 1) + ". " + announcements.get(position).name);
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return announcements != null ? announcements.size() : 0;
    }
    
    /**
     * Move item from one position to another
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (announcements == null || announcements.isEmpty()) {
            return;
        }
        
        if (fromPosition < 0 || fromPosition >= announcements.size() ||
            toPosition < 0 || toPosition >= announcements.size()) {
            return;
        }
        
        if (fromPosition == toPosition) {
            return;
        }
        
        // Move the item in the list
        AudioMixer.AnnouncementData item = announcements.remove(fromPosition);
        announcements.add(toPosition, item);
        
        // Notify adapter of the move
        notifyItemMoved(fromPosition, toPosition);
        
        // Notify listener to update playlist
        if (moveListener != null) {
            moveListener.onItemMove(fromPosition, toPosition);
        }
    }
    
    public void updateList(List<AudioMixer.AnnouncementData> newList) {
        if (newList == null) {
            this.announcements = new ArrayList<>();
        } else {
            // Create a new list to avoid reference issues
            this.announcements = new ArrayList<>(newList);
        }
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

