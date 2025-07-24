/**
 * Material Design 3 implementation of SelectionActionStyleProvider
 * Provides consistent Material styling for action buttons
 */
package net.calvuz.qdue.ui.core.components.selection.styling;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.components.selection.model.SelectionAction;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Material Design 3 style provider for selection action buttons
 */
@Singleton
public class MaterialSelectionActionStyleProvider implements SelectionActionStyleProvider {

    // Size constants (dp)
    private static final int BUTTON_SIZE_DP = 48;
    private static final int ICON_SIZE_DP = 24;
    private static final int BUTTON_SPACING_DP = 8;
    private static final int CORNER_RADIUS_DP = 12;

    private final Context mApplicationContext;

    @Inject
    public MaterialSelectionActionStyleProvider(@NonNull Context applicationContext) {
        mApplicationContext = applicationContext.getApplicationContext();
    }

    @NonNull
    @Override
    public MaterialButton createActionButton(@NonNull Context context, @NonNull ViewGroup parent) {
        MaterialButton button = new MaterialButton(context);

        // Set layout parameters
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );

        int spacing = getButtonSpacing(context);
        layoutParams.setMargins(spacing / 2, 0, spacing / 2, 0);
        button.setLayoutParams(layoutParams);

        // Base styling
        button.setCornerRadius(dpToPx(context, CORNER_RADIUS_DP));
        button.setIconSize(getIconSize(context));
        button.setMinHeight(getButtonSize(context));
        button.setMinimumHeight(getButtonSize(context));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setInsetTop(0);
        button.setInsetBottom(0);

        // Remove text for icon-only buttons
        button.setText("");
        button.setIconPadding(0);

        return button;
    }

    @Override
    public <T> void styleActionButton(@NonNull MaterialButton button, @NonNull SelectionAction<T> action) {
        Context context = button.getContext();

        // Set icon
        button.setIcon(ContextCompat.getDrawable(context, action.getIconResource()));

        // Apply colors based on action category
        applyActionColors(context, button, action.getCategory());

        // Set elevation
        button.setElevation(0f); // Flat style for toolbar buttons
    }

    /**
     * Apply colors based on action category
     */
    private void applyActionColors(@NonNull Context context,
                                   @NonNull MaterialButton button,
                                   @NonNull SelectionAction.ActionCategory category) {

        ColorStateList backgroundTint;
        ColorStateList iconTint;
        ColorStateList rippleColor;

        switch (category) {
            case PRIMARY:
                // Primary actions use accent color
                backgroundTint = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.floatingMenuPrimary)
                );
                iconTint = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.white)
                );
                rippleColor = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected)
                );
                break;

            case SPECIAL:
                // Special actions use purple
                backgroundTint = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.purple_500)
                );
                iconTint = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.white)
                );
                rippleColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.purple_300)
                );
                break;

            case DESTRUCTIVE:
                // Destructive actions use error color
                backgroundTint = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.colorError)
                );
                iconTint = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.colorOnError)
                );
                rippleColor = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.colorErrorContainer)
                );
                break;

            case SECONDARY:
            default:
                // Secondary actions use surface variant
                backgroundTint = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.floatingMenuSurface)
                );
                iconTint = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.floatingMenuOnSurface)
                );
                rippleColor = ColorStateList.valueOf(
                        Library.getColorByThemeAttr(context, R.attr.floatingMenuSelected)
                );
                break;
        }

        // Apply colors
        button.setBackgroundTintList(backgroundTint);
        button.setIconTint(iconTint);
        button.setRippleColor(rippleColor);
    }

    @Override
    public int getButtonSize(@NonNull Context context) {
        return dpToPx(context, BUTTON_SIZE_DP);
    }

    @Override
    public int getIconSize(@NonNull Context context) {
        return dpToPx(context, ICON_SIZE_DP);
    }

    @Override
    public int getButtonSpacing(@NonNull Context context) {
        return dpToPx(context, BUTTON_SPACING_DP);
    }

    /**
     * Convert dp to pixels
     */
    private int dpToPx(@NonNull Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}