package edu.ucla.library.iiif.auth.handlers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.services.AccessCookieCryptoService;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
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
     * The URI path for access token requests.
     */
    protected static final String GET_TOKEN_PATH = "/token";

    /**
     * The URI path template for access level requests.
     */
    protected static final String GET_ACCESS_LEVEL_PATH = "/access/{}";

    /**
     * The URI path template for access cookie requests.
     */
    protected static final String GET_COOKIE_PATH = "/cookie?origin={}";

    /**
     * A test id.
     */
    protected static final String TEST_ID = "ark:/21198/00000000";

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
    protected AccessCookieCryptoService myAccessCookieCryptoService;

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

            myConfig = config;
            myWebClient = WebClient.create(aVertx);
            myPort = config.getInteger(Config.HTTP_PORT, 8888);
            myAccessCookieCryptoService = AccessCookieCryptoService.create(config);

            // Add some database entries
            return CompositeFuture.all(db.setAccessLevel(TEST_ID, 0), db.setDegradedAllowed(TEST_ORIGIN, false))
                    .compose(result -> db.close());
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
