
package net.calvuz.qdue.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * Fragment per la visualizzazione calendario dei turni dell'utente.
 * Utilizza l'adapter base unificato specializzato per la vista calendario.
 */
public class CalendarViewFragment extends BaseFragment {

    private static final String TAG = "CalendarViewFragment";
    private static final boolean LOG_ENABLED = true;

    private CalendarAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    protected void findViews(View rootView) {
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
    }

    @Override
    protected void setupRecyclerView() {
        if (mRecyclerView == null) {
            if (LOG_ENABLED) Log.e(TAG, "mRecyclerView è null");
            return;
        }

        // Per il calendario, usa GridLayoutManager con 7 colonne
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 7);
        mRecyclerView.setLayoutManager(gridLayoutManager);

        // Il LinearLayoutManager della classe base non serve
        mLayoutManager = null;

        // Ottimizzazioni
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // Per il calendario, converte includendo celle vuote per completare la griglia
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        if (LOG_ENABLED) Log.d(TAG, "setupAdapter");

        mAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                mQD.getUserHalfTeam()
        );
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected RecyclerView.Adapter<?> getAdapter() {
        return mAdapter;
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            // Per il calendario in griglia, calcola se oggi è visibile
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

    @Override
    protected void onUserTeamChanged() {
        super.onUserTeamChanged();
        if (mAdapter != null) {
            mAdapter.updateUserTeam(mQD.getUserHalfTeam());
        }
    }

    /**
     * Override dei metodi di scroll per gestire GridLayoutManager.
     */
    @Override
    protected void setupInfiniteScrolling() {
        // La classe base usa LinearLayoutManager, ma per il calendario usiamo GridLayoutManager
        // Reimplementiamo solo la parte di scroll detection
        super.setupInfiniteScrolling();

        // Sostituisci il listener per gestire GridLayoutManager
        if (mRecyclerView != null) {
            mRecyclerView.clearOnScrollListeners();
            mRecyclerView.addOnScrollListener(new CalendarScrollListener());
        }
    }

    /**
     * Listener personalizzato per il GridLayoutManager del calendario.
     */
    private class CalendarScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - mLastScrollTime < 100) return; // Throttling
            mLastScrollTime = currentTime;

            GridLayoutManager gridManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            if (gridManager == null) return;

            int firstVisible = gridManager.findFirstVisibleItemPosition();
            int lastVisible = gridManager.findLastVisibleItemPosition();

            if (firstVisible == RecyclerView.NO_POSITION) return;

            // Gestisci caricamento basato su scroll per griglia
            handleGridScrollBasedLoading(firstVisible, lastVisible, dy);

            // Aggiorna visibilità FAB
            updateFabVisibility(firstVisible, lastVisible);
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                mScrollVelocity = 0;
                mMainHandler.postDelayed(() -> {
                    if (mScrollVelocity == 0) {
                        processPendingOperations();
                        scheduleCleanupIfNeeded();
                    }
                }, SCROLL_SETTLE_DELAY);
            }
        }
    }

    /**
     * Gestisce il caricamento per la griglia calendario.
     */
    private void handleGridScrollBasedLoading(int firstVisible, int lastVisible, int scrollDirection) {
        // Per la griglia 7x6, calcola quante "righe" siamo dall'inizio/fine
        int firstRow = firstVisible / 7;
        int lastRow = lastVisible / 7;
        int totalRows = (mItemsCache.size() + 6) / 7; // Arrotonda per eccesso

        // Caricamento verso l'alto (primi 2 righe visibili)
        if (firstRow <= 2 && scrollDirection <= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingTopLoad.get() && !mShowingTopLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering top load at row: " + firstRow);
            triggerTopLoad();
        }

        // Caricamento verso il basso (ultime 2 righe visibili)
        if (lastRow >= totalRows - 3 && scrollDirection >= 0 &&
                !mIsUpdatingCache.get() && !mIsPendingBottomLoad.get() && !mShowingBottomLoader) {

            if (LOG_ENABLED) Log.d(TAG, "Triggering bottom load at row: " + lastRow);
            triggerBottomLoad();
        }
    }

    /**
     * Metodi helper per accedere ai metodi protetti della classe base.
     */
    private void triggerTopLoad() {
        mIsPendingTopLoad.set(true);
        // Chiama il metodo protetto della classe base tramite reflection o esponi come protected
        mBackgroundHandler.postDelayed(() -> executeTopLoad(), 100);
    }

    private void triggerBottomLoad() {
        mIsPendingBottomLoad.set(true);
        mBackgroundHandler.postDelayed(() -> executeBottomLoad(), 100);
    }

    // Questi metodi dovrebbero essere esposti come protected nella classe base
    private void executeTopLoad() {
        // Implementazione semplificata - la classe base dovrebbe esporre questo metodo
        if (LOG_ENABLED) Log.d(TAG, "executeTopLoad chiamato");
    }

    private void executeBottomLoad() {
        // Implementazione semplificata - la classe base dovrebbe esporre questo metodo
        if (LOG_ENABLED) Log.d(TAG, "executeBottomLoad chiamato");
    }

    private void processPendingOperations() {
        // La classe base dovrebbe esporre questo metodo
    }

    private void scheduleCleanupIfNeeded() {
        // La classe base dovrebbe esporre questo metodo
    }
}