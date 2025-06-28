package net.calvuz.qdue.ui.welcome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Introduction Fragment - First step of welcome flow
 * <p></p>
 * Provides overview of QDue app functionality:
 * - Shift management system explanation
 * - Key features preview
 * - Benefits for team coordination
 * - Animated feature showcase
 */
public class IntroductionFragment extends Fragment {

    // Feature item data class
    public static class FeatureItem {
        public final int iconRes;
        public final String title;
        public final String description;
        public final boolean isComingSoon;

        public FeatureItem(int iconRes, String title, String description, boolean isComingSoon) {
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
            this.isComingSoon = isComingSoon;
        }
    }

    // View components
    private RecyclerView featuresRecyclerView;
    private FeatureItemAdapter adapter;
    private TextView titleText;
    private TextView subtitleText;
    private MaterialCardView headerCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_introduction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupFeaturesList();
        startIntroAnimation();
    }

    /**
     * Initialize view components
     */
    private void initializeViews(View view) {
        titleText = view.findViewById(R.id.intro_title);
        subtitleText = view.findViewById(R.id.intro_subtitle);
        headerCard = view.findViewById(R.id.header_card);
        featuresRecyclerView = view.findViewById(R.id.features_recycler_view);

        // Setup RecyclerView
        featuresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        featuresRecyclerView.setHasFixedSize(true);
    }

    /**
     * Setup features list with current and upcoming functionality
     */
    private void setupFeaturesList() {
        List<FeatureItem> features = createFeaturesList();
        adapter = new FeatureItemAdapter(features);
        featuresRecyclerView.setAdapter(adapter);
    }

    /**
     * Create list of app features to showcase
     */
    private List<FeatureItem> createFeaturesList() {
        List<FeatureItem> features = new ArrayList<>();

        // Current features (already implemented)
        features.add(new FeatureItem(
                R.drawable.ic_view_calendar,
                getString(R.string.feature_calendar_view_title),
                getString(R.string.feature_calendar_view_desc),
                false
        ));

        features.add(new FeatureItem(
                R.drawable.ic_view_dayslist,
                getString(R.string.feature_dayslist_view_title),
                getString(R.string.feature_dayslist_view_desc),
                false
        ));

        features.add(new FeatureItem(
                R.drawable.ic_import,
                getString(R.string.feature_import_events_title),
                getString(R.string.feature_import_events_desc),
                false
        ));

        features.add(new FeatureItem(
                R.drawable.ic_rounded_event_24,
                getString(R.string.feature_create_events_title),
                getString(R.string.feature_create_events_desc),
                false
        ));

        features.add(new FeatureItem(
                R.drawable.ic_rounded_edit_calendar_24,
                getString(R.string.feature_manage_events_title),
                getString(R.string.feature_manage_events_desc),
                false
        ));

        features.add(new FeatureItem(
                R.drawable.ic_rounded_share_24,
                getString(R.string.feature_share_events_title),
                getString(R.string.feature_share_events_desc),
                false
        ));

        // Coming soon features
        features.add(new FeatureItem(
                R.drawable.ic_rounded_notifications_24,
                getString(R.string.feature_notifications_title),
                getString(R.string.feature_notifications_desc),
                true
        ));

        features.add(new FeatureItem(
                R.drawable.ic_rounded_cloud_sync_24,
                getString(R.string.feature_cloud_sync_title),
                getString(R.string.feature_cloud_sync_desc),
                true
        ));

        features.add(new FeatureItem(
                R.drawable.ic_rounded_analytics_24,
                getString(R.string.feature_analytics_title),
                getString(R.string.feature_analytics_desc),
                true
        ));

        return features;
    }

    /**
     * Start introduction animation sequence
     */
    private void startIntroAnimation() {
        // Initial state - elements are scaled down and transparent
        titleText.setScaleX(0.8f);
        titleText.setScaleY(0.8f);
        titleText.setAlpha(0f);

        subtitleText.setScaleX(0.8f);
        subtitleText.setScaleY(0.8f);
        subtitleText.setAlpha(0f);

        headerCard.setScaleX(0.9f);
        headerCard.setScaleY(0.9f);
        headerCard.setAlpha(0f);

        featuresRecyclerView.setTranslationY(100f);
        featuresRecyclerView.setAlpha(0f);

        // Animate title
        titleText.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate subtitle (delayed)
        subtitleText.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate header card
        headerCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate features list
        featuresRecyclerView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restart animation if fragment becomes visible
        if (isVisible()) {
            startIntroAnimation();
        }
    }
}