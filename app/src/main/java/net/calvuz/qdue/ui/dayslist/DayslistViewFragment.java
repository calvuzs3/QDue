package net.calvuz.qdue.ui.dayslist;

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
 * Fragment per la visualizzazione lista dei turni con scrolling infinito ottimizzato.
 * Utilizza la stessa logica migliorata del CalendarViewFragment.
 */
public class DayslistViewFragment extends BaseFragment {

    // TAG
    private final String TAG = DayslistViewFragment.class.getSimpleName();

    // Dayslist adapter
    private DaysListAdapter mAdapter;
    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar - base class doesn't
        setupToolbar(view);
    }

    @Override
    protected void findViews(View view) {

        mRecyclerView = view.findViewById(R.id.rv_dayslist);
        mToolbar = view.findViewById(R.id.toolbar);
        mFabGoToToday = view.findViewById(R.id.fab_go_to_today);
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

    /**
     * FIXED: Setup toolbar with proper orientation handling.
     * Just in case of need, because it has been removed from land/layouts
     */
    private void setupToolbar(View root) {
        final String mTAG = "setupToolbar: ";

        if (mToolbar == null) {
            Log.e(TAG, mTAG + "Toolbar not found in fragment layout");
            return;
        }

        try {
            // Set toolbar as ActionBar for this fragment's activity
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity =
                        (androidx.appcompat.app.AppCompatActivity) getActivity();
                activity.setSupportActionBar(mToolbar);

                // Configure ActionBar
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
                }
            }

            // CRITICAL: Set menu item click listener
            mToolbar.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();
                Log.v(TAG, mTAG + "onMenuItemClickListener() -> ("
                        + id + ") \n"
                        + item.getTitle());
                try {
                    if (item.getTitle() == (String) getResources().getString(R.string.go_to_today))
                        Log.v(TAG, mTAG + "stringhe coincidenti");
                    scrollToToday();
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "Error: " + e.getMessage());
                }

                try {
                    if (id == R.id.action_about) {
                        navigateTo(R.id.nav_about);
                        return true;
                    }
                    if (id == R.id.action_settings) {
                        navigateTo(R.id.nav_settings);
                        return true;
                    }
                    if (id == R.id.fab_go_to_today) {
                        Log.e(TAG, mTAG + "FAB found as a menu item in setuptoolbar");
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, mTAG + "onMenuItemClick failed: " + e.getMessage());
                }

                Log.v(TAG, mTAG + "onMenuItemClickListener() ->" +
                        " got (" + id + ")" + " expected (" + R.id.fab_go_to_today + ") \n");
                return true;
            });

            // Enable options menu for this fragment
            setHasOptionsMenu(true);

            Log.d(TAG, mTAG + "Fragment toolbar setup complete");

        } catch (Exception e) {
            Log.e(TAG, mTAG + "Error setting up fragment toolbar: " + e.getMessage());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuInflater menuiflater = getActivity().getMenuInflater();
        menuiflater.inflate(R.menu.toolbar_menu, menu);
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//
//        if (item.getItemId() == R.id.action_about) {
//            navigateTo(R.id.nav_about);
//            return true;
//        }
//        if (item.getItemId() == R.id.action_settings) {
//            navigateTo(R.id.nav_settings);
//            return true;
//        }
//        if (item.getItemId() == R.id.fab_go_to_today) {
//            Log.e(TAG, "FAB found as a menu item");
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
