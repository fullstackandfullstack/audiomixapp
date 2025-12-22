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
 * Adapter for displaying announcements in a RecyclerView with drag-and-drop reordering
 */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<AudioMixer.AnnouncementData> announcements;
    private OnItemMoveListener moveListener;
    private OnItemDeleteListener deleteListener;
    
    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }
    
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }
    
    public AnnouncementAdapter(List<AudioMixer.AnnouncementData> announcements, OnItemMoveListener moveListener, OnItemDeleteListener deleteListener) {
        // Create a new list to avoid reference issues
        this.announcements = announcements != null ? new ArrayList<>(announcements) : new ArrayList<>();
        this.moveListener = moveListener;
        this.deleteListener = deleteListener;
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
                .inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (announcements != null && position < announcements.size()) {
            AudioMixer.AnnouncementData ann = announcements.get(position);
            holder.textView.setText((position + 1) + ". " + ann.name);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onItemDelete(position);
                }
            });
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
    
    public void removeItem(int position) {
        if (position >= 0 && position < announcements.size()) {
            announcements.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, announcements.size());
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
        ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvAnnouncementName);
            btnDelete = itemView.findViewById(R.id.btnDeleteAnnouncement);
        }
    }
}

