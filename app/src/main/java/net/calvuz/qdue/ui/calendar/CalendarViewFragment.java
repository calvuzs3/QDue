package net.calvuz.qdue.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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

    // TAG
    private static final String TAG = "CalendarViewFragment";

    // Members
    private CalendarAdapter mAdapter;
//    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar - base class doesn't handle this
//        setupToolbar(view);
    }

    @Override
    protected void findViews(View rootView) {

        mRecyclerView = rootView.findViewById(R.id.rv_calendar);
//        mToolbar = rootView.findViewById(R.id.toolbar);
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
            mAdapter.updateUserTeam(mQD.getUserHalfTeam());
        }
    }

    /**
     * ENHANCED: Override scroll to today for grid-specific behavior
     */
    @Override
    public void scrollToToday() {
        final String mTAG = "scrollToToday: ";

        // First, try to find today in current cache
        mTodayPosition = SharedViewModels.DataConverter.findTodayPosition(mItemsCache);

        if (mTodayPosition >= 0 && mTodayPosition < mItemsCache.size()) {
            // Today is in cache - scroll to it with grid-specific positioning
            Log.v(TAG, mTAG + "Today found in cache at position: " + mTodayPosition);

            if (mGridLayoutManager != null) {
                // For grid, use smooth scroll with offset for better positioning
                mRecyclerView.post(() -> {
                    try {
                        // First scroll near the position
                        int targetPosition = Math.max(0, mTodayPosition - 14); // ~2 weeks before
                        mGridLayoutManager.scrollToPosition(targetPosition);

                        // Then smooth scroll to exact position
                        mRecyclerView.postDelayed(() -> {
                            mRecyclerView.smoothScrollToPosition(mTodayPosition);
                        }, 100);
                    } catch (Exception e) {
                        Log.e(TAG, mTAG + "Error in calendar scroll to today: " + e.getMessage());
                        mRecyclerView.smoothScrollToPosition(mTodayPosition);
                    }
                });
            } else {
                mRecyclerView.smoothScrollToPosition(mTodayPosition);
            }
            return;
        }

        // Today is not in cache - use base class smart loading
        Log.v(TAG, mTAG + "Today is not in cache - use base class smart loading");
        super.scrollToToday();
    }

//    /**
//     * FIXED: Setup toolbar
//     * it has been removed from land/layouts
//     */
//    private void setupToolbar(View root) {
//        final String mTAG = "setupToolbar: ";
//
//        if (mToolbar == null) {
//            Log.i(TAG, mTAG + "Toolbar not found in fragment layout");
//            return;
//        }
//
//        try {
//            // Set toolbar as ActionBar for this fragment's activity
//            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
//                androidx.appcompat.app.AppCompatActivity activity =
//                        (androidx.appcompat.app.AppCompatActivity) getActivity();
//                activity.setSupportActionBar(mToolbar);
//
//                // Configure ActionBar
//                if (activity.getSupportActionBar() != null) {
//                    activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
//                    activity.getSupportActionBar().setTitle(getString(R.string.app_name));
//                }
//            }
//
//            // CRITICAL: Set menu item click listener
//            mToolbar.setOnMenuItemClickListener(item -> {
//                String sTAG = "setOnMenuItemClickListener: ";
//
//                int id = item.getItemId();
//                Log.v(TAG, mTAG + "onMenuItemClickListener() -> ("
//                        + id + ") \n"
//                        + item.getTitle());
//                try {
//                    if (item.getTitle() == (String) getResources().getString(R.string.go_to_today)) {
//                        Log.v(TAG, mTAG + sTAG + "found by string value");
//                        scrollToToday();
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, mTAG + sTAG + "Error: " + e.getMessage());
//                }
//
//                try {
//                    if (id == R.id.action_about) {
//                        navigateTo(R.id.nav_about);
//                        return true;
//                    }
//                    if (id == R.id.action_settings) {
//                        navigateTo(R.id.nav_settings);
//                        return true;
//                    }
//                    if (id == R.id.fab_go_to_today) {
//                        Log.e(TAG, mTAG + sTAG + "FAB found as a menu item in setuptoolbar");
//                        return true;
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, mTAG + sTAG + "Error: " + e.getMessage());
//                }
//
//                Log.v(TAG, mTAG + sTAG + "() -> " +
//                        "got (" + id + ")" + " expected (" + R.id.fab_go_to_today + ") \n");
//                return true;
//            });
//
//            // Enable options menu for this fragment
//            //TODO: update to MenuProvider interface
//            setHasOptionsMenu(true);
//
//            Log.d(TAG, mTAG + "Fragment toolbar setup complete");
//
//        } catch (Exception e) {
//            Log.e(TAG, mTAG + "Error setting up fragment toolbar: " + e.getMessage());
//        }
//    }

//    //TODO: update to MenuProvider interface
//    @Override
//    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        MenuInflater menuiflater = getActivity().getMenuInflater();
//        menuiflater.inflate(R.menu.toolbar_menu, menu);
//    }
}