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
 * so {@link #VERSION} is the fallback. {@code VERSION} is the <em>released</em>
 * tag the clinic is actually running (GAP-FTB-01), deliberately distinct from
 * the internal Maven snapshot {@code <version>} in {@code pom.xml} — a user
 * asking for support should read the release that shipped, not the build
 * snapshot the source tree happens to carry.</p>
 */
public final class ReleaseInfo {

    /** The released version shown in every footer — the git tag of this deploy (v1.0.1). */
    public static final String VERSION = "1.0.1";

    /** @return the current release, e.g. {@code 1.0.1} */
    public static String version() {
        String fromManifest = ReleaseInfo.class.getPackage().getImplementationVersion();
        return fromManifest == null || fromManifest.isBlank() ? VERSION : fromManifest;
    }

    private ReleaseInfo() {
    }
}