package net.calvuz.qdue.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.adapters.QuattroDueListAdapter;
import net.calvuz.qdue.quattrodue.models.Day;
import net.calvuz.qdue.quattrodue.models.Month;
import net.calvuz.qdue.utils.Log;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment che mostra la visualizzazione calendario dei turni.
 * Versione aggiornata senza header con pulsanti, poiché sono stati spostati nella bottom bar.
 */
public class QDueHomeFragment extends Fragment {

    private static final String TAG = "QDueHomeFragment";
    private static final boolean LOG_ENABLED = true;

    private OnQuattroDueHomeFragmentInteractionListener mListener = null;

    private QuattroDue mQD;

    private RecyclerView mRecyclerView;
    private QuattroDueListAdapter mAdapter;

    private View mRoot;

    /**
     * Costruttore vuoto obbligatorio per il fragment manager.
     */
    public QDueHomeFragment() {
        // Costruttore vuoto richiesto
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LOG_ENABLED) Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_qdue_home, container, false);
        if (LOG_ENABLED) Log.d(TAG, "onCreateView");

        // Inizializza le viste
        mRecyclerView = mRoot.findViewById(R.id.dayslist_recyclerview);

        // Configura RecyclerView
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inizializza QuattroDue
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            if (LOG_ENABLED) Log.d(TAG, "QuattroDue inizializzato");

            // Ottiene i dati del mese e crea l'adapter
            Month currentMonth = mQD.getMonth();
            if (currentMonth != null) {
                // Converti la List<Day> in ArrayList<Day> per retrocompatibilità
                List<Day> daysList = currentMonth.getDaysList();
                ArrayList<Day> daysArrayList = new ArrayList<>(daysList);

                // Crea e imposta l'adapter
                mAdapter = new QuattroDueListAdapter(getActivity(), daysArrayList);
                mRecyclerView.setAdapter(mAdapter);
                if (LOG_ENABLED) Log.d(TAG, "Adapter impostato con " + daysArrayList.size() + " giorni");
            } else {
                if (LOG_ENABLED) Log.e(TAG, "currentMonth è null");
            }
        } else {
            if (LOG_ENABLED) Log.e(TAG, "getActivity() ha restituito null");
        }

        // Carica i dati dei giorni
        loadDaysData();

        return mRoot;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (LOG_ENABLED) Log.d(TAG, "onStart");

        // Legge le preferenze
        if (mQD != null) {
            mQD.updatePreferences(getActivity());
            if (mQD.isRefresh()) {
                mQD.setRefresh(false);
                if (mAdapter != null) {
                    // Converti la List<Day> in ArrayList<Day> per retrocompatibilità
                    List<Day> daysList = mQD.getMonth().getDaysList();
                    ArrayList<Day> daysArrayList = new ArrayList<>(daysList);
                    mAdapter.setDaysList(daysArrayList);
                    if (LOG_ENABLED) Log.d(TAG, "onStart: dati aggiornati");
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (LOG_ENABLED) Log.d(TAG, "onAttach");
        if (context instanceof OnQuattroDueHomeFragmentInteractionListener) {
            mListener = (OnQuattroDueHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context +
                    " deve implementare OnQuattroDueHomeFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (LOG_ENABLED) Log.d(TAG, "onDetach");
        mListener = null;
    }

    /**
     * Carica i dati dei giorni nell'adapter.
     */
    private void loadDaysData() {
        if (mQD == null) {
            if (LOG_ENABLED) Log.e(TAG, "loadDaysData: mQD è null");
            return;
        }

        try {
            // Ottiene i giorni del mese corrente
            Month currentMonth = mQD.getMonth();
            if (currentMonth != null && mAdapter != null) {
                // Converti la List<Day> in ArrayList<Day> per retrocompatibilità
                List<Day> daysList = currentMonth.getDaysList();
                ArrayList<Day> daysArrayList = new ArrayList<>(daysList);

                // Aggiorna l'adapter
                mAdapter.setDaysList(daysArrayList);
                if (LOG_ENABLED) Log.d(TAG, "loadDaysData: dati caricati (" + daysArrayList.size() + " giorni)");
            } else {
                if (LOG_ENABLED) Log.e(TAG, "loadDaysData: currentMonth o mAdapter è null");
            }
        } catch (Exception e) {
            if (LOG_ENABLED) Log.e(TAG, "Errore nel caricamento dei dati: " + e.getMessage());
        }
    }

    /**
     * Aggiorna la lista dei giorni.
     */
    protected void updateList() {
        if (mAdapter != null && mQD != null) {
            loadDaysData();
            mAdapter.notifyDataSetChanged();
            if (LOG_ENABLED) Log.d(TAG, "updateList: notifyDataSetChanged chiamato");
        } else {
            if (LOG_ENABLED) Log.e(TAG, "updateList: mAdapter o mQD è null");
        }
    }

    /**
     * Notifica gli aggiornamenti dell'interfaccia.
     */
    public void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates chiamato");
        updateList();
    }

    /**
     * Interfaccia per la comunicazione con l'activity.
     */
    public interface OnQuattroDueHomeFragmentInteractionListener {
        void onQuattroDueHomeFragmentInteractionListener(long id);
    }
}