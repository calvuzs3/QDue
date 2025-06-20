package net.calvuz.qdue.ui.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import net.calvuz.qdue.R;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.events.data.database.EventsDatabase;
import net.calvuz.qdue.events.EventDao;
import net.calvuz.qdue.utils.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * EventDetailFragment - Display full screen event details
 *
 * Features:
 * - Google Calendar-like event display
 * - Smart custom properties mapping
 * - Toolbar with Android standard icons
 * - Status calculation (Past/Current/Upcoming)
 * - Duration formatting
 * - Action handling (edit, share, calendar integration)
 *
 * Usage:
 * Bundle args = new Bundle();
 * args.putString(ARG_EVENT_ID, eventId);
 * fragment.setArguments(args);
 */
public class EventDetailFragment extends Fragment {

    private static final String TAG = "EventDetailFragment";
    private static final String ARG_EVENT_ID = "event_id";

    // Date formatters for display
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Smart mapping for custom properties
    private static final String[][] PROPERTY_MAPPINGS = {
            {"department", "Dipartimento", "business"},
            {"cost_center", "Centro di Costo", "account_balance"},
            {"affected_teams", "Team Coinvolti", "group"},
            {"responsible", "Responsabile", "person"},
            {"contact_email", "Email", "email"},
            {"phone", "Telefono", "phone"},
            {"impact_level", "Livello Impatto", "warning"},
            {"responsible_person", "Responsabile", "person"},
            {"teams", "Team", "group"},
            {"department_code", "Codice Dipartimento", "business"},
            {"manager", "Manager", "person"},
            {"contact", "Contatto", "person"}
    };

    // View references
    private MaterialToolbar mToolbar;
    private TextView mTitleView;
    private TextView mDescriptionView;
    private TextView mStartTimeView;
    private TextView mEndTimeView;
    private TextView mDurationView;
    private TextView mAllDayView;
    private TextView mLocationView;
    private TextView mEventTypeView;
    private TextView mPriorityView;
    private TextView mEventStatusView;
    private TextView mTimeUntilView;
    private ImageView mStatusIconView;

    // Custom properties views
    private LinearLayout mDepartmentLayout;
    private TextView mDepartmentView;
    private LinearLayout mCostCenterLayout;
    private TextView mCostCenterView;
    private LinearLayout mAffectedTeamsLayout;
    private TextView mAffectedTeamsView;
    private LinearLayout mResponsibleLayout;
    private TextView mResponsibleView;
    private LinearLayout mContactEmailLayout;
    private TextView mContactEmailView;
    private LinearLayout mPhoneLayout;
    private TextView mPhoneView;
    private LinearLayout mImpactLevelLayout;
    private TextView mImpactLevelView;
    private LinearLayout mAdditionalPropertiesLayout;

    // Card views for hiding sections
    private MaterialCardView mCustomPropertiesCard;
    private LinearLayout mLocationLayout;
    private LinearLayout mTimeUntilLayout;

    // Data
    private LocalEvent mEvent;
    private String mEventId;

