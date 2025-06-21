package net.calvuz.qdue.ui.calendar;

import static net.calvuz.qdue.QDue.Debug.DEBUG_ADAPTER;
import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.quattrodue.models.Shift;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.EventIndicatorHelper;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IMPROVED CalendarAdapter - Mantiene la bellezza originale con eventi integrati
 *
 * DESIGN PRINCIPLES:
 * - Rispetta il design originale che funzionava bene
 * - Aggiunge eventi senza compromettere l'estetica
 * - Layout più alto (1/4 in più) per maggiore spazio
 * - Posizionamento preciso degli elementi
 * - Indicatori discreti ma visibili
 */
public class CalendarAdapter extends BaseAdapter {

    private static final String TAG = "CalendarAdapter";

    // ==================== EVENTI INTEGRATION ====================

    // Events data management
    private Map<LocalDate, List<LocalEvent>> mEventsData = new HashMap<>();
    private EventIndicatorHelper mEventHelper;
    private EventsDatabase mEventsDatabase;
    private final AtomicBoolean mIsLoadingEvents = new AtomicBoolean(false);

    // ==================== CONSTRUCTOR ====================

    public CalendarAdapter(Context context, List<SharedViewModels.ViewItem> items,
                           HalfTeam userHalfTeam) {
        super(context, items, userHalfTeam, 1); // Calendar doesn't show detailed shift info

        // Initialize events support
        mEventHelper = new EventIndicatorHelper(context);
        mEventsDatabase = EventsDatabase.getInstance(context);

        Log.d(TAG, "CalendarAdapter initialized with improved layout");
    }

    // ==================== VIEW HOLDER CREATION ====================

