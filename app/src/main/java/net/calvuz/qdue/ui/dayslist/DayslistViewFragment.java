package net.calvuz.qdue.ui.dayslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.calvuz.qdue.QDue;
import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.BaseFragment;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

/**
 * Fragment per la visualizzazione lista dei turni con scrolling infinito ottimizzato.
 * Utilizza la stessa logica migliorata del CalendarViewFragment.
 */
public class DayslistViewFragment extends BaseFragment {

    // TAG
    private final String TAG = "DayslistViewFragment";

    // Dayslist adapter
    SimpleEnhancedDaysListAdapter mAdapter;
//    private EnhancedDaysListAdapter mAdapter;
//    private DaysListAdapter mAdapter;
//    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        return root;
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        return mAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        this.mAdapter = (SimpleEnhancedDaysListAdapter) adapter;
    }

    @Override
    protected void findViews(View view) {
        mRecyclerView = view.findViewById(R.id.rv_dayslist);
        mFabGoToToday = view.findViewById(R.id.fab_go_to_today);
    }

    /**
     * Return 1 column for list-like appearance using GridLayoutManager.
     * This unifies the implementation with CalendarViewFragment while maintaining list behavior.
     */
    @Override
    protected int getGridColumnCount() {
        return 1; // Single column for list-like behavior
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return SharedViewModels.DataConverter.convertForDaysList(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        Log.v(TAG, "setupAdapter: called.");

        mAdapter = new SimpleEnhancedDaysListAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam(), 3
        );

        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        Log.v(TAG, "updateFabVisibility: called.");
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            showFab = !(firstVisible <= mTodayPosition && lastVisible >= mTodayPosition);
        }

        if (showFab && mFabGoToToday.getVisibility() != View.VISIBLE) {
            mFabGoToToday.show();
        } else if (!showFab && mFabGoToToday.getVisibility() == View.VISIBLE) {
            mFabGoToToday.hide();
        }
    }

}