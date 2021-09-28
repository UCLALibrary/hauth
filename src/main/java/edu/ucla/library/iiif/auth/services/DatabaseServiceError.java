
package edu.ucla.library.iiif.auth.services;

/**
 * DatabaseService errors.
 */
public enum DatabaseServiceError {
    // The failure code if the service is unable to query the underlying database.
    INTERNAL,

    // The failure code if the service receives a get request for an unknown id or origin.
    NOT_FOUND;
}