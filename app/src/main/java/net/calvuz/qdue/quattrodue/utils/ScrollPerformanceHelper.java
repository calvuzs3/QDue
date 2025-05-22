package net.calvuz.qdue.quattrodue.utils;

import androidx.recyclerview.widget.RecyclerView;
import net.calvuz.qdue.utils.Log;

/**
 * Helper class per ottimizzare le prestazioni dello scroll infinito.
 */
public class ScrollPerformanceHelper {

    private static final String TAG = "ScrollPerformanceHelper";
    private static final boolean LOG_ENABLED = true;

    /**
     * Configura le ottimizzazioni per un RecyclerView con scroll infinito.
     *
     * @param recyclerView RecyclerView da ottimizzare
     */
    public static void optimizeRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) return;

        // Abilita le ottimizzazioni per dataset fissi
        recyclerView.setHasFixedSize(true);

        // Migliora le prestazioni del drawing
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(android.view.View.DRAWING_CACHE_QUALITY_HIGH);

        // Ottimizza il pool di ViewHolder
        RecyclerView.RecycledViewPool pool = recyclerView.getRecycledViewPool();
        pool.setMaxRecycledViews(0, 20); // Mantieni fino a 20 ViewHolder in cache

        // Abilita il nested scrolling se necessario
        recyclerView.setNestedScrollingEnabled(true);

        if (LOG_ENABLED) {
            Log.d(TAG, "RecyclerView ottimizzato per scroll infinito");
        }
    }

    /**
     * Aggiunge un listener per monitorare le prestazioni dello scroll.
     *
     * @param recyclerView RecyclerView da monitorare
     */
    public static void addPerformanceMonitoring(RecyclerView recyclerView) {
        if (recyclerView == null) return;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private long lastScrollTime = 0;
            private int scrollEventCount = 0;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                long currentTime = System.currentTimeMillis();
                scrollEventCount++;

                // Log delle prestazioni ogni 50 eventi di scroll
                if (scrollEventCount % 50 == 0) {
                    long timeDiff = currentTime - lastScrollTime;
                    if (LOG_ENABLED && timeDiff > 0) {
                        Log.d(TAG, "Performance: " + scrollEventCount + " eventi in " +
                                timeDiff + "ms, FPS appross: " + (1000f / timeDiff * 50));
                    }
                    lastScrollTime = currentTime;
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                String stateString = "";
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        stateString = "IDLE";
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        stateString = "DRAGGING";
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        stateString = "SETTLING";
                        break;
                }

                if (LOG_ENABLED) {
                    Log.v(TAG, "Scroll state changed: " + stateString);
                }
            }
        });
    }
}