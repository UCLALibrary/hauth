
package edu.ucla.library.iiif.auth;

/**
 * Properties that are used to configure the application.
 */
public final class Config {

    /**
     * The configuration property for the application's port.
     */
    public static final String HTTP_PORT = "http.port";

    /**
     * The configuration property for the application's host.
     */
    public static final String HTTP_HOST = "http.host";

    /**
     * The configuration property for the location of the application's OpenAPI specification.
     */
    public static final String API_SPEC = "api.spec";

    /**
     * The configuration property for the database password.
     */
    public static final String DB_PASSWORD = "pgsql.password";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

}
