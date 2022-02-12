
package edu.ucla.library.iiif.auth;

/**
 * OpenAPI operation IDs.
 */
public final class Op {

    /**
     * Gets the application's status.
     */
    public static final String GET_STATUS = "getStatus";

    /**
     * Gets an item's access mode.
     */
    public static final String GET_ACCESS_MODE = "getAccessMode";

    /**
     * Gets an authentication cookie.
     */
    public static final String GET_COOKIE = "getCookie";

    /**
     * Gets an authentication token.
     */
    public static final String GET_TOKEN = "getToken";

    /**
     * Gets a Sinai authentication token.
     */
    public static final String GET_TOKEN_SINAI = "getTokenSinai";

    /**
     * Sets items.
     */
    public static final String POST_ITEMS = "postItems";

    /**
     * Constant class constructors should be private.
     */
    private Op() {
        // This is intentionally left empty
    }

}
