package net.calvuz.qdue.ui.shared;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.shared.enums.ToolbarAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for toolbar actions in BottomSelectionToolbar
 */
public class ToolbarActionsAdapter extends RecyclerView.Adapter<ToolbarActionsAdapter.ActionViewHolder> {

    private List<ToolbarAction> mActions = new ArrayList<>();
    private OnActionClickListener mListener;

    public interface OnActionClickListener {
        void onActionClick(ToolbarAction action);
    }

    public void setActions(List<ToolbarAction> actions) {
        mActions.clear();
        if (actions != null) {
            mActions.addAll(actions);
        }
        notifyDataSetChanged();
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_toolbar_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        ToolbarAction action = mActions.get(position);
        holder.bind(action, mListener);
    }

    @Override
    public int getItemCount() {
        return mActions.size();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton mButton;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            mButton = itemView.findViewById(R.id.btn_toolbar_action);
        }

        public void bind(ToolbarAction action, OnActionClickListener listener) {
            mButton.setText( ToolbarAction.getEventTypeName(action));
            mButton.setIcon(itemView.getContext().getDrawable(  ToolbarAction.getEventIcon(action)));
            mButton.setContentDescription( ToolbarAction.getEventTypeName(action));

            mButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(action);
                }
            });
        }
    }
}