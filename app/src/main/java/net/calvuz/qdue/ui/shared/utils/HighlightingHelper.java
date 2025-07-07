package net.calvuz.qdue.ui.shared.utils;

import static net.calvuz.qdue.utils.Library.getColorByThemeAttr;

import android.content.Context;

import androidx.core.content.ContextCompat;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.util.List;

public final class HighlightingHelper {

    private static final String TAG = "HIGHLIGHTING";

    private static final int NORMAL_STROKE_WIDTH = 2;
    private static final int TODAY_STROKE_WIDTH = 0;

    private static final float NORMAL_ELEVATION = 0f;
    private static final float TODAY_ELEVATION = 4f;
    private static final float SUNDAY_ELEVATION = 2f;
    private static final float EVENTS_ELEVATION = 2f;

    private static final float OLD_DAYS_ALPHA = 0.75f;

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
     * Setup card style for today
     */
    public static void setupOldDaysCardStyle(com.google.android.material.card.MaterialCardView cardView) {
        cardView.setStrokeWidth(NORMAL_STROKE_WIDTH);
        cardView.setCardElevation(NORMAL_ELEVATION);
        cardView.setAlpha(OLD_DAYS_ALPHA); // initial 0.75

        Log.v(TAG, "Old days card style applied");
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
