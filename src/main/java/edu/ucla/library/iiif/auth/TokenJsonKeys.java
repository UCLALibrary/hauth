
package edu.ucla.library.iiif.auth;

/**
 * A constants class for keys used in the JSON for representing access tokens.
 * <p>
 * Tokens are passed around as base64-encoded JSON. Decoded, they look like this:
 *
 * <pre>
 * {
 *   "version": 0.0.0-SNAPSHOT,
 *   "campusNetwork": true
 * }
 * </pre>
 */
public final class TokenJsonKeys {

    /**
     * The JSON key for the Hauth version identifier.
     */
    public static final String VERSION = "version";

    /**
     * The JSON key for whether the client IP address is on the campus network.
     */
    public static final String CAMPUS_NETWORK = "campusNetwork";

    /**
     * The JSON key for whether the client has proven affiliation with Sinai.
     */
    public static final String SINAI_AFFILIATE = "sinaiAffiliate";

    /**
     * Private constructor for utility class.
     */
    private TokenJsonKeys() {
    }
}
