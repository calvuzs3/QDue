package net.calvuz.qdue.events;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.calvuz.qdue.events.models.EventPriority;
import net.calvuz.qdue.events.models.EventType;
import net.calvuz.qdue.events.models.LocalEvent;
import net.calvuz.qdue.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager; /**
 * Manages external event packages with SSL validation
 */
public class EventPackageManager {

    private static final String TAG = "EventPackageManager";

    private final Context mContext;
    private final SharedPreferences mPreferences;
//    private final EventDao mEventDao; // Your database DAO
    private final Gson mGson;

    // SSL Configuration
    private static final int CONNECT_TIMEOUT = 15000; // 15 seconds
    private static final int READ_TIMEOUT = 30000;    // 30 seconds


    // ==================== 4. UPDATED PACKAGE MANAGER CONSTRUCTOR ====================

    /**
     * FIXED: EventPackageManager constructor to use MockEventDao
     * Replace the existing constructor in EventPackageManager
     */

    // And update the member variable declaration:
    private final MockEventDao mEventDao; // Change from EventDao to MockEventDao

// In EventPackageManager.java, replace constructor with:
    public EventPackageManager(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mEventDao = new MockEventDao(); // Use mock DAO for now
        mGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
    }



//    public EventPackageManager(Context context, EventDao eventDao) {
//        mContext = context;
//        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        mEventDao = eventDao;
//        mGson = new GsonBuilder()
//                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
//                .create();
//    }

