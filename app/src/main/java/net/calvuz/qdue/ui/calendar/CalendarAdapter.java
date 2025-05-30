package net.calvuz.qdue.ui.calendar;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.HalfTeam;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.utils.Log;
import net.calvuz.qdue.R;

import java.util.List;

public class CalendarAdapter extends BaseAdapter {

    /**
     * Adapter specializzato per CalendarViewFragment.
     */

        public CalendarAdapter(Context context, List<SharedViewModels.ViewItem> items,
                               HalfTeam userHalfTeam) {
            super(context, items, userHalfTeam, 1); // Calendar non mostra dettagli turni
        }

        @Override
        protected RecyclerView.ViewHolder createDayViewHolder(LayoutInflater inflater, ViewGroup parent) {
            // Usa il layout specifico del calendario
            View view = inflater.inflate(R.layout.item_calendar_day, parent, false);
            return new CalendarDayViewHolder(view);
        }

        @Override
        protected void bindDay(DayViewHolder holder, SharedViewModels.DayItem dayItem, int position) {
            if (!(holder instanceof CalendarDayViewHolder)) {
                super.bindDay(holder, dayItem, position);
                return;
            }

            CalendarDayViewHolder calendarHolder = (CalendarDayViewHolder) holder;
            Day day = dayItem.day;

            if (day == null) {
                // Cella vuota del calendario
                calendarHolder.tvDayNumber.setText("");
                calendarHolder.tvDayName.setText("");
                calendarHolder.vShiftIndicator.setVisibility(View.INVISIBLE);
                calendarHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                return;
            }

            // Imposta il numero del giorno
            calendarHolder.tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));

            // Nascondi il nome del giorno
            calendarHolder.tvDayName.setVisibility(View.GONE);

            // Evidenzia oggi
            if (dayItem.isToday()) {
                calendarHolder.itemView.setBackgroundColor(mCachedTodayBackgroundColor);
            } else {
                calendarHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            // Colore domenica
            if (dayItem.isSunday()) {
                calendarHolder.tvDayNumber.setTextColor(mCachedSundayTextColor);
            } else {
                calendarHolder.tvDayNumber.setTextColor(mCachedNormalTextColor);
            }

            // Indicatore turno utente
            int userShiftPosition = -1;
            if (mUserHalfTeam != null) {
                userShiftPosition = day.getInWichTeamIsHalfTeam(mUserHalfTeam);
            }

            if (userShiftPosition >= 0) {
                calendarHolder.vShiftIndicator.setVisibility(View.VISIBLE);
                int shiftColor = getShiftColor(day, userShiftPosition);
                if (shiftColor != 0) {
                    calendarHolder.vShiftIndicator.setBackgroundColor(shiftColor);
                } else {
                    calendarHolder.vShiftIndicator.setBackgroundColor(
                            mContext.getResources().getColor(R.color.background_user_shift));
                }
            } else {
                calendarHolder.vShiftIndicator.setVisibility(View.INVISIBLE);
            }
        }

        private int getShiftColor(Day day, int shiftPosition) {
            try {
                if (shiftPosition < day.getShifts().size()) {
                    return day.getShifts().get(shiftPosition).getShiftType().getColor();
                }
            } catch (Exception e) {
                // Ignora errori
                Log.e(TAG, e.getMessage());
            }
            return 0;
        }

        /**
         * ViewHolder specifico per le celle del calendario.
         */
        public class CalendarDayViewHolder extends DayViewHolder {
            public final TextView tvDayNumber;
            public final TextView tvDayName;
            public final View vShiftIndicator;

            public CalendarDayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayNumber = itemView.findViewById(R.id.tv_day_number);
                tvDayName = itemView.findViewById(R.id.tv_day_name);
                vShiftIndicator = itemView.findViewById(R.id.v_shift_indicator);
            }
        }
        

}
