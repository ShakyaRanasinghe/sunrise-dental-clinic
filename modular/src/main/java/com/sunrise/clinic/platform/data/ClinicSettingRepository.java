package com.sunrise.clinic.platform.data;

import java.util.Map;
import java.util.Optional;

/**
 * Persistence for the clinic identity settings (name, phone, email, address).
 *
 * <p>A simple key-value store rather than a full entity: the four settings are
 * loaded by name and saved by name, with no identity column or version field.
 * The repository returns empty Optional when a key has no row; the service
 * layer falls back to the properties-file default.</p>
 */
public interface ClinicSettingRepository {

    /** The value stored for {@code key}, or empty if no row exists. */
    Optional<String> get(String key);

    /** Every setting as a key-value map. */
    Map<String, String> getAll();

    /** Insert or update one setting. */
    void save(String key, String value);
}
