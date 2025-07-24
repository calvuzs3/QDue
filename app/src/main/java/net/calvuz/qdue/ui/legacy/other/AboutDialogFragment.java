package net.calvuz.qdue.ui.legacy.other;

import static net.calvuz.qdue.ui.core.common.utils.Library.getColorByThemeAttr;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
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
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;

import java.text.MessageFormat;
import java.util.List;

public class AboutDialogFragment extends DialogFragment {

    private FragmentDialogAboutBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = FragmentDialogAboutBinding.inflate(getLayoutInflater());
        View view = getLayoutInflater().inflate(R.layout.fragment_dialog_about, null);

        // Configura il contenuto del dialog
        setupDialogContent();

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
        setupDialogContent();

        // Configura la legenda dei turni
        setupShiftsLegend();
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Navigate back to the previous fragment
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

    private void setupDialogContent() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle(getString(R.string.app_name));
        }
    }

    /**
     * Configure legend with dynamic colors for shifts.
     */
    private void setupShiftsLegend() {
        LinearLayout legendContainer = binding.llLegendContainer;

        try {
            // Shift Types
            List<ShiftType> shiftTypes = ShiftTypeFactory.getAllShiftTypes();

            // Add an indicator item for each shift type
            for (int i = 0; i < shiftTypes.size(); i++) {
                ShiftType shiftType = shiftTypes.get(i);
                addShiftLegendItem(legendContainer, shiftType);
            }

        } catch (Exception e) {
            Log.e("AboutDialogFragment", "ShiftType list not acquired");
        }
    }

    /**
     * Add a legend item for a shift type.
     */
    private void addShiftLegendItem(LinearLayout container, ShiftType shiftType) {
        if (getContext() == null) return;

        // Create the horizontal layout for the item
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL | Gravity.END);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, Library.dpToPx(getContext(), 4));
        itemLayout.setLayoutParams(itemParams);

        // Create the TextView for the shift name
        TextView textView = new TextView(getContext());
        textView.setText(MessageFormat.format(" ({0}-{1}) {2}  ",
                shiftType.getFormattedStartTime(),
                shiftType.getFormattedEndTime(),
                shiftType.getName()));
        textView.setTextColor(getColorByThemeAttr(getContext(),
                com.google.android.material.R.attr.colorOnSurface));
        textView.setTextAppearance(android.R.style.TextAppearance_Material_Body2);

        // Create the color indicator
        View colorIndicator = new View(getContext());
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                Library.dpToPx(getContext(), 32) , Library.dpToPx(getContext(), 12)
        );
        indicatorParams.setMargins(0, 0, Library.dpToPx(getContext(), 8), 0);
        colorIndicator.setLayoutParams(indicatorParams);

        // Create the drawable for the color indicator
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(shiftType.getColor());
        drawable.setCornerRadius(Library.dpToPx(getContext(), 2));
        colorIndicator.setBackground(drawable);

        // Add the views to the layout
        itemLayout.addView(textView);
        itemLayout.addView(colorIndicator);

        // Add the item layout to the container
        container.addView(itemLayout);
    }
}