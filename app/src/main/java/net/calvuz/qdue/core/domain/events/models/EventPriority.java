package net.calvuz.qdue.core.domain.events.models;

import static net.calvuz.qdue.ui.core.common.utils.Library.getString;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;

public enum EventPriority {
    LOW(R.string.priority_low, R.color.priority_low),
    NORMAL(R.string.priority_normal, R.color.priority_normal),
    HIGH(R.string.priority_high, R.color.priority_high),
    URGENT(R.string.priority_urgent, R.color.priority_urgent);

    @StringRes
    private final int displayName;

    @ColorRes
    private final int color;

    EventPriority(@StringRes int displayName,@ColorRes int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return getString(QDue.getContext(), displayName); }
    public int getColor() { return color; }
}