package net.calvuz.qdue.ui.dayslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.Day;
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
    private final String TAG = DayslistViewFragment.class.getSimpleName();

    // Dayslist adapter
    private DaysListAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        return root;
    }

    @Override
    protected void findViews(View rootView) {
        mFabGoToToday = rootView.findViewById(R.id.fab_go_to_today);
        mRecyclerView = rootView.findViewById(R.id.rv_dayslist);
    }

    @Override
    protected List<SharedViewModels.ViewItem> convertMonthData(List<Day> days, LocalDate monthDate) {
        return SharedViewModels.DataConverter.convertForDaysList(days, monthDate);
    }

    @Override
    protected RecyclerView.Adapter<?> getAdapter() {
        return mAdapter;
    }

    @Override
    protected void setupAdapter() {
        Log.d(TAG, "setupAdapter()");

        mAdapter = new DaysListAdapter(
                getContext(),
                mItemsCache,
                mQD.getUserHalfTeam(),
                3 // numero turni
        );
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void updateFabVisibility(int firstVisible, int lastVisible) {
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

    // Metodo per aggiornare quando cambiano le preferenze
    // TODO remove method
    public void notifyUpdates() {
        if (mAdapter != null) {
            mAdapter.updateUserTeam(mQD.getUserHalfTeam());
        }
    }

    @Override
    protected void onUserTeamChanged() {
        super.onUserTeamChanged();
        if (mAdapter != null) {
            mAdapter.updateUserTeam(mQD.getUserHalfTeam());
        }
    }
}