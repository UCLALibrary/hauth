
package edu.ucla.library.iiif.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.json.JsonObject;

/**
 * Properties that are used to configure the application.
 */
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
     * The optional ENV property for the host domain to which the access cookie will be sent.
     * <p>
     * If unset, the access cookie will be sent to whatever domain Hauth itself is hosted at.
     */
    public static final String ACCESS_COOKIE_DOMAIN = "ACCESS_COOKIE_DOMAIN";

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
     * The ENV property for the password used to generate the secret key, which is used for creating and validating
     * access cookies.
     */
    public static final String SECRET_KEY_PASSWORD = "SECRET_KEY_PASSWORD";

    /**
     * The ENV property for the salt used to generate the secret key, which is used for creating and validating access
     * cookies.
     */
    public static final String SECRET_KEY_SALT = "SECRET_KEY_SALT";

    /**
     * The ENV property for the password used to derive the secret key for validating Sinai cookies. This value must be
     * the same as the {@code CIPHER_KEY} configuration option of the Sinai application, which is used to generate its
     * encryption key.
     */
    public static final String SINAI_COOKIE_SECRET_KEY_PASSWORD = "SINAI_COOKIE_SECRET_KEY_PASSWORD";

    /**
     * The ENV property for the prefix that we'll use to validate decrypted Sinai cookies. This value be consistent with
     * the Sinai application.
     */
    public static final String SINAI_COOKIE_VALID_PREFIX = "SINAI_COOKIE_VALID_PREFIX";

    /**
     * A logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class, MessageCodes.BUNDLE);

    /**
     * Constant classes should have private constructors.
     */
    private Config() {
        // This is intentionally left empty
    }

    /**
     * A configuration processor that adds the application version, if available.
     *
     * @param aConfig An application configuration
     * @return The processed application configuration
     */
    public static JsonObject setAppVersion(final JsonObject aConfig) {
        final String manifestPath = "/META-INF/MANIFEST.MF";

        try (InputStream manifest = Config.class.getResourceAsStream(manifestPath)) {
            final Properties properties = new Properties();
            final Optional<String> version;

            properties.load(manifest);
            version = Optional.ofNullable(properties.getProperty("Maven-Version"));

            if (version.isPresent()) {
                return aConfig.copy().put(HAUTH_VERSION, version.get());
            } else {
                return aConfig;
            }
        } catch (final IOException details) {
            // Either the app wasn't deployed as a JAR, or Vert.x Maven Plugin isn't creating the manifest file
            LOGGER.warn(MessageCodes.AUTH_024, manifestPath, details.getMessage());

            return aConfig;
        }
    }

}
