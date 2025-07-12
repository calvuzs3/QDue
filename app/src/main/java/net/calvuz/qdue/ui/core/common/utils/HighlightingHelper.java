package net.calvuz.qdue.ui.core.common.utils;

import static net.calvuz.qdue.ui.core.common.utils.Library.getColorByThemeAttr;

import android.content.Context;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;

import java.time.LocalDate;
import java.util.List;

public final class HighlightingHelper {

    private static final String TAG = "HIGHLIGHTING";

    private static final int NORMAL_STROKE_WIDTH = 2;
    private static final int TODAY_STROKE_WIDTH = 0;

    private static final float NORMAL_ELEVATION = 0f;
    private static final float TODAY_ELEVATION = 6f;
    private static final float SUNDAY_ELEVATION = 2f;
    private static final float EVENTS_ELEVATION = 2f;

    private static final float OLD_DAYS_ALPHA = 0.45f;

    private static final float BLEND_WHITE_WEIGHT = 0.92f;

    /**
     * ✅ SOLUTION: Force refresh universale per tutti i giorni
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

//        Log.d(TAG, String.format("Highlighting %s: today=%s, sunday=%s, events=%s, old=%s",
//                date, isToday, isSunday, hasEvents, isOldDay));

        // ✅ STEP 1: Reset to regular style (baseline)
        setupRegularCardStyle(context, cardView);

        // ✅ STEP 2: Apply BACKGROUND styles in PRIORITY ORDER

        // Priority 3: Sunday background (only if no events and not today)
        if (isSunday && !isToday && !hasEvents) {
            setupSundayCardStyle(context, cardView);
//            Log.d(TAG, "Applied SUNDAY background");
        }

        // Priority 2: Events background (if not today)
        else if (hasEvents && !isToday) {
            setupEventsCardStyle(context, eventHelper, cardView, events);
//            Log.d(TAG, "Applied EVENTS background");
        }

        // Priority 1: Today background (ALWAYS wins)
        if (isToday) {
            setupTodayCardStyle(context, cardView);
//            Log.d(TAG, "Applied TODAY background");
        }

        // ✅ STEP 3: Apply overlay and UNIVERSAL force refresh
        if (isOldDay) {
            // Old days: alpha overlay
            cardView.setAlpha(OLD_DAYS_ALPHA);
//            Log.d(TAG, "Applied OLD_DAYS overlay");
        } else {
            // ✅ SOLUTION: Force refresh for ALL other days (future + today)
            applyUniversalRefresh(cardView);
//            Log.d(TAG, "Applied UNIVERSAL refresh");
        }
    }

    /**
     * ✅ SAME: Text highlighting (già funziona)
     */
    public static void applyUnifiedTextHighlighting(Context context,
                                                    LocalDate date,
                                                    TextView... textViews) {

        if (textViews == null || textViews.length == 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        boolean isToday = date.equals(today);
        boolean isSunday = date.getDayOfWeek().getValue() == 7;

//        Log.i(TAG, String.format(QDue.getLocale(), "Text highlighting %s: today=%s, sunday=%s, views=%d",
//                date, isToday, isSunday, textViews.length));

        for (int i = 0; i < textViews.length; i++) {
            TextView textView = textViews[i];
            if (textView == null) continue;

            // ✅ Priority 1: Today text color
            if (isToday) {
                int todayColor = getColorByThemeAttr(context, androidx.appcompat.R.attr.colorPrimary);
                textView.setTextColor(todayColor);
                textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
//                Log.v(TAG, "Applied TODAY text to TextView[" + i + "]");
            }
            // ✅ Priority 2: Sunday text color (SEMPRE applicato)
            else if (isSunday) {
                int sundayColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
                textView.setTextColor(sundayColor);
                textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
//                Log.v(TAG, "Applied SUNDAY text to TextView[" + i + "] - RED");
            }
            // ✅ Default: Regular text
            else {
                int regularColor = getColorByThemeAttr(context, com.google.android.material.R.attr.colorOnSurface);
                textView.setTextColor(regularColor);
                textView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL);
//                Log.v(TAG, "Applied REGULAR text to TextView[" + i + "]");
            }

            // ✅ UNIVERSAL: Force refresh EVERY TextView
            forceTextViewRefresh(textView);
        }
    }

