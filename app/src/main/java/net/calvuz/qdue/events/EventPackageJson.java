package net.calvuz.qdue.events;

/**
 * STEP 2: JSON Package Format and SSL Validation System
 *
 * Simple JSON format for event packages with SSL validation
 * and manual update functionality
 */

// ==================== 1. JSON PACKAGE FORMAT ====================

/**
 * JSON Schema for event packages
 *
 * Example package structure:
 * {
 *   "package_info": {
 *     "id": "company_events_2025",
 *     "name": "Eventi Aziendali 2025",
 *     "version": "1.2.0",
 *     "description": "Pacchetto eventi aziendali aggiornato",
 *     "created_date": "2025-01-15T10:30:00Z",
 *     "valid_from": "2025-01-01",
 *     "valid_to": "2025-12-31",
 *     "author": "HR Department",
 *     "contact_email": "hr@company.com"
 *   },
 *   "events": [
 *     {
 *       "id": "evt_001",
 *       "title": "Fermata Programmata Linea A",
 *       "description": "Manutenzione ordinaria programmata",
 *       "start_date": "2025-03-15",
 *       "end_date": "2025-03-17",
 *       "start_time": "08:00",
 *       "end_time": "17:00",
 *       "all_day": false,
 *       "event_type": "STOP_PLANNED",
 *       "priority": "HIGH",
 *       "location": "Stabilimento Nord - Linea A",
 *       "tags": ["manutenzione", "linea_a", "programmata"],
 *       "custom_properties": {
 *         "department": "Produzione",
 *         "affected_teams": ["A", "B"],
 *         "cost_center": "PROD_001"
 *       }
 *     }
 *   ]
 * }
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

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
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Data classes for JSON package parsing
 */
public class EventPackageJson {
    public PackageInfo package_info;
    public List<EventJson> events;

    public static class PackageInfo {
        public String id;
        public String name;
        public String version;
        public String description;
        public String created_date;
        public String valid_from;
        public String valid_to;
        public String author;
        public String contact_email;
    }

    public static class EventJson {
        public String id;
        public String title;
        public String description;
        public String start_date;
        public String end_date;
        public String start_time;
        public String end_time;
        public boolean all_day = false;
        public String event_type = "GENERAL";
        public String priority = "NORMAL";
        public String location;
        public List<String> tags;
        public Map<String, String> custom_properties;
    }
}

// ==================== 2. PACKAGE MANAGER ====================

