package net.calvuz.qdue.ui.features.assignment.list.component;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * AssignmentListItemDecoration - RecyclerView Item Spacing Decoration
 *
 * <p>Custom ItemDecoration for consistent spacing between assignment cards
 * in the RecyclerView list, following Material Design spacing guidelines.</p>
 *
 * <h3>Spacing Behavior:</h3>
 * <ul>
 *   <li><strong>Top Item</strong>: No extra top spacing (handled by RecyclerView padding)</li>
 *   <li><strong>Middle Items</strong>: Consistent spacing between cards</li>
 *   <li><strong>Bottom Item</strong>: Extra bottom spacing for FAB clearance</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0 - Assignment List UI
 * @since Clean Architecture Phase 2
 */
public class AssignmentListItemDecoration extends RecyclerView.ItemDecoration {

    private final int mSpacing;

    /**
     * Create item decoration with specified spacing.
     *
     * @param spacing Spacing between items in pixels
     */
    public AssignmentListItemDecoration(int spacing) {
        this.mSpacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        int position = parent.getChildAdapterPosition(view);
        int itemCount = state.getItemCount();

        // Apply spacing logic
        if (position == 0) {
            // First item: no extra top spacing (handled by RecyclerView padding)
            outRect.top = 0;
        } else {
            // All other items: top spacing
            outRect.top = mSpacing;
        }

        // No horizontal spacing (handled by card margins)
        outRect.left = 0;
        outRect.right = 0;

        // Bottom spacing for last item (extra space for FAB)
        if (position == itemCount - 1) {
            outRect.bottom = mSpacing * 2; // Extra space for FAB
        } else {
            outRect.bottom = 0;
        }
    }
}