    /**
     * ✅ NEW: Universal refresh per tutti i giorni (eccetto old days)
     * Usa lo stesso "trucco alpha" che funziona per old days
     */
    private static void applyUniversalRefresh(com.google.android.material.card.MaterialCardView cardView) {
        // Stesso identico meccanismo che funziona per old days
        // Ma con alpha = 1.0 (quindi invisibile all'utente)

        // Metodo 1: Force refresh con alpha trick
        //cardView.setAlpha(0.95f);  // Quasi impercettibile, ma forza refresh
        cardView.post(() -> cardView.setAlpha(1.0f));

        // Metodo 2: Backup invalidation
        cardView.invalidate();
        cardView.requestLayout();

//        Log.v(TAG, "Universal refresh applied");
    }

    /**
     * ✅ NEW: Force refresh per TextView
     */
    private static void forceTextViewRefresh(TextView textView) {
        // Force rendering refresh
        textView.invalidate();

        // Trigger layout pass
        textView.requestLayout();

        // Alpha trick for TextView (se necessario)
        textView.setAlpha(0.999f);
        textView.post(() -> textView.setAlpha(1.0f));
    }

    /**
     * Setup card style for regular days
     */
    public static void setupRegularCardStyle(Context context, com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH);
        cardView.setCardElevation(NORMAL_ELEVATION);
        cardView.setCardBackgroundColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorSurface));
        cardView.setStrokeColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorOutlineVariant));

//        Log.v(TAG, "Regular card style applied");
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

//        Log.v(TAG, "Today card style applied");
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

//        Log.v(TAG, "Sunday card style applied");
    }

    /**
     * Setup card style for EVENTS with subtle highlighting
     */
    public static void setupEventsCardStyle(Context context, EventIndicatorHelper helper, com.google.android.material.card.MaterialCardView cardView, List<LocalEvent> events) {
        // Ottenere colore dominante eventi
        int eventColor = getDominantEventTypeColor(context, helper, events);

        // Applicare blend con bianco per background leggibile
        int lightBackground = blendEventColorWithWhite(eventColor);

        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH);
        cardView.setCardElevation(EVENTS_ELEVATION);
        cardView.setCardBackgroundColor(lightBackground);
        cardView.setStrokeColor(getColorByThemeAttr(context,
                com.google.android.material.R.attr.colorOutlineVariant));

//        Log.v(TAG, "Events card style applied");
    }

    // ===================================== HELPER METHODS

    /**
     * Ottenere colore dominante eventi
     */
    private static int getDominantEventTypeColor(Context context, EventIndicatorHelper helper, List<LocalEvent> events) {
        if (events == null || events.isEmpty()) {
            return ContextCompat.getColor(context, R.color.event_type_general);
        }

        return helper.getHighestPriorityColor(events);
    }

    /**
     * Blend event color with white for readable background
     */
    private static int blendEventColorWithWhite(int eventColor) {
        int eventRed = android.graphics.Color.red(eventColor);
        int eventGreen = android.graphics.Color.green(eventColor);
        int eventBlue = android.graphics.Color.blue(eventColor);

        float whiteWeight = BLEND_WHITE_WEIGHT;
        float eventWeight = 1 - BLEND_WHITE_WEIGHT;

        int blendedRed = (int) (255 * whiteWeight + eventRed * eventWeight);
        int blendedGreen = (int) (255 * whiteWeight + eventGreen * eventWeight);
        int blendedBlue = (int) (255 * whiteWeight + eventBlue * eventWeight);

        return android.graphics.Color.rgb(blendedRed, blendedGreen, blendedBlue);
    }
}