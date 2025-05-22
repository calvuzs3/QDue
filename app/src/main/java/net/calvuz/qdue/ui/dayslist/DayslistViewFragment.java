package net.calvuz.qdue.ui.dayslist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.QuattroDue;
import net.calvuz.qdue.quattrodue.adapters.QuattroDueEnhancedAdapter;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDate;

/**
 * Fragment che mostra la visualizzazione calendario dei turni con scroll infinito,
 * header mensili e FAB per tornare alla data odierna.
 */
public class DayslistViewFragment extends Fragment {

    private static final String TAG = "QDueHomeFragment";
    private static final boolean LOG_ENABLED = true;

    private OnQuattroDueDayslistFragmentInteractionListener mListener = null;

    private QuattroDue mQD;

    private RecyclerView mRecyclerView;
    private QuattroDueEnhancedAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private FloatingActionButton mFabToToday;

    private View mRoot;

    // Flag per evitare loop nelle chiamate di scroll
    private boolean mScrolling = false;

    // Flag per sapere se il FAB deve essere mostrato
    private boolean mShowFab = false;

    /**
     * Costruttore vuoto obbligatorio per il fragment manager.
     */
    public DayslistViewFragment() {
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
        mRoot = inflater.inflate(R.layout.fragment_dayslist_view, container, false);
        if (LOG_ENABLED) Log.d(TAG, "onCreateView");

        // Inizializza le viste
        mRecyclerView = mRoot.findViewById(R.id.dayslist_recyclerview);
        mFabToToday = mRoot.findViewById(R.id.fab_go_to_today);

        // Configura RecyclerView con LayoutManager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Inizializza QuattroDue
        if (getActivity() != null) {
            mQD = QuattroDue.getInstance(getActivity().getApplicationContext());
            if (LOG_ENABLED) Log.d(TAG, "QuattroDue inizializzato");

            // Crea l'adapter avanzato
            mAdapter = new QuattroDueEnhancedAdapter(getActivity());
            mRecyclerView.setAdapter(mAdapter);

            // Scrolla alla posizione del giorno corrente
            scrollToToday();

            // Configura il FAB
            setupFloatingActionButton();

            // Aggiunge listener per aggiornare il titolo e la visibilità del FAB durante lo scroll
            setupScrollListener();

            if (LOG_ENABLED) Log.d(TAG, "Adapter avanzato impostato");
        } else {
            if (LOG_ENABLED) Log.e(TAG, "getActivity() ha restituito null");
        }

        return mRoot;
    }

    /**
     * Configura il Floating Action Button.
     */
    private void setupFloatingActionButton() {
        if (mFabToToday != null) {
            // Inizialmente nascosto
            mFabToToday.hide();

            mFabToToday.setOnClickListener(v -> {
                if (LOG_ENABLED) Log.d(TAG, "FAB clicked - scrolling to today");
                scrollToToday();
                mFabToToday.hide(); // Nasconde il FAB dopo il click
                mShowFab = false;
            });

            if (LOG_ENABLED) Log.d(TAG, "FAB configurato");
        }
    }

