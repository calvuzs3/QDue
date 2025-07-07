package net.calvuz.qdue.ui.shared.utils;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.content.Context;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;
import java.util.List;

public final class HighlightingHelper {

    /*
    PRIORITÀ DEFINITA (dal più importante al meno importante):
    1. TODAY (massima priorità)
    2. EVENTS (media priorità)
    3. SUNDAY (bassa priorità)
    4. OLD_DAYS (overlay finale)
    5. REGULAR (default)
    */


    private static final String TAG = "HIGHLIGHTING";

    private static final int NORMAL_STROKE_WIDTH = 2;
    private static final int TODAY_STROKE_WIDTH = 0;

    private static final float NORMAL_ELEVATION = 0f;
    private static final float TODAY_ELEVATION = 4f;
    private static final float SUNDAY_ELEVATION = 2f;
    private static final float EVENTS_ELEVATION = 2f;

    private static final float OLD_DAYS_ALPHA = 0.75f;

    /**
     * ✅ NUOVO: Metodo unificato per applicare tutti gli highlighting
     * Gestisce automaticamente le priorità e evita conflitti
     */
    public static void applyUnifiedHighlighting(Context context,
                                                com.google.android.material.card.MaterialCardView cardView,
                                                LocalDate date,
                                                List<LocalEvent> events,
                                                EventIndicatorHelper eventHelper) {

        LocalDate today = LocalDate.now();
        boolean isToday = date.equals(today);
        boolean isSunday = date.getDayOfWeek().getValue() == 7;
        boolean hasEvents = events != null && !events.isEmpty();
        boolean isOldDay = date.isBefore(today);

        Log.d(TAG, String.format("Applying unified highlighting for %s: today=%s, sunday=%s, events=%s, old=%s",
                date, isToday, isSunday, hasEvents, isOldDay));

        // ✅ STEP 1: Reset to regular style (baseline)
        setupRegularCardStyle(context, cardView);

        // ✅ STEP 2: Apply styles in PRIORITY ORDER (lower priority first)

        // Priority 3: Sunday (if not today and not events)
        if (isSunday && !isToday && !hasEvents) {
            setupSundayCardStyle(context, cardView);
            Log.d(TAG, "Applied SUNDAY style");
        }

        // Priority 2: Events (if not today)
        else if (hasEvents && !isToday) {
            setupEventsCardStyle(context, eventHelper, cardView, events);
            Log.d(TAG, "Applied EVENTS style");
        }

        // Priority 1: Today (ALWAYS wins)
        if (isToday) {
            setupTodayCardStyle(context, cardView);
            Log.d(TAG, "Applied TODAY style (highest priority)");
        }

        // ✅ STEP 3: Apply old days overlay (if applicable)
        if (isOldDay) {
            applyOldDaysOverlay(cardView);
            Log.d(TAG, "Applied OLD_DAYS overlay");
        }
    }

    /**
     * ✅ NUOVO: Metodo per applicare text highlighting unificato
     * Gestisce colori del testo per domenica e today
     */
    public static void applyUnifiedTextHighlighting(Context context,
                                                    LocalDate date,
                                                    TextView... textViews) {

        LocalDate today = LocalDate.now();
        boolean isToday = date.equals(today);
        boolean isSunday = date.getDayOfWeek().getValue() == 7;

        for (TextView textView : textViews) {
            if (textView == null) continue;

            // ✅ Priority 1: Today text color
            if (isToday) {
                textView.setTextColor(getColorByThemeAttr(context,
                        androidx.appcompat.R.attr.colorPrimary));
                textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
                Log.v(TAG, "Applied TODAY text style");
            }
            // ✅ Priority 2: Sunday text color (only if not today)
            else if (isSunday) {
                textView.setTextColor(ContextCompat.getColor(context,
                        android.R.color.holo_red_dark));
                textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
                Log.v(TAG, "Applied SUNDAY text style");
            }
            // ✅ Default: Regular text
            else {
                textView.setTextColor(getColorByThemeAttr(context,
                        com.google.android.material.R.attr.colorOnSurface));
                textView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL);
            }
        }
    }

    /**
     * Setup card style for regular days
     */
    public static void setupRegularCardStyle(Context context, com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH);
        cardView.setCardElevation(NORMAL_ELEVATION);
        cardView.setCardBackgroundColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorSurface));
