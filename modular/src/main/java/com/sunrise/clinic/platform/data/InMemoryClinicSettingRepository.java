package com.sunrise.clinic.platform.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link ClinicSettingRepository} for unit tests.
 */
public class InMemoryClinicSettingRepository implements ClinicSettingRepository {

    private final Map<String, String> store = new HashMap<>();

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public Map<String, String> getAll() {
        return new HashMap<>(store);
    }

    @Override
    public void save(String key, String value) {
        store.put(key, value);
    }
}