    /**
     * Factory method to create new instance
     */
    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mEventId = getArguments().getString("eventId");
            Log.d(TAG, "Received eventId: " + mEventId);
        } else {
            Log.e(TAG, "No arguments received");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupToolbar();
        loadEventData();
    }

    /**
     * Initialize all view references
     */
    private void initializeViews(View view) {
        // Toolbar removed - handled by activity

        // Core event info
        mTitleView = view.findViewById(R.id.tv_event_title);
        mDescriptionView = view.findViewById(R.id.tv_event_description);
        mStartTimeView = view.findViewById(R.id.tv_start_time);
        mEndTimeView = view.findViewById(R.id.tv_end_time);
        mDurationView = view.findViewById(R.id.tv_duration);
        mAllDayView = view.findViewById(R.id.tv_all_day);
        mLocationView = view.findViewById(R.id.tv_location);
        mLocationLayout = view.findViewById(R.id.layout_location);

        // Categorization
        mEventTypeView = view.findViewById(R.id.tv_event_type);
        mPriorityView = view.findViewById(R.id.tv_priority);

        // Status
        mEventStatusView = view.findViewById(R.id.tv_event_status);
        mTimeUntilView = view.findViewById(R.id.tv_time_until);
        mStatusIconView = view.findViewById(R.id.iv_status_icon);
        mTimeUntilLayout = view.findViewById(R.id.layout_time_until);

        // Custom properties
        mCustomPropertiesCard = view.findViewById(R.id.card_custom_properties);
        mDepartmentLayout = view.findViewById(R.id.layout_department);
        mDepartmentView = view.findViewById(R.id.tv_department);
        mCostCenterLayout = view.findViewById(R.id.layout_cost_center);
        mCostCenterView = view.findViewById(R.id.tv_cost_center);
        mAffectedTeamsLayout = view.findViewById(R.id.layout_affected_teams);
        mAffectedTeamsView = view.findViewById(R.id.tv_affected_teams);
        mResponsibleLayout = view.findViewById(R.id.layout_responsible);
        mResponsibleView = view.findViewById(R.id.tv_responsible);
        mContactEmailLayout = view.findViewById(R.id.layout_contact_email);
        mContactEmailView = view.findViewById(R.id.tv_contact_email);
        mPhoneLayout = view.findViewById(R.id.layout_phone);
        mPhoneView = view.findViewById(R.id.tv_phone);
        mImpactLevelLayout = view.findViewById(R.id.layout_impact_level);
        mImpactLevelView = view.findViewById(R.id.tv_impact_level);
        mAdditionalPropertiesLayout = view.findViewById(R.id.layout_additional_properties);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_event_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return handleMenuItemClick(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Setup toolbar with menu and navigation
     */
    private void setupToolbar() {
        // Remove the fragment's own toolbar setup since EventsActivity handles it
        // Set the menu on the activity's toolbar instead
        setHasOptionsMenu(true);

        // The navigation will be handled by EventsActivity's toolbar
        Log.d(TAG, "Toolbar setup completed - using activity toolbar");
    }

    /**
     * Handle toolbar menu item clicks
     */
    private boolean handleMenuItemClick(MenuItem item) {
        if (mEvent == null) return false;

        int itemId = item.getItemId();

        if (itemId == R.id.action_edit_event) {
            handleEditEvent();
            return true;
        } else if (itemId == R.id.action_share_event) {
            handleShareEvent();
            return true;
        } else if (itemId == R.id.action_add_to_calendar) {
            handleAddToCalendar();
            return true;
        } else if (itemId == R.id.action_delete_event) {
            handleDeleteEvent();
            return true;
        } else if (itemId == R.id.action_copy_details) {
            handleCopyDetails();
            return true;
        } else if (itemId == R.id.action_view_source) {
            handleViewSource();
            return true;
        } else if (itemId == R.id.action_export_event) {
            handleExportEvent();
            return true;
        }

        return false;
    }

    /**
     * Load event data and populate views
     */
    private void loadEventData() {
        if (TextUtils.isEmpty(mEventId)) {
            Log.e(TAG, "No event ID provided");
            showError("Evento non trovato");
            return;
        }

        Log.d(TAG, "Loading event with ID: " + mEventId);

        try {
            // Get database instance and DAO
            EventDao eventDao = EventsDatabase.getInstance(requireContext()).eventDao();

            // Load event asynchronously
            new Thread(() -> {
                try {
                    LocalEvent event = eventDao.getEventById(mEventId);

                    // Update UI on main thread
                    requireActivity().runOnUiThread(() -> {
                        if (event != null) {
                            mEvent = event;
                            populateEventDetails();
                            Log.d(TAG, "Successfully loaded event: " + event.getTitle());
                        } else {
                            Log.w(TAG, "Event not found in database, using sample data");
                            // Fallback to sample event for testing
                            mEvent = createSampleEvent();
                            populateEventDetails();
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error loading event from database: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        // Fallback to sample event
                        mEvent = createSampleEvent();
                        if (mEvent != null) {
                            populateEventDetails();
                        } else {
                            showError("Errore caricamento evento dal database");
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error getting database instance: " + e.getMessage());

            // Fallback to sample event
            mEvent = createSampleEvent();
            if (mEvent != null) {
                populateEventDetails();
            } else {
                showError("Errore accesso database");
            }
        }
    }

    /**
     * Populate all view fields with event data
     */
    private void populateEventDetails() {
        // Core event info
        mTitleView.setText(mEvent.getTitle());

        if (!TextUtils.isEmpty(mEvent.getDescription())) {
            mDescriptionView.setText(mEvent.getDescription());
            mDescriptionView.setVisibility(View.VISIBLE);
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }

        // Time information
        populateTimeInfo();

        // Location
        populateLocationInfo();

        // Categorization
        populateCategorization();

        // Status
        populateStatusInfo();

        // Custom properties
        populateCustomProperties();
    }

    /**
     * Populate time-related information
     */
    private void populateTimeInfo() {
        if (mEvent.getStartTime() != null) {
            if (mEvent.isAllDay()) {
                mStartTimeView.setText(mEvent.getStartTime().format(DATE_FORMATTER));
                mAllDayView.setVisibility(View.VISIBLE);
            } else {
                mStartTimeView.setText(mEvent.getStartTime().format(DATE_TIME_FORMATTER));
                mAllDayView.setVisibility(View.GONE);
            }
        }

        if (mEvent.getEndTime() != null) {
            if (mEvent.isAllDay()) {
                mEndTimeView.setText(mEvent.getEndTime().format(DATE_FORMATTER));
            } else {
                mEndTimeView.setText(mEvent.getEndTime().format(DATE_TIME_FORMATTER));
            }
        }

        // Calculate and display duration
        if (mEvent.getStartTime() != null && mEvent.getEndTime() != null) {
            String duration = calculateDuration(mEvent.getStartTime(), mEvent.getEndTime());
            mDurationView.setText(duration);
        }
    }

    /**
     * Populate location information
     */
    private void populateLocationInfo() {
        if (!TextUtils.isEmpty(mEvent.getLocation())) {
            mLocationView.setText(mEvent.getLocation());
            mLocationLayout.setVisibility(View.VISIBLE);
        } else {
            mLocationLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Populate categorization information
     */
    private void populateCategorization() {
        // Event type
        if (mEvent.getEventType() != null) {
            mEventTypeView.setText(formatEventType(mEvent.getEventType().toString()));
        }

        // Priority
        if (mEvent.getPriority() != null) {
            mPriorityView.setText(formatPriority(mEvent.getPriority().toString()));
        }
    }

    /**
     * Populate status information
     */
    private void populateStatusInfo() {
        LocalDateTime now = LocalDateTime.now();
        EventStatus status = calculateEventStatus(now);

        // Set status text and icon
        mEventStatusView.setText(getStatusText(status));
        mStatusIconView.setImageResource(getStatusIcon(status));

        // Calculate time until event
        if (status == EventStatus.UPCOMING && mEvent.getStartTime() != null) {
            String timeUntil = calculateTimeUntil(now, mEvent.getStartTime());
            mTimeUntilView.setText(timeUntil);
            mTimeUntilLayout.setVisibility(View.VISIBLE);
        } else if (status == EventStatus.CURRENT && mEvent.getEndTime() != null) {
            String timeRemaining = calculateTimeUntil(now, mEvent.getEndTime());
            mTimeUntilView.setText("Termina " + timeRemaining);
            mTimeUntilLayout.setVisibility(View.VISIBLE);
        } else {
            mTimeUntilLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Populate custom properties with smart mapping
     */
    private void populateCustomProperties() {
        Map<String, String> customProps = mEvent.getCustomProperties();

        if (customProps == null || customProps.isEmpty()) {
            mCustomPropertiesCard.setVisibility(View.GONE);
            return;
        }

        boolean hasVisibleProperties = false;

        // Process known properties with smart mapping
        for (String[] mapping : PROPERTY_MAPPINGS) {
            String key = mapping[0];
            String displayName = mapping[1];

            if (customProps.containsKey(key)) {
                String value = customProps.get(key);
                if (!TextUtils.isEmpty(value)) {
                    setCustomPropertyValue(key, value);
                    hasVisibleProperties = true;
                }
            }
        }

        // Process unknown properties
        mAdditionalPropertiesLayout.removeAllViews();
        for (Map.Entry<String, String> entry : customProps.entrySet()) {
            if (!isKnownProperty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                addUnknownProperty(entry.getKey(), entry.getValue());
                hasVisibleProperties = true;
            }
        }

        // Show/hide the entire custom properties card
        mCustomPropertiesCard.setVisibility(hasVisibleProperties ? View.VISIBLE : View.GONE);
    }

    /**
     * Set value for known custom property
     */
    private void setCustomPropertyValue(String key, String value) {
        switch (key) {
            case "department":
            case "department_code":
                mDepartmentView.setText(value);
                mDepartmentLayout.setVisibility(View.VISIBLE);
                break;
            case "cost_center":
                mCostCenterView.setText(value);
                mCostCenterLayout.setVisibility(View.VISIBLE);
                break;
            case "affected_teams":
            case "teams":
                mAffectedTeamsView.setText(value);
                mAffectedTeamsLayout.setVisibility(View.VISIBLE);
                break;
            case "responsible":
            case "responsible_person":
            case "manager":
                mResponsibleView.setText(value);
                mResponsibleLayout.setVisibility(View.VISIBLE);
                break;
            case "contact_email":
                mContactEmailView.setText(value);
                mContactEmailLayout.setVisibility(View.VISIBLE);
                break;
            case "phone":
                mPhoneView.setText(value);
                mPhoneLayout.setVisibility(View.VISIBLE);
                break;
            case "impact_level":
                mImpactLevelView.setText(value);
                mImpactLevelLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Add unknown property to additional properties layout
     */
    private void addUnknownProperty(String key, String value) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View propertyView = inflater.inflate(android.R.layout.simple_list_item_2,
                mAdditionalPropertiesLayout, false);

        TextView keyView = propertyView.findViewById(android.R.id.text1);
        TextView valueView = propertyView.findViewById(android.R.id.text2);

        keyView.setText(formatPropertyKey(key));
        valueView.setText(value);

        mAdditionalPropertiesLayout.addView(propertyView);
        mAdditionalPropertiesLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Check if property key is known/mapped
     */
    private boolean isKnownProperty(String key) {
        for (String[] mapping : PROPERTY_MAPPINGS) {
            if (mapping[0].equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format property key for display
     */
    private String formatPropertyKey(String key) {
        return key.replace("_", " ")
                .toLowerCase()
                .replaceAll("\\b\\w", String.valueOf(key.charAt(0)).toUpperCase());
    }

    /**
     * Calculate event status based on current time
     */
    private EventStatus calculateEventStatus(LocalDateTime now) {
        if (mEvent.getStartTime() == null || mEvent.getEndTime() == null) {
            return EventStatus.UNKNOWN;
        }

        if (now.isBefore(mEvent.getStartTime())) {
            return EventStatus.UPCOMING;
        } else if (now.isAfter(mEvent.getEndTime())) {
            return EventStatus.PAST;
        } else {
            return EventStatus.CURRENT;
        }
    }

    /**
     * Get status text for display
     */
    private String getStatusText(EventStatus status) {
        switch (status) {
            case UPCOMING: return "In Programma";
            case CURRENT: return "In Corso";
            case PAST: return "Terminato";
            default: return "Sconosciuto";
        }
    }

    /**
     * Get status icon resource
     */
    private int getStatusIcon(EventStatus status) {
        switch (status) {
            case UPCOMING: return android.R.drawable.ic_menu_recent_history;
            case CURRENT: return android.R.drawable.ic_media_play;
            case PAST: return android.R.drawable.ic_menu_agenda;
            default: return android.R.drawable.ic_dialog_info;
        }
    }

    /**
     * Calculate duration between two LocalDateTime objects
     */
    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        long totalMinutes = ChronoUnit.MINUTES.between(start, end);

        if (totalMinutes < 60) {
            return totalMinutes + " minuti";
        } else if (totalMinutes < 1440) { // Less than 24 hours
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (minutes == 0) {
                return hours + (hours == 1 ? " ora" : " ore");
            } else {
                return hours + (hours == 1 ? " ora " : " ore ") + minutes + " min";
            }
        } else { // Multiple days
            long days = totalMinutes / 1440;
            long remainingMinutes = totalMinutes % 1440;
            long hours = remainingMinutes / 60;

            StringBuilder result = new StringBuilder();
            result.append(days).append(days == 1 ? " giorno" : " giorni");
            if (hours > 0) {
                result.append(" ").append(hours).append(hours == 1 ? " ora" : " ore");
            }
            return result.toString();
        }
    }

    /**
     * Calculate time until a future event
     */
    private String calculateTimeUntil(LocalDateTime now, LocalDateTime eventTime) {
        long totalMinutes = ChronoUnit.MINUTES.between(now, eventTime);

        if (totalMinutes < 60) {
            return "fra " + totalMinutes + " minuti";
        } else if (totalMinutes < 1440) {
            long hours = totalMinutes / 60;
            return "fra " + hours + (hours == 1 ? " ora" : " ore");
        } else {
            long days = totalMinutes / 1440;
            long hours = (totalMinutes % 1440) / 60;

            if (hours == 0) {
                return "fra " + days + (days == 1 ? " giorno" : " giorni");
            } else {
                return "fra " + days + (days == 1 ? " giorno " : " giorni ") +
                        hours + (hours == 1 ? " ora" : " ore");
            }
        }
    }

    /**
     * Format event type for display
     */
    private String formatEventType(String eventType) {
        switch (eventType.toUpperCase()) {
            case "STOP_PLANNED": return "Fermata Programmata";
            case "STOP_EMERGENCY": return "Fermata Emergenza";
            case "MAINTENANCE": return "Manutenzione";
            case "MEETING": return "Riunione";
            case "GENERAL": return "Generale";
            default: return eventType.replace("_", " ");
        }
    }

    /**
     * Format priority for display
     */
    private String formatPriority(String priority) {
        switch (priority.toUpperCase()) {
            case "HIGH": return "Alta";
            case "NORMAL": return "Normale";
            case "LOW": return "Bassa";
            default: return priority;
        }
    }

    // ==================== ACTION HANDLERS ====================

    private void handleEditEvent() {
        if (mEvent == null || mEvent.getId() == null) {
            Toast.makeText(getContext(), "Errore: evento non valido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to EventEditFragment
        Bundle args = new Bundle();
        args.putString("eventId", mEvent.getId());

        try {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_event_detail_to_edit, args);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to edit: " + e.getMessage());
            Toast.makeText(getContext(), "Errore navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleShareEvent() {
        if (mEvent == null) return;

        String shareText = createShareText();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Evento: " + mEvent.getTitle());

        startActivity(Intent.createChooser(shareIntent, "Condividi Evento"));
    }

    private void handleAddToCalendar() {
        if (mEvent == null || mEvent.getStartTime() == null) return;

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, mEvent.getTitle());
        intent.putExtra(CalendarContract.Events.DESCRIPTION, mEvent.getDescription());
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, mEvent.getLocation());
        intent.putExtra(CalendarContract.Events.ALL_DAY, mEvent.isAllDay());

        // Convert LocalDateTime to milliseconds
        long startMillis = mEvent.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);

        if (mEvent.getEndTime() != null) {
            long endMillis = mEvent.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Impossibile aprire il calendario", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDeleteEvent() {
        Toast.makeText(getContext(), "Elimina evento - TODO", Toast.LENGTH_SHORT).show();
        // TODO: Show confirmation dialog and delete event
    }

    private void handleCopyDetails() {
        if (mEvent == null) return;

        String details = createShareText();
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Event Details", details);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), "Dettagli copiati negli appunti", Toast.LENGTH_SHORT).show();
    }

    private void handleViewSource() {
        if (mEvent == null || TextUtils.isEmpty(mEvent.getSourceUrl())) {
            Toast.makeText(getContext(), "Nessun URL sorgente disponibile", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.getSourceUrl()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Impossibile aprire l'URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleExportEvent() {
        Toast.makeText(getContext(), "Esporta evento - TODO", Toast.LENGTH_SHORT).show();
        // TODO: Export event to JSON or other format
    }

    /**
     * Create share text from event details
     */
    private String createShareText() {
        StringBuilder text = new StringBuilder();
        text.append(mEvent.getTitle()).append("\n\n");

        if (!TextUtils.isEmpty(mEvent.getDescription())) {
            text.append(mEvent.getDescription()).append("\n\n");
        }

        if (mEvent.getStartTime() != null) {
            text.append("Inizio: ").append(mEvent.getStartTime().format(DATE_TIME_FORMATTER)).append("\n");
        }

        if (mEvent.getEndTime() != null) {
            text.append("Fine: ").append(mEvent.getEndTime().format(DATE_TIME_FORMATTER)).append("\n");
        }

        if (!TextUtils.isEmpty(mEvent.getLocation())) {
            text.append("Luogo: ").append(mEvent.getLocation()).append("\n");
        }

        return text.toString();
    }

    /**
     * Show error message and navigate back
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    // ==================== SAMPLE DATA FOR TESTING ====================

    /**
     * Create sample event for testing purposes
     * TODO: Remove when real database integration is implemented
     */
    private LocalEvent createSampleEvent() {
        LocalEvent event = new LocalEvent();
        event.setId("sample_event_001");
        event.setTitle("Fermata Programmata Linea A");
        event.setDescription("Manutenzione ordinaria programmata per ottimizzazione prestazioni della linea di produzione A. Durante questo periodo verranno eseguiti controlli approfonditi e sostituzioni preventive.");
        event.setStartTime(LocalDateTime.now().plusDays(3).withHour(8).withMinute(0));
        event.setEndTime(LocalDateTime.now().plusDays(5).withHour(17).withMinute(0));
        event.setLocation("Stabilimento Nord - Linea A");
        event.setAllDay(false);

        // Add custom properties for testing smart mapping
        Map<String, String> customProps = new java.util.HashMap<>();
        customProps.put("department", "Produzione");
        customProps.put("cost_center", "PROD_001");
        customProps.put("affected_teams", "A, B, C");
        customProps.put("responsible", "Mario Rossi");
        customProps.put("contact_email", "produzione@company.com");
        customProps.put("impact_level", "Medio");
        customProps.put("custom_field", "Valore personalizzato");
        event.setCustomProperties(customProps);

        event.setPackageId("company_events_2025");
        event.setSourceUrl("https://company.com/events.json");
        event.setPackageVersion("1.2.0");

        return event;
    }

    /**
     * Event status enumeration
     */
    private enum EventStatus {
        UPCOMING, CURRENT, PAST, UNKNOWN
    }
}