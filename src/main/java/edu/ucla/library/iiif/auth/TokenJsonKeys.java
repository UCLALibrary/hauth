
package edu.ucla.library.iiif.auth;

/**
 * A constants class for keys used in the JSON for representing access tokens.
 * <p>
 * Tokens are passed around as base64-encoded JSON. Decoded, they look like this:
 *
 * <pre>
 * {
 *   "version": 0.0.0-SNAPSHOT,
 *   "campus-network": true
 * }
 * </pre>
 */
public final class TokenJsonKeys {

    /**
     * The JSON key for the Hauth version identifier.
     */
    public static final String VERSION = CookieJsonKeys.VERSION;

    /**
     * The JSON key for whether the client IP address is on the campus network.
     */
    public static final String CAMPUS_NETWORK = CookieJsonKeys.CAMPUS_NETWORK;

    /**
     * Private constructor for utility class.
     */
    private TokenJsonKeys() {
    }
}