    @Override
    protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
        // Use improved calendar layout
        View view = inflater.inflate(R.layout.item_calendar_day_enhanced, parent, false);
        return new ImprovedCalendarDayViewHolder(view);
    }

    // ==================== DAY BINDING ====================

    @Override
    protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
        if (!(holder instanceof ImprovedCalendarDayViewHolder)) {
            Log.e(TAG, "Invalid ViewHolder type");
            return;
        }

        ImprovedCalendarDayViewHolder calendarHolder = (ImprovedCalendarDayViewHolder) holder;

        // STEP 1: Reset visual state
        resetCalendarCellState(calendarHolder);

        // STEP 2: Setup day number (top-left, smaller font)
        setupDayNumber(calendarHolder, dayItem);

        // STEP 3: Setup events indicator (top-right, dot with badge)
        setupEventsIndicator(calendarHolder, dayItem);

        // STEP 4: Setup shift name and indicator (bottom-right area)
        setupShiftDisplay(calendarHolder, dayItem);

        // STEP 5: Apply today/special day styling
        applySpecialDayStyling(calendarHolder, dayItem);

        // STEP 6: Apply Sunday highlighting (NEW - missing from original)
        applySundayHighlighting(calendarHolder, dayItem);

        // STEP 7: Apply background styling for better visual hierarchy
        applyBackgroundStyling(calendarHolder, dayItem);

        if (DEBUG_ADAPTER) {
            Log.v(TAG, "Bound improved calendar day: " + dayItem.day.getLocalDate());
        }
    }

    // ==================== SETUP METHODS ====================

    /**
     * Reset all visual state for consistent appearance
     */
    private void resetCalendarCellState(ImprovedCalendarDayViewHolder holder) {
        // Reset day number
        if (holder.tvDayNumber != null) {
            holder.tvDayNumber.setTextColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorOnSurface));
            holder.tvDayNumber.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        // Reset events indicators
        if (holder.vEventsDot != null) {
            holder.vEventsDot.setVisibility(View.GONE);
        }
        if (holder.tvEventsCount != null) {
            holder.tvEventsCount.setVisibility(View.GONE);
        }

        // Reset shift elements
        if (holder.tvShiftName != null) {
            holder.tvShiftName.setVisibility(View.GONE);
            holder.tvShiftName.setTextColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorOnSurface));
        }
        if (holder.vShiftIndicator != null) {
            holder.vShiftIndicator.setVisibility(View.INVISIBLE);
        }

        // Reset card styling - IMPORTANT: Reset all MaterialCardView properties
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) holder.itemView;

            // Reset to default card appearance
            cardView.setStrokeWidth(1);
            cardView.setCardElevation(1f);
            cardView.setCardBackgroundColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorSurface));
            cardView.setStrokeColor(getColorByThemeAttr(mContext,
                    com.google.android.material.R.attr.colorOutlineVariant));
        }

        holder.itemView.setAlpha(1.0f);
    }

    /**
     * Setup day number in top-left corner with smaller font
     */
    private void setupDayNumber(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (holder.tvDayNumber == null || dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        holder.tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));
    }

    /**
     * Setup events indicator in top-right corner
     * Dot for presence + badge for count if > 1
     */
    private void setupEventsIndicator(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        Log.d(TAG, "setupEventsIndicator called");

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        List<LocalEvent> events = getEventsForDate(date);

        Log.d(TAG, "Date: " + date + ", Events count: " + events.size());

        if (events.isEmpty()) {
            // No events - hide both indicators
            if (holder.vEventsDot != null) {
                holder.vEventsDot.setVisibility(View.GONE);
            }
            if (holder.tvEventsCount != null) {
                holder.tvEventsCount.setVisibility(View.GONE);
            }
        } else {
            int eventCount = events.size();

            // FIX: Get priority color for tinting
            int priorityColor = mEventHelper.getHighestPriorityColor(events);


            Log.d(TAG, "Setting up events indicator for " + eventCount + " events");
            Log.d(TAG, "Priority color: " + Integer.toHexString(priorityColor));

            if (eventCount == 1) {
                Log.d(TAG, "Single event - showing dot");
                // Single event - show colored dot
                if (holder.vEventsDot != null) {
                    holder.vEventsDot.setVisibility(View.VISIBLE);
                    // FIX: Apply color to dot background
                    holder.vEventsDot.setBackgroundColor(priorityColor);
                    Log.d(TAG, "Dot visibility set to VISIBLE");
                } else {
                    Log.e(TAG, "vEventsDot is NULL!");
                }

                if (holder.tvEventsCount != null) {
                    holder.tvEventsCount.setVisibility(View.GONE);
                }
            } else {
                Log.d(TAG, "Multiple events - showing badge: " + eventCount);
                // Multiple events - show count badge with color
                if (holder.vEventsDot != null) {
                    holder.vEventsDot.setVisibility(View.GONE);
                }
                if (holder.tvEventsCount != null) {
                    holder.tvEventsCount.setVisibility(View.VISIBLE);
                    holder.tvEventsCount.setText(eventCount > 9 ? "9+" : String.valueOf(eventCount));

                    // CRITICAL: Apply background tint
                    holder.tvEventsCount.getBackground().setTint(priorityColor);
                    holder.tvEventsCount.setTextColor(getContrastingTextColor(priorityColor));

                    Log.d(TAG, "Badge set: text=" + eventCount + ", visibility=VISIBLE");
                } else {
                    Log.e(TAG, "tvEventsCount is NULL!");
                }
            }
        }
    }

    /**
     * AGGIUNGERE metodo helper per testo contrastante:
     */
    private int getContrastingTextColor(int backgroundColor) {
        // Calculate luminance
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        // Return white for dark backgrounds, black for light
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }



    /**
     * Setup shift name (first letter) and shift indicator
     */
    private void setupShiftDisplay(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) return;

        Day day = dayItem.day;

        // Check if user has a shift on this day
        if (day.getInWichTeamIsHalfTeam(mUserHalfTeam)>=0) {
            // Get user's shift for this day
            Shift userShift = day.getShifts().get(day.getInWichTeamIsHalfTeam(mUserHalfTeam));

            if (userShift != null) {
                // Show shift name (first letter)
                if (holder.tvShiftName != null) {
                    String shiftName = userShift.getShiftType().getShortName();
                    String firstLetter = shiftName.length() > 0 ?
                            shiftName.substring(0, 1).toUpperCase() : "S";
                    holder.tvShiftName.setText(firstLetter);
                    holder.tvShiftName.setVisibility(View.VISIBLE);
                }

                // Show shift indicator bar
                if (holder.vShiftIndicator != null) {
                    holder.vShiftIndicator.setVisibility(View.VISIBLE);
                    int shiftColor = getShiftColor(userShift);
                    holder.vShiftIndicator.setBackgroundColor(shiftColor);
                }
            }
        } else {
            // No shift - hide both elements
            if (holder.tvShiftName != null) {
                holder.tvShiftName.setVisibility(View.GONE);
            }
            if (holder.vShiftIndicator != null) {
                holder.vShiftIndicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Apply special styling for today and other important days
     */
    private void applySpecialDayStyling(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        LocalDate today = LocalDate.now();

        if (date.equals(today)) {
            // Today styling - highlight day number and border
            if (holder.tvDayNumber != null) {
                holder.tvDayNumber.setTextColor(getColorByThemeAttr(mContext,
                        androidx.appcompat.R.attr.colorPrimary));
                holder.tvDayNumber.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }

            // Add border to card
            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
//                ((com.google.android.material.card.MaterialCardView) holder.itemView)
//                        .setStrokeWidth(1);
//                ((com.google.android.material.card.MaterialCardView) holder.itemView)
//                        .setStrokeColor(getColorByThemeAttr(mContext,
//                                androidx.appcompat.R.attr.colorPrimary));
            }
        } else if (date.isBefore(today)) {
            // Past days - slightly faded
            holder.itemView.setAlpha(0.50f);
        }
    }

    /**
     * Apply Sunday highlighting - RED text and special background
     * This restores the original calendar Sunday highlighting behavior
     */
    private void applySundayHighlighting(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();

        // Check if this is Sunday (DayOfWeek.SUNDAY = 7)
        if (date.getDayOfWeek().getValue() == 7) {
            // Apply red text to day number for Sunday
            if (holder.tvDayNumber != null) {
                holder.tvDayNumber.setTextColor(
                        ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
                holder.tvDayNumber.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }

            // Apply red color to shift name if present
            if (holder.tvShiftName != null && holder.tvShiftName.getVisibility() == View.VISIBLE) {
                holder.tvShiftName.setTextColor(
                        ContextCompat.getColor(mContext, android.R.color.holo_red_dark));
            }
        }
    }

    /**
     * Apply background styling for visual hierarchy
     * Provides subtle backgrounds for better calendar readability
     */
    private void applyBackgroundStyling(ImprovedCalendarDayViewHolder holder, SharedViewModels.DayItem dayItem) {
        if (!(holder.itemView instanceof com.google.android.material.card.MaterialCardView)) return;

        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) holder.itemView;

        if (dayItem.day == null) return;

        LocalDate date = dayItem.day.getLocalDate();
        LocalDate today = LocalDate.now();

        // NUOVO: Controllare se ci sono eventi per questa data
        List<LocalEvent> events = getEventsForDate(date);

        if (date.equals(today)) {
            // Today: Special background with elevation
            setupTodayCardStyle(cardView);
        } else if (date.getDayOfWeek().getValue() == 7) { // Sunday
            // Sunday: Light background highlighting
            setupSundayCardStyle(cardView);
        }  else if (!events.isEmpty()) {
            // NUOVO: Giorni con eventi - background colorato
            setupEventsCardStyle(cardView, events);
        } else {
            // Regular days: Standard subtle background
            setupRegularCardStyle(cardView);
        }
    }

    /**
     * Setup card style for today
     */
    private void setupTodayCardStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(2f);
        cardView.setCardBackgroundColor(getColorByThemeAttr(mContext,
                com.google.android.material.R.attr.colorPrimaryContainer));
        cardView.setStrokeColor(getColorByThemeAttr(mContext,
                androidx.appcompat.R.attr.colorPrimary));
    }

    /**
     * Setup card style for Sunday with subtle highlighting
     */
    private void setupSundayCardStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(1f);
        cardView.setCardBackgroundColor(getColorByThemeAttr(mContext,
                com.google.android.material.R.attr.colorSurfaceVariant));
        cardView.setStrokeColor(
                ContextCompat.getColor(mContext, android.R.color.holo_red_light));
    }

    /**
     * Setup card style for EVENTS with subtle highlighting
     */
    private void setupEventsCardStyle(com.google.android.material.card.MaterialCardView cardView, List<LocalEvent> events) {
        // Ottenere colore dominante eventi
        int eventColor = getDominantEventTypeColor(events);

        // Applicare blend con bianco per background leggibile
        int lightBackground = blendEventColorWithWhite(eventColor);

        cardView.setStrokeWidth(1); // FIX: Bordo normale, non spesso
        cardView.setCardElevation(1f);
        cardView.setCardBackgroundColor(lightBackground);
        cardView.setStrokeColor(getColorByThemeAttr(mContext,
                com.google.android.material.R.attr.colorOutlineVariant));
    }

    /**
     * Setup card style for regular days
     */
    private void setupRegularCardStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(1);
        cardView.setCardElevation(1f);
        cardView.setCardBackgroundColor(getColorByThemeAttr(mContext,
                com.google.android.material.R.attr.colorSurface));
        cardView.setStrokeColor(getColorByThemeAttr(mContext,
                com.google.android.material.R.attr.colorOutlineVariant));
    }


    /**
     * AGGIUNGERE questo metodo in CalendarAdapter.java (copiato da DaysListAdapter funzionante):
     */
    private int blendEventColorWithWhite(int eventColor) {
        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        // Blend con bianco (84% bianco, 16% evento) - stessi valori che funzionano in DaysList
        float eventWeight = 0.16f;
        float whiteWeight = 0.84f;

        int blendedRed = (int) (255 * whiteWeight + eventRed * eventWeight);
        int blendedGreen = (int) (255 * whiteWeight + eventGreen * eventWeight);
        int blendedBlue = (int) (255 * whiteWeight + eventBlue * eventWeight);

        return android.graphics.Color.rgb(blendedRed, blendedGreen, blendedBlue);
    }

    /**
     * AGGIUNGERE questo metodo per ottenere colore dominante eventi:
     */
    private int getDominantEventTypeColor(List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            return ContextCompat.getColor(mContext, R.color.event_type_general);
        }

        // Usare EventIndicatorHelper per ottenere colore priorità più alta
        return mEventHelper.getHighestPriorityColor(events);
    }

    // ==================== EVENTS DATA MANAGEMENT ====================

    /**
     * Get events for a specific date - FIXED VERSION WITH DEBUG
     * AGGIUNGERE debug specifico per le date con eventi:
     */
    private List<LocalEvent> getEventsForDate(LocalDate date) {
        Log.v(TAG, "getEventsForDate(" + date + ") called");
        Log.v(TAG, "mEventsData has " + mEventsData.size() + " total dates");

        List<LocalEvent> events = mEventsData.get(date);

        return (events != null) ? events : new ArrayList<>();
    }

    /**
     * Load events from database asynchronously
     */
    public void loadEventsAsync() {
        if (mIsLoadingEvents.getAndSet(true)) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return mEventsDatabase.eventDao().getAllEvents();
            } catch (Exception e) {
                Log.e(TAG, "Error loading events", e);
                return new ArrayList<LocalEvent>();
            }
        }).thenAccept(events -> {
            Map<LocalDate, List<LocalEvent>> eventsMap = new HashMap<>();
            for (LocalEvent event : events) {
                LocalDate eventDate = event.getStartDate();
                eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            }

            if (mContext instanceof android.app.Activity) {
                ((android.app.Activity) mContext).runOnUiThread(() -> {
                    updateEventsData(eventsMap);
                    mIsLoadingEvents.set(false);
                });
            }
        });
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get color for shift indicator based on shift type
     */
    private int getShiftColor(Shift shift) {
        if (shift == null || shift.getShiftType() == null) {
            return getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary);
        }

        // Use the color from ShiftType
        int shiftColor = shift.getShiftType().getColor();

        // If no color set, use theme-based colors by shift name
        if (shiftColor == 0) {
            String shiftName = shift.getShiftType().getName().toLowerCase();

            if (shiftName.contains("mattino") || shiftName.contains("morning") || shiftName.contains("m")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftMorning);
            } else if (shiftName.contains("pomeriggio") || shiftName.contains("afternoon") || shiftName.contains("p")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftAfternoon);
            } else if (shiftName.contains("notte") || shiftName.contains("night") || shiftName.contains("n")) {
                return getColorByThemeAttr(mContext, R.attr.colorShiftNight);
            } else {
                return getColorByThemeAttr(mContext, androidx.appcompat.R.attr.colorPrimary);
            }
        }

        return shiftColor;
    }


    // ==================== IMPROVED VIEW HOLDER ====================

    /**
     * Improved CalendarDayViewHolder with precise element positioning
     */
    public class ImprovedCalendarDayViewHolder extends DayViewHolder {

        // Day number (top-left, smaller)
        public final TextView tvDayNumber;

        // Events indicators (top-right area)
        public final FrameLayout eventsContainer;
        public final View vEventsDot;
        public final TextView tvEventsCount;

        // Shift display (bottom-right area)
        public final TextView tvShiftName;
        public final View vShiftIndicator;

        public ImprovedCalendarDayViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize all elements
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);

            eventsContainer = itemView.findViewById(R.id.events_container);
            vEventsDot = itemView.findViewById(R.id.v_events_dot);
            tvEventsCount = itemView.findViewById(R.id.tv_events_count);

            tvShiftName = itemView.findViewById(R.id.tv_shift_name);
            vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);

            // Set click listener for day interaction
            itemView.setOnClickListener(v -> {
                Log.v(TAG, "Calendar day clicked: " + getAdapterPosition());
                // Handle day click - could show day details, events, etc.
            });

            // DEBUG: Verificare che gli elementi esistano
