package edu.ucla.library.iiif.auth;

/**
 * A constants class for keys used in the JSON for representing access cookies.
 * <p>
 * Cookies are stored on the client machine as base64-encoded JSON. Decoded, they look like this:
 *
 * <pre>
 * {
 *   "version": 0.0.0-SNAPSHOT,
 *   "secret": [encrypted],
 *   "nonce": "0123456789ABCDEF0123456789ABCDEF"
 * }
 * </pre>
 *
 * Decrypted, the secret looks something like this:
 *
 * <pre>
 * {
 *   "client-ip-address": "127.0.0.1",
 *   "campus-network": false,
 *   "degraded-allowed": true
 * }
 * </pre>
 */
public final class CookieJsonKeys {

    /**
     * The JSON key for the Hauth version identifier.
     */
    public static final String VERSION = "version";

    /**
     * The JSON key for the encrypted access cookie data.
     */
    public static final String SECRET = "secret";

    /**
     * The JSON key for the CBC initialization vector.
     */
    public static final String NONCE = "nonce";

    /**
     * The JSON key for the client IP address.
     */
    public static final String CLIENT_IP_ADDRESS = "client-ip-address";

    /**
     * The JSON key for whether the client IP address is on the campus network.
     */
    public static final String CAMPUS_NETWORK = "campus-network";

    /**
     * The JSON key for whether degraded content is available at the origin for which the cookie applies.
     */
    public static final String DEGRADED_ALLOWED = "degraded-allowed";

    /**
     * Private constructor for utility class.
     */
    private CookieJsonKeys() {
    }
}
