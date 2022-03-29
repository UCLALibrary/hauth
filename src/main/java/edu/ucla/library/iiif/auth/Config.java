
package edu.ucla.library.iiif.auth;

/**
 * Properties that are used to configure the application.
 */
@SuppressWarnings({ "PMD.CommentSize", "checkstyle:lineLengthChecker" })
public final class Config {

    /**
     * The ENV property for the application's version identifier.
     */
    public static final String HAUTH_VERSION = "HAUTH_VERSION";

    /**
     * The ENV property for the application's port.
     */
    public static final String HTTP_PORT = "HTTP_PORT";

    /**
     * The ENV property for the application's host.
     */
    public static final String HTTP_HOST = "HTTP_HOST";

    /**
     * The optional ENV property for the number of seconds after which the pop-up window that is presented to users
     * after their client has called the access cookie service should close.
     * <p>
     * If unset, the window must be closed with user interaction.
     */
    public static final String ACCESS_COOKIE_WINDOW_CLOSE_DELAY = "ACCESS_COOKIE_WINDOW_CLOSE_DELAY";

    /**
     * The optional ENV property for the number of seconds after which an access token will cease to be valid.
     */
    public static final String ACCESS_TOKEN_EXPIRES_IN = "ACCESS_TOKEN_EXPIRES_IN";

    /**
     * The ENV property for the location of the application's OpenAPI specification.
     */
    public static final String API_SPEC = "API_SPEC";

    /**
     * The ENV property for the API key for private endpoints.
     */
    public static final String API_KEY = "API_KEY";

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
     * The ENV property for the database cache host.
     */
    public static final String DB_CACHE_HOST = "DB_CACHE_HOST";

    /**
     * The ENV property for the database cache port.
     */
    public static final String DB_CACHE_PORT = "DB_CACHE_PORT";

    /**
     * The ENV property for the list of Campus Network subnets, separated by commas, in CIDR notation.
     */
    public static final String CAMPUS_NETWORK_SUBNETS = "CAMPUS_NETWORK_SUBNETS";

    /**
     * The ENV property for the password used to derive the secret key for generating and validating access cookies.
     */
    @SuppressWarnings({ "PMD.LongVariable" })
    public static final String SECRET_KEY_GENERATION_PASSWORD = "SECRET_KEY_GENERATION_PASSWORD";

    /**
     * The ENV property for the salt used to derive the secret key for generating and validating access cookies.
     */
    @SuppressWarnings({ "PMD.LongVariable" })
    public static final String SECRET_KEY_GENERATION_SALT = "SECRET_KEY_GENERATION_SALT";

    /**
     * The ENV property for the password used to derive the secret key for validating Sinai cookies. This value must be
     * the same as the {@code CIPHER_KEY} configuration option of the Sinai application, which is used to generate its
     * encryption key.
     *
     * @see <a href=
     *      "https://github.com/UCLALibrary/sinaimanuscripts/blob/44cbbd9bf508c32b742f1617205a679edf77603e/app/controllers/application_controller.rb#L100">How
     *      the Sinai application's key generation password is configured</a>
     */
    @SuppressWarnings({ "PMD.LongVariable" })
    public static final String SINAI_COOKIE_SECRET_KEY_GENERATION_PASSWORD =
            "SINAI_COOKIE_SECRET_KEY_GENERATION_PASSWORD";

    /**
     * The ENV property for the prefix that we'll use to validate decrypted Sinai cookies. This value be consistent with
     * the Sinai application.
     *
     * @see <a href=
     *      "https://github.com/UCLALibrary/sinaimanuscripts/blob/44cbbd9bf508c32b742f1617205a679edf77603e/app/controllers/application_controller.rb#L98-L103">How
     *      the Sinai application creates cookie values</a>
     */
    public static final String SINAI_COOKIE_VALID_PREFIX = "SINAI_COOKIE_VALID_PREFIX";

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

}
