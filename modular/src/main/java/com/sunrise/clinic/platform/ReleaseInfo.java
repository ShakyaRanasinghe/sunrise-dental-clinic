package com.sunrise.clinic.platform;

/**
 * The application version shown in every page footer.
 *
 * <p>The release number exists so a person sees which build the clinic is
 * running and can act on it — a support call should start "you are on
 * release …" rather than an archeological dig through receipts.</p>
 *
 * <p>When the WAR carries an {@code Implementation-Version} manifest entry
 * (set by Maven from {@code <version>} on a normal {@code mvn package}), that
 * is used. In the fast class-by-class deploy loop there is no fresh manifest,
 * so {@link #VERSION} is the fallback — and the two are kept in lockstep with
 * {@code pom.xml}'s {@code <version>}.</p>
 */
public final class ReleaseInfo {

    /** Fallback build version — keep in lockstep with {@code <version>} in pom.xml. */
    public static final String VERSION = "3.0.0-SNAPSHOT";

    /** @return the current release, e.g. {@code 3.0.0-SNAPSHOT} */
    public static String version() {
        String fromManifest = ReleaseInfo.class.getPackage().getImplementationVersion();
        return fromManifest == null || fromManifest.isBlank() ? VERSION : fromManifest;
    }

    private ReleaseInfo() {
    }
}