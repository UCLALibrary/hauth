
package edu.ucla.library.iiif.auth;

/**
 * Valid JSON keys for the response schemas defined in the Hauth OpenAPI specification.
 */
public final class ResponseJsonKeys {

    /**
     * The status key.
     */
    public static final String STATUS = "status";

    /**
     * The ID key.
     */
    public static final String ID = "id";

    /**
     * The restricted key.
     */
    public static final String RESTRICTED = "restricted";

    /**
     * The error key.
     */
    public static final String ERROR = "error";

    /**
     * The message key.
     */
    public static final String MESSAGE = "message";

    /**
     * The access token key.
     */
    public static final String ACCESS_TOKEN = "accessToken";

    /**
     * The expires-in key.
     */
    public static final String EXPIRES_IN = "expiresIn";

    /**
     * Private constructor for utility class.
     */
    private ResponseJsonKeys() {
    }
}
