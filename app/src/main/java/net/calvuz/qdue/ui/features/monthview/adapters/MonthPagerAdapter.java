package net.calvuz.qdue.ui.features.monthview.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.domain.calendar.models.WorkScheduleDay;
import net.calvuz.qdue.domain.calendar.models.LocalEvent;
import net.calvuz.qdue.ui.features.swipecalendar.components.SwipeCalendarStateManager;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MonthPagerAdapter - ViewPager2 adapter for month-by-month navigation.
 *
 * <p>Manages finite range of months (1900-2100) with efficient memory management
 * and data loading. Each page displays a complete month view with integrated
 * events and work schedule data.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><strong>Finite Range</strong>: 2401 months from January 1900 to December 2100</li>
 *   <li><strong>Memory Efficient</strong>: ViewPager2 maintains only ~3 pages in memory</li>
 *   <li><strong>Lazy Loading</strong>: Data loaded on-demand per visible month</li>
 *   <li><strong>Error Handling</strong>: Graceful degradation with retry mechanisms</li>
 * </ul>
 *
 * <h3>Data Loading Strategy:</h3>
 * <ul>
 *   <li>Load data when month becomes visible</li>
 *   <li>Cache loaded data for quick re-access</li>
 *   <li>Background thread data loading to prevent UI blocking</li>
 *   <li>Automatic retry on loading failures</li>
 * </ul>
 *
 * @author QDue Development Team
 * @version 1.0.0
 * @since Database Version 6
 */
public class MonthPagerAdapter extends RecyclerView.Adapter<MonthPagerAdapter.MonthViewHolder>
{

    private static final String TAG = "MonthPagerAdapter";

    // ==================== INTERFACES ====================

    /**
     * Interface for month data loading operations.
     */
    public interface DataLoader
    {
        /**
         * Load events for a specific month.
         *
         * @param month    Target month
         * @param callback Callback for results
         */
        void loadEventsForMonth(@NonNull YearMonth month, @NonNull DataCallback<Map<LocalDate, List<LocalEvent>>> callback);

        /**
         * Load work schedule for a specific month.
         *
         * @param month    Target month
         * @param callback Callback for results
         */
        void loadWorkScheduleForMonth(@NonNull YearMonth month, @NonNull DataCallback<Map<LocalDate, WorkScheduleDay>> callback);
    }

    /**
     * Generic callback for data loading operations.
     */
    public interface DataCallback<T>
    {
        void onSuccess(@NonNull T data);

        void onError(@NonNull Exception error);
    }

    /**
     * Interface for day interaction events.
     */
    public interface OnMonthInteractionListener
    {
        /**
         * Called when user clicks on a day.
         */
        void onDayClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull List<LocalEvent> events);

        /**
         * Called when user long-clicks on a day.
         */
        void onDayLongClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull View view);

        /**
         * Called when month data loading fails.
         */
        void onMonthLoadError(@NonNull YearMonth month, @NonNull Exception error);
    }

    // ==================== DATA CLASSES ====================

    /**
     * Represents loading state for a month.
     */
    private enum LoadingState
    {
        IDLE,
        LOADING_EVENTS,
        LOADING_WORK_SCHEDULE,
        LOADED,
        ERROR
    }

    /**
     * Holds cached data for a month.
     */
    private static class MonthData
    {
        final YearMonth month;
        LoadingState state = LoadingState.IDLE;
        Map<LocalDate, List<LocalEvent>> events = new ConcurrentHashMap<>();
        Map<LocalDate, WorkScheduleDay> workSchedule = new ConcurrentHashMap<>();
        Exception lastError;

        MonthData(@NonNull YearMonth month) {
            this.month = month;
        }

        boolean isLoaded() {
            return state == LoadingState.LOADED;
        }

        boolean isLoading() {
            return state == LoadingState.LOADING_EVENTS || state == LoadingState.LOADING_WORK_SCHEDULE;
        }

        boolean hasError() {
            return state == LoadingState.ERROR;
        }
    }

    // ==================== DEPENDENCIES ====================

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final DataLoader mDataLoader;
    private final Handler mMainHandler;
    private final ExecutorService mBackgroundExecutor;

    // ==================== DATA ====================

    // Cache for month data
    private final Map<YearMonth, MonthData> mMonthDataCache = new ConcurrentHashMap<>();

    // ==================== LISTENERS ====================

    private OnMonthInteractionListener mInteractionListener;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates MonthPagerAdapter with required dependencies.
     *
     * @param context    Context for resource access
     * @param dataLoader Data loading interface
     */
    public MonthPagerAdapter(@NonNull Context context, @NonNull DataLoader dataLoader) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from( context );
        this.mDataLoader = dataLoader;
        this.mMainHandler = new Handler( Looper.getMainLooper() );
        this.mBackgroundExecutor = Executors.newFixedThreadPool( 2 );

        setHasStableIds( true );
        Log.d( TAG, "MonthPagerAdapter created" );
        Log.d( TAG,
               "MonthPagerAdapter created with context: " + context.getClass().getSimpleName() );

        // ✅ ADD: Initialize listener to null explicitly for debugging
        this.mInteractionListener = null;
        Log.d(TAG, "mInteractionListener initialized to null");
    }

    // ==================== ADAPTER IMPLEMENTATION ====================

    @Override
    public int getItemCount() {
        return SwipeCalendarStateManager.getTotalMonths();
    }

    @Override
    public long getItemId(int position) {
        // Use position as stable ID
        return position;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate( R.layout.item_swipe_calendar_month, parent, false );
        return new MonthViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        YearMonth month = SwipeCalendarStateManager.getMonthForPosition( position );
        holder.bind( month );
    }
