
package edu.ucla.library.iiif.auth;

/**
 * A constants class for valid keys for passing values to Handlebars templates.
 */
public final class TemplateKeys {

    /**
     * The access token object key.
     */
    public static final String ACCESS_TOKEN_OBJECT = "accessTokenObject";

    /**
     * The campus network key.
     */
    public static final String CAMPUS_NETWORK = "campusNetwork";

    /**
     * The client IP address key.
     */
    public static final String CLIENT_IP_ADDRESS = "clientIpAddress";

    /**
     * The degraded allowed key.
     */
    public static final String DEGRADED_ALLOWED = "degradedAllowed";

    /**
     * The window close delay key.
     */
    public static final String WINDOW_CLOSE_DELAY = "windowCloseDelay";

    /**
     * The origin key.
     */
    public static final String ORIGIN = "origin";

    /**
     * The version key.
     */
    public static final String VERSION = "version";

    /**
     * Private constructor for utility class.
     */
    private TemplateKeys() {
    }
}
