package com.sunrise.clinic.platform.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application settings, loaded once at start-up.
 *
 * <p>Replaces the framework's {@code @Value} injection. Values are read from
 * {@code clinic.properties} on the classpath, and any of them can be overridden
 * by an environment variable so the same WAR runs in dev, QA and production
 * without a rebuild. The variable name is the property name upper-cased with
 * dots replaced by underscores — {@code db.url} is overridden by {@code DB_URL}.</p>
 */
public final class AppConfig {

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());
    private static final String FILE = "clinic.properties";

    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE)) {
            if (in != null) {
                properties.load(in);
            } else {
                log.warning(FILE + " not found on the classpath — using defaults and environment variables");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + FILE, e);
        }
    }

    /** @return the property value, or {@code defaultValue} when it is not set anywhere. */
    public String get(String key, String defaultValue) {
        String fromEnv = System.getenv(envName(key));
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String raw = get(key, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Invalid integer for {0}: {1} — using {2}",
                    new Object[]{key, raw, defaultValue});
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String raw = get(key, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Invalid number for {0}: {1} — using {2}",
                    new Object[]{key, raw, defaultValue});
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String raw = get(key, null);
        return raw == null || raw.isBlank() ? defaultValue : Boolean.parseBoolean(raw.trim());
    }

    /** {@code db.url} &rarr; {@code DB_URL} */
    private static String envName(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }
}