//
//    @Override
//    public void onViewRecycled(@NonNull MonthViewHolder holder) {
//        super.onViewRecycled( holder );
//        holder.cleanup();
//    }

    // ✅ FIX 4: ADD onViewRecycled() override in main adapter class
    @Override
    public void onViewRecycled(@NonNull MonthViewHolder holder) {
        super.onViewRecycled(holder);
        // Only clear listener when actually recycling, not just cleaning up
        holder.clearListenerForRecycling();
    }

    // ==================== VIEWHOLDER ====================

    /**
     * ViewHolder for individual month pages.
     * Uses item_calendar_month.xml layout.
     */
    public class MonthViewHolder extends RecyclerView.ViewHolder
    {

        // Views from item_calendar_month.xml
        private final RecyclerView monthDaysRecycler;
        private final FrameLayout loadingOverlay;
        private final LinearLayout errorState;
        private final MaterialButton retryButton;

        // Month-specific adapter
        private MonthPagerDayAdapter dayAdapter;
        private YearMonth currentMonth;

        public MonthViewHolder(@NonNull View itemView) {
            super( itemView );

            // Find views
            monthDaysRecycler = itemView.findViewById( R.id.recycler_month_days );
            loadingOverlay = itemView.findViewById( R.id.loading_overlay );
            errorState = itemView.findViewById( R.id.error_state );
            retryButton = itemView.findViewById( R.id.btn_retry );

            // Configure RecyclerView
            setupRecyclerView();

            // Set retry button listener
            if (retryButton != null) {
                retryButton.setOnClickListener( v -> retryLoadData() );
            }
        }

        /**
         * Bind month data to this holder.
         */
        public void bind(@NonNull YearMonth month) {
            this.currentMonth = month;

            // Create or update day adapter
            if (dayAdapter == null) {
                dayAdapter = new MonthPagerDayAdapter( mContext, month );
                monthDaysRecycler.setAdapter( dayAdapter );


            } else {
                dayAdapter.updateMonth( month );
            }

            // ✅ ALWAYS set the listener - even for reused adapters
            setupDayClickListener();

            // Load data for this month
            loadDataForMonth( month );

            Log.v( TAG, "Bound ViewHolder to month: " + month );
        }


        // ✅ FIX 2: ADD NEW setupDayClickListener() method
        private void setupDayClickListener() {
            if (dayAdapter != null) {
                dayAdapter.setOnDayClickListener(new MonthPagerDayAdapter.OnDayClickListener() {
                    @Override
                    public void onDayClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull List<LocalEvent> dayEvents) {
                        Log.d(TAG, "Day clicked in MonthPagerAdapter: " + date);
                        if (mInteractionListener != null) {
                            mInteractionListener.onDayClick(date, day, dayEvents);
                            Log.d(TAG, "Day click forwarded to fragment: " + date);
                        } else {
                            Log.w(TAG, "mInteractionListener is null, cannot forward day click: " + date);
                        }
                    }

                    @Override
                    public void onDayLongClick(@NonNull LocalDate date, @Nullable WorkScheduleDay day, @NonNull View view) {
                        Log.d(TAG, "Day long clicked in MonthPagerAdapter: " + date);
                        if (mInteractionListener != null) {
                            mInteractionListener.onDayLongClick(date, day, view);
                            Log.d(TAG, "Day long click forwarded to fragment: " + date);
                        } else {
                            Log.w(TAG, "mInteractionListener is null, cannot forward day long click: " + date);
                        }
                    }
                });

                Log.d(TAG, "Day click listener set on MonthPagerDayAdapter");
            } else {
                Log.e(TAG, "Cannot set day click listener - dayAdapter is null");
            }
        }

        /**
         * Setup RecyclerView configuration.
         */
        private void setupRecyclerView() {
            GridLayoutManager layoutManager = new GridLayoutManager( mContext,
                                                                     7 ); // 7 days per week
            monthDaysRecycler.setLayoutManager( layoutManager );
            monthDaysRecycler.setHasFixedSize( true );
            monthDaysRecycler.setNestedScrollingEnabled( false );
            monthDaysRecycler.setOverScrollMode( View.OVER_SCROLL_NEVER );
        }

        /**
         * Load data for the specified month.
         */
        private void loadDataForMonth(@NonNull YearMonth month) {
            Log.i( TAG, "Loading data for month: " + month );

            MonthData monthData = mMonthDataCache.get( month );

            if (monthData == null) {
                monthData = new MonthData( month );
                mMonthDataCache.put( month, monthData );
            }

            if (monthData.isLoaded()) {
                // Data already loaded - update UI
                updateAdapterWithData( monthData );
                Log.i( TAG, "(loaded) Created new month data for month: " + monthData );

                showLoadedState();
            } else if (monthData.isLoading()) {
                // Currently loading - show loading state
                Log.i( TAG, "(loading) Loading data for month: " + month );
                showLoadingState();
            } else if (monthData.hasError()) {
                // Previous error - show error state
                Log.i( TAG, "(error) Error loading data for month: " + month );
                showErrorState();
            } else {
                // Need to load data
                Log.i( TAG, "(idle) Need to load data for month: " + month );

                // Load Events
                loadLocalEventsData( monthData );

                // Now load Work Schedule
                loadWorkScheduleData( monthData );
            }
        }

        /**
         * Start loading data for a month.
         */
        private void loadLocalEventsData(@NonNull MonthData monthData) {
            showLoadingState();
            monthData.state = LoadingState.LOADING_EVENTS;

            // Load events first
            mDataLoader.loadEventsForMonth( monthData.month, new DataCallback<>()
            {
                @Override
                public void onSuccess(@NonNull Map<LocalDate, List<LocalEvent>> eventsData) {
                    mMainHandler.post( () -> {
                        monthData.events.clear();
                        monthData.events.putAll( eventsData );
                        monthData.state = LoadingState.LOADING_WORK_SCHEDULE;
                    } );
                }

                @Override
                public void onError(@NonNull Exception error) {
                    mMainHandler.post( () -> {
                        monthData.state = LoadingState.ERROR;
                        monthData.lastError = error;
                        showErrorState();

                        if (mInteractionListener != null) {
                            mInteractionListener.onMonthLoadError( monthData.month, error );
                        }
                    } );
                }
            } );
        }

        /**
         * Load work schedule data (second phase of loading).
         */
        private void loadWorkScheduleData(@NonNull MonthData monthData) {
            mDataLoader.loadWorkScheduleForMonth( monthData.month, new DataCallback<>()
            {
                @Override
                public void onSuccess(@NonNull Map<LocalDate, WorkScheduleDay> workScheduleData) {
                    mMainHandler.post( () -> {
                        monthData.workSchedule.clear();
                        monthData.workSchedule.putAll( workScheduleData );
                        monthData.state = LoadingState.LOADED;

                        // Update UI with complete data
                        updateAdapterWithData( monthData );
                        showLoadedState();
                    } );
                }

                @Override
                public void onError(@NonNull Exception error) {
                    mMainHandler.post( () -> {
                        monthData.state = LoadingState.ERROR;
                        monthData.lastError = error;
                        showErrorState();

                        if (mInteractionListener != null) {
                            mInteractionListener.onMonthLoadError( monthData.month, error );
                        }
                    } );
                }
            } );
        }

        /**
         * Update day adapter with loaded data.
         */
        private void updateAdapterWithData(@NonNull MonthData monthData) {
            if (dayAdapter != null) {
                dayAdapter.updateEvents( monthData.events );
                dayAdapter.updateWorkSchedule( monthData.workSchedule );
            }
        }

        /**
         * Show loading state UI.
         */
        private void showLoadingState() {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility( View.VISIBLE );
            }
            if (errorState != null) {
                errorState.setVisibility( View.GONE );
            }
        }

        /**
         * Show loaded state UI.
         */
        private void showLoadedState() {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility( View.GONE );
            }
            if (errorState != null) {
                errorState.setVisibility( View.GONE );
            }
        }

        /**
         * Show error state UI.
         */
        private void showErrorState() {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility( View.GONE );
            }
            if (errorState != null) {
                errorState.setVisibility( View.VISIBLE );
            }
        }

        /**
         * Retry loading data for current month.
         */
        private void retryLoadData() {
            if (currentMonth != null) {
                MonthData monthData = mMonthDataCache.get( currentMonth );
                if (monthData != null) {
                    monthData.state = LoadingState.IDLE;
                    loadDataForMonth( currentMonth );
                }
            }
        }

        /**
         * Cleanup resources when ViewHolder is recycled.
         */
        public void cleanup() {
//            // Clear references to prevent memory leaks
//            if (dayAdapter != null) {
//                dayAdapter.setOnDayClickListener( null );
//            }
            // Don't clear listener immediately - let it be garbage collected naturally
            // This prevents issues when ViewHolder is reused
            Log.d(TAG, "MonthViewHolder cleanup called");
        }

        // ✅ FIX 5: ADD new method in MonthViewHolder for actual recycling
        public void clearListenerForRecycling() {
            if (dayAdapter != null) {
                dayAdapter.setOnDayClickListener(null);
                Log.d(TAG, "Cleared listener for ViewHolder recycling");
            }
        }

    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Set interaction listener for day events.
     *
     * @param listener Interaction listener
     */
    public void setOnMonthInteractionListener(@Nullable OnMonthInteractionListener listener) {
        this.mInteractionListener = listener;

        Log.d(TAG, "OnMonthInteractionListener set: " + (listener != null ? "YES" : "NULL"));

        // If we have ViewHolders already created, update their listeners
        notifyDataSetChanged(); // This will cause bind() to be called again, setting up listeners
    }

    // ✅ FIX 8: ADD method to verify listener chain for debugging
    public void debugListenerChain() {
        Log.d(TAG, "=== LISTENER CHAIN DEBUG ===");
        Log.d(TAG, "mInteractionListener: " + (mInteractionListener != null ? "SET" : "NULL"));
        Log.d(TAG, "Total ViewHolders: ViewPager manages internally");
        Log.d(TAG, "==============================");
    }

    /**
     * Force refresh data for a specific month.
     *
     * @param month Month to refresh
     */
    public void refreshMonth(@NonNull YearMonth month) {
        MonthData monthData = mMonthDataCache.get( month );
        if (monthData != null) {
            monthData.state = LoadingState.IDLE;
            monthData.events.clear();
            monthData.workSchedule.clear();
            monthData.lastError = null;

            // Notify adapter to rebind affected ViewHolder
            int position = SwipeCalendarStateManager.getPositionForMonth( month );
            notifyItemChanged( position );
        }
    }

    /**
     * Clear cached data to free memory.
     * Call this when adapter is no longer needed.
     */
    public void clearCache() {
        mMonthDataCache.clear();
        Log.d( TAG, "Month data cache cleared" );
    }

    /**
     * Cleanup adapter resources.
     * Call this when adapter is destroyed.
     */
    public void cleanup() {
        clearCache();
        mBackgroundExecutor.shutdown();
        mInteractionListener = null;

        Log.d( TAG, "MonthPagerAdapter cleaned up" );
    }

    // ✅ FIX 6: ADD debugging method to check listener state
    public boolean isListenerSet() {
        return mInteractionListener != null;
    }

}