
package edu.ucla.library.iiif.auth;

/**
 * Access token error conditions.
 *
 * @see <a href="https://iiif.io/api/auth/1.0/#access-token-error-conditions">The semantics of each error</a>
 */
@SuppressWarnings("PMD.FieldNamingConventions") // It's easier if the name is the same as the string representation
public enum AccessTokenError {
    invalidRequest, missingCredentials, invalidCredentials, invalidOrigin, unavailable
}
