
package edu.ucla.library.iiif.auth;

/**
 * A constants class for cookie names.
 */
public final class CookieNames {

    /**
     * The name of the cookie created by this application.
     */
    public static final String HAUTH = "iiif-access";

    /**
     * The name of the cookie, created by the Sinai application, which contains the affiliation of the bearer.
     */
    public static final String SINAI_CIPHERTEXT = "sinai_authenticated_3day";

    /**
     * The name of the cookie, created by the Sinai application, which contains the initialization vector used to create
     * {@link #SINAI_CIPHERTEXT}.
     */
    public static final String SINAI_IV = "initialization_vector";

    /**
     * Private constructor for utility class.
     */
    private CookieNames() {
    }
}
