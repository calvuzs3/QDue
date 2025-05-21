package net.calvuz.qdue.quattrodue.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory;
import net.calvuz.qdue.utils.Log;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.List;

/**
 * Activity per la configurazione dei turni.
 * Questa activity permette di configurare il numero di turni e le loro caratteristiche.
 */
public class ShiftSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ShiftSettingsActivity";
    private static final boolean LOG_ENABLED = true;

    // UI Components
    private RecyclerView recyclerView;
    private ShiftTypeAdapter adapter;
    private NumberPicker shiftCountPicker;
    private FloatingActionButton fabSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_settings);

        // Configura la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configura la action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.shift_settings_title);
        }

        // Configura il picker per il numero di turni
        shiftCountPicker = findViewById(R.id.shift_count_picker);
        shiftCountPicker.setMinValue(1);
        shiftCountPicker.setMaxValue(5);
        shiftCountPicker.setValue(ShiftTypeFactory.getShiftCount(this));
        shiftCountPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (LOG_ENABLED) Log.d(TAG, "Numero turni cambiato da " + oldVal + " a " + newVal);
            ShiftTypeFactory.setShiftCount(this, newVal);
            loadShiftTypes();
        });

        // Configura la RecyclerView
        recyclerView = findViewById(R.id.shift_types_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Carica i tipi di turno
        loadShiftTypes();

        // Configura il pulsante di reset
        Button resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> {
            if (LOG_ENABLED) Log.d(TAG, "Reset turni ai valori predefiniti");
            ShiftTypeFactory.resetToDefaults(this);
            shiftCountPicker.setValue(ShiftTypeFactory.getShiftCount(this));
            loadShiftTypes();
            Toast.makeText(this, R.string.msg_shifts_reset, Toast.LENGTH_SHORT).show();
        });

        // Configura il pulsante di salvataggio
        fabSave = findViewById(R.id.fab_save);
        fabSave.setOnClickListener(v -> {
            if (LOG_ENABLED) Log.d(TAG, "Salvataggio configurazione turni");
            setResult(RESULT_OK);
            finish();
            Toast.makeText(this, R.string.msg_shifts_saved, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Carica i tipi di turno nella RecyclerView.
     */
    private void loadShiftTypes() {
        List<ShiftType> shiftTypes = ShiftTypeFactory.loadAllShiftTypes(this);
        if (LOG_ENABLED) Log.d(TAG, "Caricati " + shiftTypes.size() + " tipi di turno");

        adapter = new ShiftTypeAdapter(this, shiftTypes);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    /**
     * Adapter per la visualizzazione e modifica dei tipi di turno.
     */
    private class ShiftTypeAdapter extends RecyclerView.Adapter<ShiftTypeAdapter.ViewHolder> {

        private final Context context;
        private final List<ShiftType> shiftTypes;

        public ShiftTypeAdapter(Context context, List<ShiftType> shiftTypes) {
            this.context = context;
            this.shiftTypes = shiftTypes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shift_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ShiftType shiftType = shiftTypes.get(position);

            // Imposta i valori nei campi
            holder.nameEditText.setText(shiftType.getName());
            holder.descriptionEditText.setText(shiftType.getDescription());

            // Configura i number picker per l'ora
            holder.startHourPicker.setMinValue(0);
            holder.startHourPicker.setMaxValue(23);
            holder.startHourPicker.setValue(shiftType.getStartHour());

            // Configura i number picker per i minuti
            holder.startMinutePicker.setMinValue(0);
            holder.startMinutePicker.setMaxValue(59);
            holder.startMinutePicker.setValue(shiftType.getStartMinute());

            // Configura i number picker per la durata in ore
            holder.durationHourPicker.setMinValue(1);
            holder.durationHourPicker.setMaxValue(12);
            holder.durationHourPicker.setValue(shiftType.getDurationHours());

            // Configura i number picker per la durata in minuti
            holder.durationMinutePicker.setMinValue(0);
            holder.durationMinutePicker.setMaxValue(59);
            holder.durationMinutePicker.setValue(shiftType.getDurationMinutes());

            // Imposta il colore
            int color = shiftType.getColor();
            holder.colorPreview.setBackgroundColor(color);
            holder.colorValueText.setText(String.format("#%06X", (0xFFFFFF & color)));

            // Imposta il listener per il pulsante di selezione colore
            holder.colorCardView.setOnClickListener(v -> showColorPicker(position, holder));

            // Imposta il listener per il pulsante di salvataggio
            holder.saveButton.setOnClickListener(v -> saveShiftType(position, holder));
        }

        @Override
        public int getItemCount() {
            return shiftTypes.size();
        }

        /**
         * Mostra il color picker per selezionare il colore del turno.
         *
         * @param position Posizione del turno
         * @param holder ViewHolder contenente le viste
         */
        private void showColorPicker(int position, ViewHolder holder) {
            if (LOG_ENABLED) Log.d(TAG, "Apertura color picker per turno " + position);

            // Ottiene il colore attuale
            int currentColor = 0;
            if (holder.colorPreview.getBackground() != null) {
                try {
                    currentColor = ((android.graphics.drawable.ColorDrawable) holder.colorPreview.getBackground()).getColor();
                } catch (ClassCastException e) {
                    if (LOG_ENABLED) Log.e(TAG, "Errore nel recupero del colore: " + e.getMessage());
                }
            }

            // Crea e mostra il dialog per la selezione del colore
            new ColorPickerDialog.Builder(context)
                    .setTitle(R.string.dialog_pick_color)
                    .setPreferenceName("ColorPickerDialog")
                    .setPositiveButton(R.string.confirm,
                            (ColorEnvelopeListener) (envelope, fromUser) -> {
                                int color = envelope.getColor();
                                holder.colorPreview.setBackgroundColor(color);
                                holder.colorValueText.setText(String.format("#%06X", (0xFFFFFF & color)));
                            })
                    .setNegativeButton(R.string.cancel,
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(false)
                    .attachBrightnessSlideBar(true)
                    .setBottomSpace(12)
                    .show();
        }

        /**
         * Salva le modifiche a un tipo di turno.
         *
         * @param position Posizione del turno
         * @param holder ViewHolder contenente le viste
         */
        private void saveShiftType(int position, ViewHolder holder) {
            String name = holder.nameEditText.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(context, R.string.error_shift_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            String description = holder.descriptionEditText.getText().toString();
            int startHour = holder.startHourPicker.getValue();
            int startMinute = holder.startMinutePicker.getValue();
            int durationHours = holder.durationHourPicker.getValue();
            int durationMinutes = holder.durationMinutePicker.getValue();

            // Ottiene il colore dal preview
            int color = 0;
            if (holder.colorPreview.getBackground() != null) {
                try {
                    color = ((android.graphics.drawable.ColorDrawable) holder.colorPreview.getBackground()).getColor();
                } catch (ClassCastException e) {
                    if (LOG_ENABLED) Log.e(TAG, "Errore nel recupero del colore: " + e.getMessage());
                }
            }

            // Crea il nuovo tipo di turno
            ShiftType shiftType = ShiftTypeFactory.createCustom(
                    name, description, startHour, startMinute, durationHours, durationMinutes, color);

            if (LOG_ENABLED) Log.d(TAG, "Salvataggio turno " + position + ": " + shiftType);

            // Salva nelle preferenze
            ShiftTypeFactory.saveShiftType(context, position, shiftType);

            // Aggiorna la lista
            shiftTypes.set(position, shiftType);
            notifyItemChanged(position);

            // Mostra un messaggio di conferma
            Toast.makeText(context, R.string.msg_shift_saved, Toast.LENGTH_SHORT).show();
        }

        /**
         * ViewHolder per le viste dei tipi di turno.
         */
        public class ViewHolder extends RecyclerView.ViewHolder {

            public EditText nameEditText;
            public EditText descriptionEditText;
            public NumberPicker startHourPicker;
            public NumberPicker startMinutePicker;
            public NumberPicker durationHourPicker;
            public NumberPicker durationMinutePicker;
            public MaterialCardView colorCardView;
            public View colorPreview;
            public TextView colorValueText;
            public Button saveButton;

            public ViewHolder(View view) {
                super(view);

                nameEditText = view.findViewById(R.id.shift_name_edit);
                descriptionEditText = view.findViewById(R.id.shift_description_edit);
                startHourPicker = view.findViewById(R.id.start_hour_picker);
                startMinutePicker = view.findViewById(R.id.start_minute_picker);
                durationHourPicker = view.findViewById(R.id.duration_hour_picker);
                durationMinutePicker = view.findViewById(R.id.duration_minute_picker);
                colorCardView = view.findViewById(R.id.color_card_view);
                colorPreview = view.findViewById(R.id.color_preview);
                colorValueText = view.findViewById(R.id.color_value_text);
                saveButton = view.findViewById(R.id.save_shift_button);
            }
        }
    }
}