    /**
     * Configura il listener per lo scroll che aggiorna il titolo della toolbar e la visibilità del FAB.
     */
    private void setupScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!mScrolling) {
                    updateToolbarTitle();
                    updateFabVisibility();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Aggiorna la visibilità del FAB quando lo scroll è terminato
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateFabVisibility();
                }
            }
        });
    }

    /**
     * Aggiorna la visibilità del FAB basandosi sulla posizione attuale.
     */
    private void updateFabVisibility() {
        if (mLayoutManager == null || mFabToToday == null) return;

        int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();

        if (firstVisiblePosition != RecyclerView.NO_POSITION && lastVisiblePosition != RecyclerView.NO_POSITION) {
            // Calcola se oggi è visibile
            int todayPosition = mAdapter.getTodayPosition();
            boolean todayVisible = todayPosition >= firstVisiblePosition && todayPosition <= lastVisiblePosition;

            // Mostra il FAB solo se oggi non è visibile
            boolean shouldShowFab = !todayVisible;

            if (shouldShowFab != mShowFab) {
                mShowFab = shouldShowFab;
                if (mShowFab) {
                    mFabToToday.show();
                } else {
                    mFabToToday.hide();
                }

                if (LOG_ENABLED) {
                    Log.v(TAG, "FAB visibility updated: " + (mShowFab ? "visible" : "hidden") +
                            " (today pos: " + todayPosition + ", visible range: " +
                            firstVisiblePosition + "-" + lastVisiblePosition + ")");
                }
            }
        }
    }

    /**
     * Aggiorna il titolo della toolbar con il mese del primo elemento visibile.
     */
    private void updateToolbarTitle() {
        if (mLayoutManager != null && mAdapter != null) {
            int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
            if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                LocalDate visibleDate = getDateForPosition(firstVisiblePosition);
                if (visibleDate != null && getActivity() != null) {
                    // Aggiorna il cursore in QuattroDue per mantenere la coerenza
                    LocalDate firstOfMonth = LocalDate.of(visibleDate.getYear(), visibleDate.getMonth(), 1);

                    // Questo dovrebbe essere chiamato sull'activity principale
                    if (mListener != null) {
                        mListener.onMonthChanged(firstOfMonth);
                    }
                }
            }
        }
    }

    /**
     * Calcola la data per una determinata posizione.
     */
    private LocalDate getDateForPosition(int position) {
        // Utilizza la stessa logica dell'adapter
        final int INITIAL_POSITION = 10000;
        LocalDate referenceDate = LocalDate.now();

        // Stima la posizione del giorno escludendo gli header
        int dayPosition = position - (position / 31);
        int offset = dayPosition - INITIAL_POSITION;
        return referenceDate.plusDays(offset);
    }

    /**
     * Scrolla alla posizione del giorno corrente con animazione fluida.
     */
    private void scrollToToday() {
        if (mAdapter != null && mLayoutManager != null) {
            mScrolling = true;
            int todayPosition = mAdapter.getTodayPosition();

            // Usa smoothScrollToPosition per un'animazione fluida
            mRecyclerView.smoothScrollToPosition(todayPosition);

            // Dopo un breve delay, centra la vista
            mRecyclerView.postDelayed(() -> {
                mLayoutManager.scrollToPositionWithOffset(todayPosition,
                        mRecyclerView.getHeight() / 2 - mRecyclerView.getChildAt(0).getHeight() / 2);
                mScrolling = false;
            }, 500);

            if (LOG_ENABLED) Log.d(TAG, "Scrolling to today: position " + todayPosition);
        }
    }

    /**
     * Scrolla a una data specifica.
     */
    public void scrollToDate(LocalDate date) {
        if (mAdapter != null && mLayoutManager != null) {
            mScrolling = true;
            int position = mAdapter.getPositionForDate(date);
            mRecyclerView.smoothScrollToPosition(position);

            mRecyclerView.postDelayed(() -> {
                mLayoutManager.scrollToPositionWithOffset(position, 0);
                mScrolling = false;
            }, 500);

            if (LOG_ENABLED) Log.d(TAG, "Scrolled to date: " + date + " position: " + position);
        }
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
                    mAdapter.notifyDataSetChanged();
                    if (LOG_ENABLED) Log.d(TAG, "onStart: dati aggiornati");
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LOG_ENABLED) Log.d(TAG, "onResume");

        // Aggiorna la visualizzazione quando si torna al fragment
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        // Aggiorna la visibilità del FAB
        updateFabVisibility();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (LOG_ENABLED) Log.d(TAG, "onAttach");
        if (context instanceof OnQuattroDueDayslistFragmentInteractionListener) {
            mListener = (OnQuattroDueDayslistFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context +
                    " deve implementare OnQuattroDueDayslistFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (LOG_ENABLED) Log.d(TAG, "onDetach");
        mListener = null;
    }

    /**
     * Aggiorna la lista dei giorni.
     */
    protected void updateList() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            if (LOG_ENABLED) Log.d(TAG, "updateList: notifyDataSetChanged chiamato");
        } else {
            if (LOG_ENABLED) Log.e(TAG, "updateList: mAdapter è null");
        }
    }

    /**
     * Notifica gli aggiornamenti dell'interfaccia.
     */
    public void notifyUpdates() {
        if (LOG_ENABLED) Log.d(TAG, "notifyUpdates chiamato");
        updateList();
        updateFabVisibility();
    }

    /**
     * Metodo chiamato dai pulsanti di navigazione (se ancora presenti).
     */
    public void moveToNextMonth() {
        if (mLayoutManager != null) {
            int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
            LocalDate currentDate = getDateForPosition(firstVisible);
            LocalDate nextMonth = currentDate.plusMonths(1).withDayOfMonth(1);
            scrollToDate(nextMonth);
        }
    }

    /**
     * Metodo chiamato dai pulsanti di navigazione (se ancora presenti).
     */
    public void moveToPreviousMonth() {
        if (mLayoutManager != null) {
            int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
            LocalDate currentDate = getDateForPosition(firstVisible);
            LocalDate previousMonth = currentDate.minusMonths(1).withDayOfMonth(1);
            scrollToDate(previousMonth);
        }
    }

    /**
     * Interfaccia per la comunicazione con l'activity.
     */
    public interface OnQuattroDueDayslistFragmentInteractionListener {
        void onQuattroDueHomeFragmentInteractionListener(long id);

        /**
         * Chiamato quando il mese visibile cambia durante lo scroll.
         */
        void onMonthChanged(LocalDate newMonth);
    }
}
