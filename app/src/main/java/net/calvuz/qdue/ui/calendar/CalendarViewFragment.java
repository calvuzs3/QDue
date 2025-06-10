package net.calvuz.qdue.ui.calendar;

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
 * Fragment per la visualizzazione calendario dei turni dell'utente.
 * Utilizza l'adapter base unificato specializzato per la vista calendario.
 */
public class CalendarViewFragment extends BaseFragment {

    // TAG
    private static final String TAG = "CalendarViewFragment";

    // Members
    private CalendarAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    protected BaseAdapter getFragmentAdapter() {
        return mAdapter;
    }

    @Override
    protected void setFragmentAdapter(BaseAdapter adapter) {
        this.mAdapter = (CalendarAdapter) adapter;
    }

    @Override
    protected void findViews(View rootView) {

        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
    }

    /**
     * Return 7 columns for calendar grid layout.
     * This creates the traditional week-based calendar view.
     */
    @Override
    protected int getGridColumnCount() {
        return 7; // Seven columns for calendar days (Sun-Sat)
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        // For calendar, convert including empty cells to complete the grid
        return SharedViewModels.DataConverter.convertForCalendar(days, monthDate);
    }

    @Override
    protected void setupAdapter() {
        Log.v(TAG, "setupAdapter: called.");

        mAdapter = new CalendarAdapter(
                getContext(),
                mItemsCache,
                QDue.getQuattrodue().getUserHalfTeam()
        );
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
        if (mFabGoToToday == null) return;

        boolean showFab = true;
        if (mTodayPosition >= 0) {
            // For grid calendar, calculate if today is visible
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
            mAdapter.updateUserTeam(QDue.getQuattrodue().getUserHalfTeam());
        }
    }

}