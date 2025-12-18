package com.mixapp;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Callback for handling drag-and-drop in RecyclerView
 */
public class ItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
    private AnnouncementAdapter announcementAdapter;
    private TrackAdapter trackAdapter;
    
    public ItemTouchHelperCallback(AnnouncementAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        this.announcementAdapter = adapter;
    }
    
    public ItemTouchHelperCallback(TrackAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        this.trackAdapter = adapter;
    }
    
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, 
                         @NonNull RecyclerView.ViewHolder viewHolder, 
                         @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        
        // Validate positions
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        
        if (fromPosition == toPosition) {
            return false;
        }
        
        if (announcementAdapter != null) {
            announcementAdapter.moveItem(fromPosition, toPosition);
        } else if (trackAdapter != null) {
            trackAdapter.moveItem(fromPosition, toPosition);
        }
        return true;
    }
    
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used for drag-and-drop
    }
    
    @Override
    public boolean isLongPressDragEnabled() {
        return true; // Enable long press to start dragging
    }
    
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Add visual feedback when dragging
            if (viewHolder != null) {
                viewHolder.itemView.setAlpha(0.7f);
            }
        }
    }
    
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Restore visual state after drag
        viewHolder.itemView.setAlpha(1.0f);
    }
}

