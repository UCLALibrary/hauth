
package edu.ucla.library.iiif.auth;

/**
 * Access token error conditions.
 *
 * @see <a href="https://iiif.io/api/auth/1.0/#access-token-error-conditions">The semantics of each error</a>
 */
@SuppressWarnings("PMD.FieldNamingConventions") // It's easier if the name is the same as the string representation
public enum AccessTokenError {
    /**
     * The request is invalid.
     */

    invalidRequest,
    /**
     * Credentials are missing.
     */

    missingCredentials,
    /**
     * Credentials provided are invalid.
     */

    invalidCredentials,
    /**
     * The request comes from an invalid origin.
     */

    invalidOrigin,

    /**
     * The requested operation is currently unavailable.
     */
    unavailable
}
