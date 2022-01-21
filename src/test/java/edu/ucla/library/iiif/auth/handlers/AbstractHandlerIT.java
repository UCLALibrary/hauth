
package edu.ucla.library.iiif.auth.handlers;

import java.security.GeneralSecurityException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * A base class for handler integration tests.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractHandlerIT {

    /**
     * The URI path for status requests.
     */
    protected static final String GET_STATUS_PATH = "/status";

    /**
     * The URI path template for access token requests.
     */
    protected static final String GET_TOKEN_PATH = "/token?{}";

    /**
     * The URI path template for Sinai access token requests.
     */
    protected static final String GET_TOKEN_SINAI_PATH = "/token/sinai?{}";

    /**
     * The URI path template for access mode requests.
     */
    protected static final String GET_ACCESS_MODE_PATH = "/access/{}";

    /**
     * The URI path template for access cookie requests.
     */
    protected static final String GET_COOKIE_PATH = "/cookie?origin={}";

    /**
     * A test id for an item with open access.
     */
    protected static final String TEST_ID_OPEN_ACCESS = "ark:/21198/00000000";

    /**
     * A test id for an item with tiered access.
     */
    protected static final String TEST_ID_TIERED_ACCESS = "ark:/21198/11111111";

    /**
     * A test id for an item with all-or-nothing access.
     */
    protected static final String TEST_ID_ALL_OR_NOTHING_ACCESS = "ark:/21198/22222222";

    /**
     * A test origin.
     */
    protected static final String TEST_ORIGIN = "https://iiif-test.library.ucla.edu";

    /**
     * The application configuration.
     */
    protected JsonObject myConfig;

    /**
     * A WebClient for calling the HTTP API.
     */
    protected WebClient myWebClient;

    /**
     * The port on which the application is listening.
     */
    protected int myPort;

    /**
     * A duplicate service of the one running inside the Hauth container for decrypting access cookies on the test side.
     */
    protected AccessCookieService myAccessCookieService;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            final DatabaseService db = DatabaseService.create(aVertx, config);
            final List<Future> dbOps = List.of(db.setAccessMode(TEST_ID_OPEN_ACCESS, 0),
                    db.setAccessMode(TEST_ID_TIERED_ACCESS, 1), db.setAccessMode(TEST_ID_ALL_OR_NOTHING_ACCESS, 2),
                    db.setDegradedAllowed(TEST_ORIGIN, true));

            myConfig = config;
            myWebClient = WebClient.create(aVertx);
            myPort = config.getInteger(Config.HTTP_PORT, 8888);

            try {
                myAccessCookieService = AccessCookieService.create(config);
            } catch (final GeneralSecurityException details) {
                return Future.failedFuture(details);
            }

            // Add some database entries
            return CompositeFuture.all(dbOps).compose(result -> db.close());
        }).onSuccess(result -> aContext.completeNow()).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterAll
    public void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        aContext.completeNow();
    }
}
