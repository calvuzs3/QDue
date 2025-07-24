/**
 * RecyclerView adapter for selection toolbar actions
 * Supports i18n and custom styling through providers
 */
package net.calvuz.qdue.ui.core.components.selection;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;
import net.calvuz.qdue.ui.core.components.selection.styling.SelectionActionStyleProvider;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying selection actions in a RecyclerView
 *
 * @param <T> The type of items being selected
 */
public class SelectionToolbarAdapter<T> extends RecyclerView.Adapter<SelectionToolbarAdapter.ActionViewHolder> {

    private static final String TAG = "SelectionToolbarAdapter";

    private final Context mContext;
    private final SelectionActionStyleProvider mStyleProvider;
    private List<SelectionAction<T>> mActions = new ArrayList<>();
    private OnActionClickListener<T> mClickListener;

    /**
     * Constructor
     *
     * @param context Android context for resources
     * @param styleProvider Provider for action button styling
     */
    public SelectionToolbarAdapter(@NonNull Context context,
                                   @NonNull SelectionActionStyleProvider styleProvider) {
        mContext = context;
        mStyleProvider = styleProvider;
    }

    /**
     * ViewHolder for action buttons
     */
    static class ActionViewHolder extends RecyclerView.ViewHolder {
        final MaterialButton actionButton;

        ActionViewHolder(@NonNull MaterialButton button) {
            super(button);
            actionButton = button;
        }
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create button with style provider
        MaterialButton button = mStyleProvider.createActionButton(mContext, parent);
        return new ActionViewHolder(button);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        SelectionAction<T> action = mActions.get(position);

        // Apply action-specific styling
        mStyleProvider.styleActionButton(holder.actionButton, action);

        // Set localized content description for accessibility
        holder.actionButton.setContentDescription(action.getContentDescription(mContext));

        // Set click listener with enhanced feedback
        holder.actionButton.setOnClickListener(v -> {
            if (mClickListener != null) {
                // Haptic feedback
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                // Visual feedback
                animateActionClick(holder.actionButton);

                // Trigger callback
                mClickListener.onActionClicked(action);

                Log.d(TAG, "Action clicked: " + action.getId());
            }
        });

        // Set tooltip (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            holder.actionButton.setTooltipText(action.getLabel(mContext));
        }
    }

    @Override
    public int getItemCount() {
        return mActions.size();
    }

    /**
     * Animate button click
     */
    private void animateActionClick(@NonNull MaterialButton button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    /**
     * Update the list of actions
     */
    public void updateActions(@NonNull List<SelectionAction<T>> actions) {
        mActions = new ArrayList<>(actions);
        notifyDataSetChanged();

        Log.d(TAG, "Actions updated: " + actions.size() + " items");
    }

    /**
     * Clear all actions
     */
    public void clearData() {
        mActions.clear();
        notifyDataSetChanged();
    }

    /**
     * Set click listener for actions
     */
    public void setOnActionClickListener(@NonNull OnActionClickListener<T> listener) {
        mClickListener = listener;
    }

    /**
     * Click listener interface
     */
    public interface OnActionClickListener<T> {
        void onActionClicked(@NonNull SelectionAction<T> action);
    }
}