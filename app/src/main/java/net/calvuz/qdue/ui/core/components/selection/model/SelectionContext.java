/**
 * Generic selection context for toolbar actions
 * Provides information about the current selection
 */
package net.calvuz.qdue.ui.core.components.selection.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Context for a selection, containing selected items and metadata
 *
 * @param <T> The type of items being selected
 */
public interface SelectionContext<T> {

    /**
     * Get all selected items
     */
    @NonNull
    Set<T> getSelectedItems();

    /**
     * Get the count of selected items
     */
    int getSelectionCount();

    /**
     * Check if selection is empty
     */
    boolean isEmpty();

    /**
     * Get metadata about the selection
     * Can include user info, permissions, etc.
     */
    @NonNull
    Map<String, Object> getMetadata();

    /**
     * Get a specific metadata value
     */
    @Nullable
    <V> V getMetadata(@NonNull String key, @NonNull Class<V> type);

    /**
     * Get the selection source (e.g., fragment/activity name)
     */
    @NonNull
    String getSource();

    /**
     * Check if context contains a specific item
     */
    boolean contains(@NonNull T item);

    /**
     * Check if metadata contains a key
     */
    boolean hasMetadata(@NonNull String key);

    /**
     * Builder for creating selection contexts
     */
    interface Builder<T> {
        Builder<T> addItem(@NonNull T item);
        Builder<T> addItems(@NonNull Set<T> items);
        Builder<T> putMetadata(@NonNull String key, @NonNull Object value);
        Builder<T> setSource(@NonNull String source);
        SelectionContext<T> build();
    }
}