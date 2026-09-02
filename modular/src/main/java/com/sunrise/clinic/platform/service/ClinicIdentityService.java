package com.sunrise.clinic.platform.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.config.AppConfig;
import com.sunrise.clinic.platform.data.ClinicSettingRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Clinic identity — the name, phone, email and address shown on the landing
 * page, help page, receipts and appointment slips.
 *
 * <p>Values are stored in the {@code clinic_setting} table and edited at
 * runtime. When a key has no row in the database, the properties-file
 * default is returned so the application works before the table is seeded.</p>
 *
 * <p>Write access is split by role:</p>
 * <ul>
 *   <li>Administrator — all four fields ({@link Action#MANAGE_CLINIC_SETTINGS},
 *       phone via {@link Action#MANAGE_PHONE})</li>
 * </ul>
 *
 * <p>The reception role no longer edits the phone (GAP-REC-10); that belongs to
 * the administrator's Clinic screen alone.</p>
 */
public class ClinicIdentityService {

    /** The four keys this service manages. */
    public static final Set<String> ALL_KEYS = Set.of(
            "clinic.name", "clinic.phone", "clinic.email", "clinic.address");

    /** Keys a receptionist may edit. */
    public static final Set<String> RECEPTION_KEYS = Set.of("clinic.phone");

    private final ClinicSettingRepository settings;
    private final AppConfig config;

    public ClinicIdentityService(ClinicSettingRepository settings, AppConfig config) {
        this.settings = settings;
        this.config = config;
    }

    /** Read one value — database first, then properties-file fallback. */
    public String get(String key) {
        return settings.get(key).orElse(defaultFor(key));
    }

    /** All four values as a map, for the admin form. */
    public Map<String, String> getAll() {
        Map<String, String> db = settings.getAll();
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : ALL_KEYS) {
            result.put(key, db.getOrDefault(key, defaultFor(key)));
        }
        return result;
    }

    /**
     * Update one setting. The caller's role is checked against the key:
     * receptionists may only change phone; admins may change anything.
     */
    public void update(ClinicPrincipal caller, String key, String value) {
        if (!ALL_KEYS.contains(key)) {
            throw new IllegalArgumentException("Unknown setting: " + key);
        }
        if (RECEPTION_KEYS.contains(key)) {
            AccessControl.require(caller, Action.MANAGE_PHONE);
        } else {
            AccessControl.require(caller, Action.MANAGE_CLINIC_SETTINGS);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label(key) + " cannot be empty.");
        }
        String stored = value.trim();
        if ("clinic.phone".equals(key)) {
            PhoneNumbers.validate(stored, label(key));
        }
        settings.save(key, stored);
    }

    private String defaultFor(String key) {
        return switch (key) {
            case "clinic.name"    -> config.get("clinic.name", "Sunrise Dental Clinic");
            case "clinic.phone"   -> config.get("clinic.phone", "");
            case "clinic.email"   -> config.get("clinic.email", "");
            case "clinic.address" -> config.get("clinic.address", "");
            default -> "";
        };
    }

    private static String label(String key) {
        return switch (key) {
            case "clinic.name"    -> "Clinic name";
            case "clinic.phone"   -> "Phone number";
            case "clinic.email"   -> "Email address";
            case "clinic.address" -> "Address";
            default -> key;
        };
    }
}
