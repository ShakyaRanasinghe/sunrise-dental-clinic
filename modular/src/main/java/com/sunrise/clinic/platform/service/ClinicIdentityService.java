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

    /** Keys this service manages: identity plus pricing (GAP-ADM-10). */
    public static final String DENTIST_SHARE_KEY = "clinic.revenue.dentist-treatment-share";
    public static final String SERVICE_CHARGE_KEY = "clinic.billing.service-charge";

    public static final Set<String> ALL_KEYS = Set.of(
            "clinic.name", "clinic.phone", "clinic.email", "clinic.address",
            DENTIST_SHARE_KEY, SERVICE_CHARGE_KEY);

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

    /**
     * The dentist's fraction of each treatment price (GAP-ADM-10), read live so a
     * Pricing-tab save applies to the next bill without restart. Falls back to the
     * shipped default when the row is absent or unreadable.
     */
    public java.math.BigDecimal dentistShare() {
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(get(DENTIST_SHARE_KEY).trim());
            if (value.compareTo(java.math.BigDecimal.ZERO) < 0
                    || value.compareTo(java.math.BigDecimal.ONE) > 0) {
                return new java.math.BigDecimal("0.60");
            }
            return value;
        } catch (NumberFormatException e) {
            return new java.math.BigDecimal("0.60");
        }
    }

    /** The flat service charge per bill, read live like {@link #dentistShare()}. */
    public java.math.BigDecimal serviceCharge() {
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(get(SERVICE_CHARGE_KEY).trim());
            if (value.compareTo(java.math.BigDecimal.ZERO) < 0) {
                return new java.math.BigDecimal("200");
            }
            return value;
        } catch (NumberFormatException e) {
            return new java.math.BigDecimal("200");
        }
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
        if (DENTIST_SHARE_KEY.equals(key)) {
            stored = validatedShare(stored);
        }
        if (SERVICE_CHARGE_KEY.equals(key)) {
            stored = validatedCharge(stored);
        }
        settings.save(key, stored);
    }

    /**
     * A share as the Pricing tab posts it (percent, "60") or as stored (fraction,
     * "0.60") — both accepted, always stored as a 0..1 fraction.
     */
    static String validatedShare(String raw) {
        final java.math.BigDecimal value;
        try {
            value = new java.math.BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Dentist share must be a number.");
        }
        if (value.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Dentist share cannot be negative.");
        }
        // Over 1 means the tab posted percent; at most 100.
        if (value.compareTo(java.math.BigDecimal.ONE) > 0) {
            if (value.compareTo(new java.math.BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Dentist share cannot pass 100 percent.");
            }
            return value.divide(new java.math.BigDecimal("100")).toPlainString();
        }
        return value.toPlainString();
    }

    static String validatedCharge(String raw) {
        final java.math.BigDecimal value;
        try {
            value = new java.math.BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Service charge must be a number, like 200.");
        }
        if (value.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service charge cannot be negative.");
        }
        return value.toPlainString();
    }

    private String defaultFor(String key) {
        return switch (key) {
            case "clinic.name"    -> config.get("clinic.name", "Sunrise Dental Clinic");
            case "clinic.phone"   -> config.get("clinic.phone", "");
            case "clinic.email"   -> config.get("clinic.email", "");
            case "clinic.address" -> config.get("clinic.address", "");
            case DENTIST_SHARE_KEY -> config.get(DENTIST_SHARE_KEY, "0.60");
            case SERVICE_CHARGE_KEY -> config.get(SERVICE_CHARGE_KEY, "200");
            default -> "";
        };
    }

    private static String label(String key) {
        return switch (key) {
            case "clinic.name"    -> "Clinic name";
            case "clinic.phone"   -> "Phone number";
            case "clinic.email"   -> "Email address";
            case "clinic.address" -> "Address";
            case DENTIST_SHARE_KEY -> "Dentist share";
            case SERVICE_CHARGE_KEY -> "Service charge";
            default -> key;
        };
    }
}
