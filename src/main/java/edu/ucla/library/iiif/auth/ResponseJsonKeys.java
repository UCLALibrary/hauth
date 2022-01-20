
package edu.ucla.library.iiif.auth;

/**
 * A constants class for JSON keys for the response schemas that are defined in the Hauth OpenAPI specification.
 */
public final class ResponseJsonKeys {

    /**
     * The status key.
     */
    public static final String STATUS = "status";

    /**
     * The access mode key.
     */
    public static final String ACCESS_MODE = "accessMode";

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
     * The message ID key.
     */
    public static final String MESSAGE_ID = Param.MESSAGE_ID;

    /**
     * Private constructor for utility class.
     */
    private ResponseJsonKeys() {
    }
}