//            Log.d(TAG, "ViewHolder Debug:");
//            Log.d(TAG, "tvDayNumber: " + (tvDayNumber != null ? "OK" : "NULL"));
//            Log.d(TAG, "eventsContainer: " + (eventsContainer != null ? "OK" : "NULL"));
//            Log.d(TAG, "vEventsDot: " + (vEventsDot != null ? "OK" : "NULL"));
//            Log.d(TAG, "tvEventsCount: " + (tvEventsCount != null ? "OK" : "NULL"));
//            Log.d(TAG, "tvShiftName: " + (tvShiftName != null ? "OK" : "NULL"));
//            Log.d(TAG, "vShiftIndicator: " + (vShiftIndicator != null ? "OK" : "NULL"));
//
//            Log.d(TAG, "ImprovedCalendarDayViewHolder initialized");
        }
    }


    // ========================================
// FIX 4: CalendarAdapter.java - Debug in updateEventsData()
// ========================================

// AGGIUNGERE debug in CalendarAdapter.updateEventsData():

    /**
     * Update events data from external source - FIXED VERSION WITH DEBUG
     * Problema potenziale: mEventsData potrebbe essere sovrascritto da thread diversi
     */
    public void updateEventsData(Map<LocalDate, List<LocalEvent>> eventsMap) {
        // FIX: Assicurarsi che l'aggiornamento avvenga nel main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // Se non siamo nel main thread, fare post al main thread
            new Handler(Looper.getMainLooper()).post(() -> updateEventsData(eventsMap));
            return;
        }

        Log.d(TAG, "updateEventsData on main thread with " +
                (eventsMap != null ? eventsMap.size() : "null") + " entries");

        // Creare copia defensive per evitare modifiche concorrenti
        if (eventsMap != null) {
            this.mEventsData = new HashMap<>();
            for (Map.Entry<LocalDate, List<LocalEvent>> entry : eventsMap.entrySet()) {
                this.mEventsData.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        } else {
            this.mEventsData = new HashMap<>();
        }

        Log.d(TAG, "mEventsData updated with " + mEventsData.size() + " dates");

        // Notificare IMMEDIATAMENTE
        notifyDataSetChanged();
    }

}