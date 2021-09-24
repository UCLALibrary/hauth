
package edu.ucla.library.iiif.auth.services;

/**
 * AccessCookieService errors.
 */
public enum AccessCookieServiceError {
    // The failure code if the service is configured improperly.
    CONFIGURATION,

    // The failure code if the service receives a decryption request for a cookie that has been tampered with or stolen.
    INVALID_COOKIE,
}
