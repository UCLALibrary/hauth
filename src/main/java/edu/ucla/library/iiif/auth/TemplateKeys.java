
package edu.ucla.library.iiif.auth;

/**
 * Valid keys for Handlebars templates.
 */
public final class TemplateKeys {

    /**
     * The access token object key.
     */
    public static final String ACCESS_TOKEN_OBJECT = "access-token-object";

    /**
     * The campus network key.
     */
    public static final String CAMPUS_NETWORK = CookieJsonKeys.CAMPUS_NETWORK;

    /**
     * The client IP address key.
     */
    public static final String CLIENT_IP_ADDRESS = CookieJsonKeys.CLIENT_IP_ADDRESS;

    /**
     * The degraded allowed key.
     */
    public static final String DEGRADED_ALLOWED = CookieJsonKeys.DEGRADED_ALLOWED;

    /**
     * The origin key.
     */
    public static final String ORIGIN = Param.ORIGIN;

    /**
     * The version key.
     */
    public static final String VERSION = CookieJsonKeys.VERSION;

    /**
     * Private constructor for utility class.
     */
    private TemplateKeys() {
    }
}
