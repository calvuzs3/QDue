/**
 * Concrete implementation of SelectionContext
 * Immutable context for selection operations
 */
package net.calvuz.qdue.ui.core.components.selection.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable implementation of SelectionContext
 *
 * @param <T> The type of items being selected
 */
public class SelectionContextImpl<T> implements SelectionContext<T> {

    private final Set<T> selectedItems;
    private final Map<String, Object> metadata;
    private final String source;

    private SelectionContextImpl(@NonNull Builder<T> builder) {
        this.selectedItems = Collections.unmodifiableSet(new HashSet<>(builder.items));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        this.source = builder.source;
    }

    @Override
    @NonNull
    public Set<T> getSelectedItems() {
        return selectedItems;
    }

    @Override
    public int getSelectionCount() {
        return selectedItems.size();
    }

    @Override
    public boolean isEmpty() {
        return selectedItems.isEmpty();
    }

    @Override
    @NonNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    @Nullable
    public <V> V getMetadata(@NonNull String key, @NonNull Class<V> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    @Override
    public boolean contains(@NonNull T item) {
        return selectedItems.contains(item);
    }

    @Override
    public boolean hasMetadata(@NonNull String key) {
        return metadata.containsKey(key);
    }

    @Override
    @NonNull
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "SelectionContext{" +
                "itemCount=" + selectedItems.size() +
                ", source='" + source + '\'' +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }

    /**
     * Builder for creating SelectionContext instances
     */
    public static class Builder<T> implements SelectionContext.Builder<T> {
        private final Set<T> items = new HashSet<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private String source = "Unknown";

        @Override
        public Builder<T> addItem(@NonNull T item) {
            items.add(item);
            return this;
        }

        @Override
        public Builder<T> addItems(@NonNull Set<T> items) {
            this.items.addAll(items);
            return this;
        }

        @Override
        public Builder<T> putMetadata(@NonNull String key, @NonNull Object value) {
            metadata.put(key, value);
            return this;
        }

        @Override
        public Builder<T> setSource(@NonNull String source) {
            this.source = source;
            return this;
        }

        @Override
        public SelectionContext<T> build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Cannot create SelectionContext with no items");
            }
            return new SelectionContextImpl<>(this);
        }
    }

    /**
     * Static factory method for builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}