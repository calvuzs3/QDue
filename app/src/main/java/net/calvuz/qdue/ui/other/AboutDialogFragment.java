package net.calvuz.qdue.ui.other;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.FragmentDialogAboutBinding;
import net.calvuz.qdue.quattrodue.models.ShiftType;
import net.calvuz.qdue.quattrodue.utils.ShiftTypeFactory;

import java.util.List;

public class AboutDialogFragment extends DialogFragment {

    private FragmentDialogAboutBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Infla il layout
        View view = getLayoutInflater().inflate(R.layout.fragment_dialog_about, null);

        // Configura il contenuto del dialog
        setupDialogContent(view);

        // Configura la legenda dei turni
        setupShiftsLegend(view);

        // Crea l'AlertDialog con titolo e icona
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss());

        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDialogAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configura il contenuto del dialog
        setupDialogContent(view);

        // Configura la legenda dei turni
        setupShiftsLegend(view);
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Naviga indietro quando il dialog viene chiuso
        if (getActivity() != null) {
            try {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                navController.popBackStack();
            } catch (IllegalStateException e) {
                // Fallback se il NavController non è disponibile
                getActivity().getOnBackPressedDispatcher();
            }
        }
    }

    private void setupDialogContent(View view) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle(getString(R.string.app_name));
        }
    }

    /**
     * Configura la legenda con i colori dei turni dinamicamente.
     */
    private void setupShiftsLegend(View view) {
        LinearLayout legendContainer = view.findViewById(R.id.ll_legend_container);
        if (legendContainer == null) return;

        try {
            // Ottieni i tipi di turno configurati
            List<ShiftType> shiftTypes;
            if (getContext() != null) {
                shiftTypes = ShiftTypeFactory.loadAllShiftTypes(getContext());
            } else {
                // Fallback ai turni standard
                shiftTypes = List.of(
                        ShiftTypeFactory.MORNING,
                        ShiftTypeFactory.AFTERNOON,
                        ShiftTypeFactory.NIGHT
                );
            }

            // Aggiungi un indicatore per ogni turno
            for (int i = 0; i < shiftTypes.size(); i++) {
                ShiftType shiftType = shiftTypes.get(i);
                addShiftLegendItem(legendContainer, shiftType);
            }

        } catch (Exception e) {
            // In caso di errore, mostra solo i turni base
            addShiftLegendItem(legendContainer, ShiftTypeFactory.MORNING);
            addShiftLegendItem(legendContainer, ShiftTypeFactory.AFTERNOON);
            addShiftLegendItem(legendContainer, ShiftTypeFactory.NIGHT);
        }
    }

    /**
     * Aggiunge un elemento alla legenda per un tipo di turno.
     */
    private void addShiftLegendItem(LinearLayout container, ShiftType shiftType) {
        if (getContext() == null) return;

        // Crea il layout orizzontale per l'elemento
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(4));
        itemLayout.setLayoutParams(itemParams);

        // Crea l'indicatore colorato
        View colorIndicator = new View(getContext());
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                dpToPx(16), dpToPx(16)
        );
        indicatorParams.setMargins(0, 0, dpToPx(8), 0);
        colorIndicator.setLayoutParams(indicatorParams);

        // Crea il drawable con il colore del turno
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(shiftType.getColor());
        drawable.setCornerRadius(dpToPx(2));
        colorIndicator.setBackground(drawable);

        // Crea la TextView per il nome
        TextView textView = new TextView(getContext());
        textView.setText(shiftType.getName() + " (" +
                shiftType.getFormattedStartTime() + "-" +
                shiftType.getFormattedEndTime() + ")");
        textView.setTextColor(getResources().getColor(R.color.surface, getContext().getTheme()));
        textView.setTextSize(12);

        // Aggiungi gli elementi al layout
        itemLayout.addView(colorIndicator);
        itemLayout.addView(textView);

        // Aggiungi al container
        container.addView(itemLayout);
    }

    /**
     * Converte dp in pixel.
     */
    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}