//        cardView.setStrokeColor(getColorByThemeAttr(context,
//                androidx.appcompat.R.attr.colorPrimary));
        cardView.setStrokeColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorOutlineVariant));

        Log.v(TAG, "Regular card style applied");
    }

    /**
     * Setup card style for today
     */
    public static void setupTodayCardStyle(Context context, com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(TODAY_STROKE_WIDTH);
        cardView.setCardElevation(TODAY_ELEVATION);
        cardView.setCardBackgroundColor(getColorByThemeAttr(context, R.attr.colorTodayUserBackground));
        cardView.setStrokeColor(getColorByThemeAttr(context,
                androidx.appcompat.R.attr.colorPrimary));

        Log.v(TAG, "Today card style applied");
    }

    /**
     * Setup card style for Sunday with subtle highlighting
     */
    public static void setupSundayCardStyle(Context context, com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH);
        cardView.setCardElevation(SUNDAY_ELEVATION);
        cardView.setCardBackgroundColor(
                blendEventColorWithWhite(
                        getColorByThemeAttr(context,
                                com.google.android.material.R.attr.colorSurfaceVariant)));
        cardView.setStrokeColor(
                ContextCompat.getColor(context, android.R.color.holo_red_light));

        Log.v(TAG, "Sunday card style applied");
    }

    /**
     * Setup card style for EVENTS with subtle highlighting
     */
    public static void setupEventsCardStyle(Context context, EventIndicatorHelper helper, com.google.android.material.card.MaterialCardView cardView, List<LocalEvent> events) {
        // Ottenere colore dominante eventi
        int eventColor = getDominantEventTypeColor(context, helper, events);

        // Applicare blend con bianco per background leggibile
        int lightBackground = blendEventColorWithWhite(eventColor);

        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH); // FIX: Bordo normale, non spesso
        cardView.setCardElevation(EVENTS_ELEVATION);
        cardView.setCardBackgroundColor(lightBackground);
        cardView.setStrokeColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorOutlineVariant));

        Log.v(TAG, "Events card style applied");
    }

    /**
     * ✅ MIGLIORATO: Old days overlay (non sovrascrive, ma modifica)
     */
    private static void applyOldDaysOverlay(com.google.android.material.card.MaterialCardView cardView) {
        // Non cambiare background o stroke, solo alpha
        cardView.setAlpha(OLD_DAYS_ALPHA);
        Log.v(TAG, "Old days overlay applied (alpha only)");
    }


    // ===================================== HELPER METHODS

    /**
     * ottenere colore dominante eventi:
     */
    private static int getDominantEventTypeColor(Context context, EventIndicatorHelper helper, List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            return ContextCompat.getColor(context, R.color.event_type_general);
        }

        // Usare EventIndicatorHelper per ottenere colore priorità più alta
        return helper.getHighestPriorityColor(events);
    }

    /**
     * AGGIUNGERE questo metodo in CalendarAdapterLegacy.java (copiato da DaysListAdapter funzionante):
     */
    private static int blendEventColorWithWhite(int eventColor) {
        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        // Blend con bianco (84% bianco, 16% evento) - stessi valori che funzionano in DaysList
        float eventWeight = 0.10f;
        float whiteWeight = 0.90f;

        int blendedRed = (int) (255 * whiteWeight + eventRed * eventWeight);
        int blendedGreen = (int) (255 * whiteWeight + eventGreen * eventWeight);
        int blendedBlue = (int) (255 * whiteWeight + eventBlue * eventWeight);

        return android.graphics.Color.rgb(blendedRed, blendedGreen, blendedBlue);
    }
}
