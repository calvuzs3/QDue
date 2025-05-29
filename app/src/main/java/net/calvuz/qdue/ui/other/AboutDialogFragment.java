package net.calvuz.qdue.ui.other;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import net.calvuz.qdue.R;
import net.calvuz.qdue.databinding.FragmentDialogAboutBinding;

public class AboutDialogFragment extends DialogFragment {

    private FragmentDialogAboutBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Infla il layout
        View view = getLayoutInflater().inflate(R.layout.fragment_dialog_about, null);

        // Configura il contenuto del dialog
        setupDialogContent(view);

        // Crea l'AlertDialog con titolo e icona
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(R.string.app_name) // o "About" o il tuo string resource
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss());

        return builder.create();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Imposta lo stile del dialog se necessario
//        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog);
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
                getActivity().getOnBackPressedDispatcher();  //.onBackPressed();
            }
        }
    }

    private void setupDialogContent(View view) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle(getString(R.string.app_name));
        }
    }
}