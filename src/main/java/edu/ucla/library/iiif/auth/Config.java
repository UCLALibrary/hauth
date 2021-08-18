
package edu.ucla.library.iiif.auth;

/**
 * Properties that are used to configure the application.
 */
public final class Config {

    /**
     * The ENV property for the application's port.
     */
    public static final String HTTP_PORT = "HTTP_PORT";

    /**
     * The ENV property for the application's host.
     */
    public static final String HTTP_HOST = "HTTP_HOST";

    /**
     * The ENV property for the location of the application's OpenAPI specification.
     */
    public static final String API_SPEC = "API_SPEC";

    /**
     * The ENV property for the database password.
     */
    public static final String DB_PASSWORD = "DB_PASSWORD";

    /**
     * The ENV property for the database port.
     */
    public static final String DB_PORT = "DB_PORT";

    /**
     * The ENV property for the database host.
     */
    public static final String DB_HOST = "DB_HOST";

    /**
     * The ENV property for the database name.
     */
    public static final String DB_NAME = "DB_NAME";

    /**
     * The ENV property for the database user.
     */
    public static final String DB_USER = "DB_USER";

    /**
     * The ENV property for the database cache port.
     */
    public static final String DB_CACHE_PORT = "DB_CACHE_PORT";

    /**
     * The ENV property for the max size of the database connection pool.
     */
    public static final String DB_CONNECTION_POOL_MAX_SIZE = "DB_CONNECTION_POOL_MAX_SIZE";

    /**
     * The ENV property for the number of database reconnect attempts.
     */
    public static final String DB_RECONNECT_ATTEMPTS = "DB_RECONNECT_ATTEMPTS";

    /**
     * The ENV property for the length of the database reconnect interval (in milliseconds).
     */
    public static final String DB_RECONNECT_INTERVAL = "DB_RECONNECT_INTERVAL";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

}
