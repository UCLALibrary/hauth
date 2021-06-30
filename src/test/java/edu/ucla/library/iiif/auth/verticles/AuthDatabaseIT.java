
package edu.ucla.library.iiif.auth.verticles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.junit5.VertxExtension;

/**
 * A test of the database connection.
 */
@ExtendWith(VertxExtension.class)
public class AuthDatabaseIT extends AbstractBouncerIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDatabaseIT.class, MessageCodes.BUNDLE);

    /**
     * Sets up the testing environment.
     */
    @BeforeAll
    public static final void testEnvSetUp() {
        LOGGER.debug(MessageCodes.BNCR_003, System.getenv(Config.DB_PASSWORD));
    }

}