    /**
     * Manual update from external URL with SSL validation
     */
    public void updateFromUrl(String url, String packageId, UpdateCallback callback) {
        if (TextUtils.isEmpty(url)) {
            callback.onError("URL non valido");
            return;
        }

        // Validate SSL setting
        boolean sslValidation = mPreferences.getBoolean("events_ssl_validation", true);

        // Execute in background thread
        new AsyncTask<Void, Void, UpdateResult>() {
            @Override
            protected UpdateResult doInBackground(Void... voids) {
                try {
                    return downloadAndParsePackage(url, packageId, sslValidation);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating from URL: " + e.getMessage(), e);
                    return new UpdateResult(false, "Errore di connessione: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(UpdateResult result) {
                if (result.success) {
                    callback.onSuccess(result.message);
                    updateLastUpdateInfo(packageId);
                } else {
                    callback.onError(result.message);
                }
            }
        }.execute();
    }

    /**
     * Download and parse package with SSL validation
     */
    private UpdateResult downloadAndParsePackage(String url, String packageId, boolean sslValidation)
            throws IOException, JsonSyntaxException {

        HttpsURLConnection connection = null;
        try {
            URL urlObj = new URL(url);

            // Only allow HTTPS for external sources
            if (!"https".equals(urlObj.getProtocol())) {
                return new UpdateResult(false, "Solo connessioni HTTPS sono supportate");
            }

            connection = (HttpsURLConnection) urlObj.openConnection();

            // Configure SSL validation
            if (sslValidation) {
                // Use default SSL validation
                connection.setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
                connection.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
            } else {
                // WARNING: This disables SSL validation - only for testing!
                Log.w(TAG, "SSL validation disabled - this is not secure!");
                trustAllCertificates(connection);
            }

            // Configure connection
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "QDue-Events/1.0");
            connection.setRequestProperty("Accept", "application/json");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return new UpdateResult(false, "Errore HTTP: " + responseCode);
            }

            // Read response
            String jsonResponse = readResponse(connection);

            // Parse JSON
            EventPackageJson packageJson = mGson.fromJson(jsonResponse, EventPackageJson.class);

            // Validate package
            if (packageJson.package_info == null || packageJson.events == null) {
                return new UpdateResult(false, "Formato package non valido");
            }

            // Validate package ID match
            if (!TextUtils.isEmpty(packageId) &&
                    !packageId.equals(packageJson.package_info.id)) {
                return new UpdateResult(false,
                        "Package ID non corrispondente: atteso " + packageId +
                                ", ricevuto " + packageJson.package_info.id);
            }

            // Import events
            int importedCount = importEventsFromPackage(packageJson, url);

            return new UpdateResult(true,
                    "Aggiornamento completato: " + importedCount + " eventi importati");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Read HTTP response as string
     */
    private String readResponse(HttpsURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Import events from parsed package
     */
    private int importEventsFromPackage(EventPackageJson packageJson, String sourceUrl) {
        int importedCount = 0;
        String packageId = packageJson.package_info.id;
        String packageVersion = packageJson.package_info.version;

        // Remove existing events from this package
        mEventDao.deleteEventsByPackageId(packageId);

        for (EventPackageJson.EventJson eventJson : packageJson.events) {
            try {
                LocalEvent event = convertJsonToEvent(eventJson, packageId, packageVersion, sourceUrl);
                mEventDao.insertEvent(event);
                importedCount++;
            } catch (Exception e) {
                Log.w(TAG, "Error importing event: " + eventJson.title + " Exception: " + e.getMessage());
            }
        }

        return importedCount;
    }

    /**
     * Convert JSON event to LocalEvent
     */
    private LocalEvent convertJsonToEvent(EventPackageJson.EventJson eventJson,
                                          String packageId, String packageVersion, String sourceUrl) {
        LocalEvent event = new LocalEvent();

        // Basic info
        event.setId(packageId + "_" + eventJson.id); // Ensure unique ID
        event.setTitle(eventJson.title);
        event.setDescription(eventJson.description);
        event.setLocation(eventJson.location);
        event.setAllDay(eventJson.all_day);

        // Dates and times
        LocalDate startDate = LocalDate.parse(eventJson.start_date);
        LocalDate endDate = eventJson.end_date != null ?
                LocalDate.parse(eventJson.end_date) : startDate;

        if (eventJson.all_day) {
            event.setStartTime(startDate.atStartOfDay());
            event.setEndTime(endDate.atTime(23, 59));
        } else {
            LocalTime startTime = eventJson.start_time != null ?
                    LocalTime.parse(eventJson.start_time) : LocalTime.of(9, 0);
            LocalTime endTime = eventJson.end_time != null ?
                    LocalTime.parse(eventJson.end_time) : startTime.plusHours(1);

            event.setStartTime(startDate.atTime(startTime));
            event.setEndTime(endDate.atTime(endTime));
        }

        // LocalEvent type and priority
        try {
            event.setEventType(EventType.valueOf(eventJson.event_type));
        } catch (IllegalArgumentException e) {
            event.setEventType(EventType.IMPORTED);
        }

        try {
            event.setPriority(EventPriority.valueOf(eventJson.priority));
        } catch (IllegalArgumentException e) {
            event.setPriority(EventPriority.NORMAL);
        }

        // Package tracking info
        event.setPackageId(packageId);
        event.setPackageVersion(packageVersion);
        event.setSourceUrl(sourceUrl);
        event.setLastUpdated(LocalDateTime.now());

        // Custom properties
        if (eventJson.custom_properties != null) {
            event.setCustomProperties(new HashMap<>(eventJson.custom_properties));
        }

        // Add tags as custom property
        if (eventJson.tags != null && !eventJson.tags.isEmpty()) {
            event.getCustomProperties().put("tags", String.join(",", eventJson.tags));
        }

        return event;
    }

    /**
     * Update last update info in preferences
     */
    private void updateLastUpdateInfo(String packageId) {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        mPreferences.edit()
                .putString("events_last_update_time", timestamp)
                .putString("events_last_update_package", packageId)
                .apply();
    }

    /**
     * WARNING: Disable SSL validation (only for testing!)
     */
    @SuppressLint("TrustAllX509TrustManager")
    private void trustAllCertificates(HttpsURLConnection connection) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up trust all certificates", e);
        }
    }

    // ==================== INTERFACES AND CALLBACKS ====================

    public interface UpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public static class UpdateResult {
        public final boolean success;
        public final String message;

        public UpdateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
