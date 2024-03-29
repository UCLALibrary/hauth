
package edu.ucla.library.iiif.auth.handlers;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.TestUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Tuple;

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
     * The URI path for items requests.
     */
    protected static final String POST_ITEMS_PATH = "/items";

    /**
     * The name of the HTTP request header used by the reverse proxy to carry the client IP address.
     */
    protected static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * The fictitious client IP address that we'll pretend a reverse proxy sent through.
     */
    protected static final String FORWARDED_CLIENT_IP = "10.1.1.1";

    /**
     * The fictitious proxy IP address that we'll pretend a reverse proxy sent through.
     */
    protected static final String FORWARDED_PROXY_IP = "10.2.2.2";

    /**
     * The value to use for the {@link CLIENT_IP_HEADER} header.
     */
    protected static final String FORWARDED_IP_ADDRESSES =
            StringUtils.format("{}, {}", FORWARDED_CLIENT_IP, FORWARDED_PROXY_IP);

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
     * Mock values for a pair of Sinai cookies that have not expired yet.
     */
    protected Tuple myMockSinaiCookies;

    /**
     * Mock values for a pair of Sinai cookies that have expired.
     */
    protected Tuple myMockSinaiCookiesExpired;

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
            @SuppressWarnings("rawtypes")
            final List<Future> dbOps = List.of(db.setAccessMode(TEST_ID_OPEN_ACCESS, 0),
                    db.setAccessMode(TEST_ID_TIERED_ACCESS, 1), db.setAccessMode(TEST_ID_ALL_OR_NOTHING_ACCESS, 2));

            myConfig = config;
            myWebClient = WebClient.create(aVertx);
            myPort = config.getInteger(Config.HTTP_PORT, 8888);

            try {
                myAccessCookieService = AccessCookieService.create(config);
            } catch (final GeneralSecurityException details) {
                return Future.failedFuture(details);
            }

            // We also need some Sinai cookies to test with
            try {
                final Random random = new Random();

                // Choose a day within the past 3
                myMockSinaiCookies =
                        TestUtils.getMockSinaiCookies(config, LocalDate.now().minusDays(random.nextInt(3 + 1)));

                // Choose a day after the past 3 days but within, say, the past 90
                myMockSinaiCookiesExpired = TestUtils.getMockSinaiCookies(config,
                        LocalDate.now().minusDays(4 + random.nextInt(90 - 4 + 1)));
            } catch (final Exception details) {
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
