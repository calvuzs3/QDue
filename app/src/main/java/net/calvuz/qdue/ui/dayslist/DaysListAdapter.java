package net.calvuz.qdue.ui.dayslist;

import android.content.Context;

import net.calvuz.qdue.ui.shared.BaseAdapter;
import net.calvuz.qdue.ui.shared.SharedViewModels;
import net.calvuz.qdue.quattrodue.models.HalfTeam;

import java.util.List;

/**
 * Adapter specializzato per DayslistViewFragment.
 */
class DaysListAdapter extends BaseAdapter {


    public DaysListAdapter(Context context, List<SharedViewModels.ViewItem> items, HalfTeam userHalfTeam, int numShifts) {
        super(context, items, userHalfTeam, numShifts);
    }

    // Utilizza tutti i metodi default della classe base
    // Nessuna personalizzazione necessaria per la lista giorni
}