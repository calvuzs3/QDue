package net.calvuz.qdue.ui.welcome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import net.calvuz.qdue.R;
import net.calvuz.qdue.ui.welcome.IntroductionFragment.FeatureItem;

import java.util.List;

/**
 * RecyclerView Adapter for feature items in welcome introduction
 * <p></p>
 * Displays app features with:
 * - Feature icon
 * - Title and description
 * - "Coming Soon" indicator for future features
 * - Material 3 card design with hover effects
 */
public class FeatureItemAdapter extends RecyclerView.Adapter<FeatureItemAdapter.FeatureViewHolder> {

    private final List<FeatureItem> features;

    /**
     * Constructor
     * @param features List of feature items to display
     */
    public FeatureItemAdapter(List<FeatureItem> features) {
        this.features = features;
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_welcome_feature, parent, false);
        return new FeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        FeatureItem feature = features.get(position);
        holder.bind(feature, position);
    }

    @Override
    public int getItemCount() {
        return features.size();
    }

    /**
     * ViewHolder for feature items
     */
    static class FeatureViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final ImageView iconImageView;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final Chip comingSoonChip;

        public FeatureViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.feature_card);
            iconImageView = itemView.findViewById(R.id.feature_icon);
            titleTextView = itemView.findViewById(R.id.feature_title);
            descriptionTextView = itemView.findViewById(R.id.feature_description);
            comingSoonChip = itemView.findViewById(R.id.coming_soon_chip);
        }

        /**
         * Bind feature data to views
         * @param feature Feature item data
         * @param position Item position for animation timing
         */
        public void bind(FeatureItem feature, int position) {
            // Set feature content
            iconImageView.setImageResource(feature.iconRes);
            titleTextView.setText(feature.title);
            descriptionTextView.setText(feature.description);

            // Handle coming soon indicator
            if (feature.isComingSoon) {
                comingSoonChip.setVisibility(View.VISIBLE);
                comingSoonChip.setText(R.string.coming_soon);

                // Slightly different styling for coming soon features
                cardView.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.md_theme_light_surfaceVariant)
                );
                iconImageView.setAlpha(0.7f);
                titleTextView.setAlpha(0.8f);
                descriptionTextView.setAlpha(0.7f);
            } else {
                comingSoonChip.setVisibility(View.GONE);

                // Normal styling for current features
                cardView.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.md_theme_light_surface)
                );
                iconImageView.setAlpha(1f);
                titleTextView.setAlpha(1f);
                descriptionTextView.setAlpha(1f);
            }

            // Add entrance animation with staggered timing
            startEntranceAnimation(position);

            // Add click ripple effect
            setupClickEffect();
        }

        /**
         * Animate item entrance with staggered timing
         * @param position Item position for delay calculation
         */
        private void startEntranceAnimation(int position) {
            // Initial state
            cardView.setTranslationX(-100f);
            cardView.setAlpha(0f);

            // Calculate delay based on position
            long delay = position * 100L; // 100ms between each item

            // Animate to final position
            cardView.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(delay)
                    .start();
        }

        /**
         * Setup click effect for interactive feedback
         */
        private void setupClickEffect() {
            cardView.setOnClickListener(v -> {
                // Scale animation on click
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                        })
                        .start();
            });

            // Add hover effect (for devices that support it)
            cardView.setOnHoverListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_HOVER_ENTER:
                        v.animate().translationZ(8f).setDuration(200).start();
                        break;
                    case android.view.MotionEvent.ACTION_HOVER_EXIT:
                        v.animate().translationZ(2f).setDuration(200).start();
                        break;
                }
                return false;
            });
        }
    }
}