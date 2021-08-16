
package edu.ucla.library.iiif.auth.verticles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * A test of the database connection.
 */
@ExtendWith(VertxExtension.class)
public class AuthDatabaseIT extends AbstractHauthIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDatabaseIT.class, MessageCodes.BUNDLE);

    /**
     * A string that reads "null" (for logging purposes).
     */
    private static final String NULL = "null";

    private DatabaseService myDbService;

    @Override
    @BeforeEach
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        DatabaseService.create(aVertx).onSuccess(service -> {
            myDbService = service;
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterEach
    public final void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        myDbService.close().onSuccess(success -> aContext.completeNow()).onFailure(aContext::failNow);
    }

    /**
     * Tests the number of users in the database.
     *
     * @param aVertx A Vert.x instance used to run the tests
     * @param aContext A test context
     */
    @Test
    public final void testDbUserCount(final Vertx aVertx, final VertxTestContext aContext) {
        // Check that the number of users is what we expect it to be
        myDbService.getSqlClient().query("select * from pg_catalog.pg_user;").execute(query -> {
            if (query.succeeded()) {
                // Three users tells us our SQL load successfully completed
                assertEquals(3, query.result().size());
                aContext.completeNow();
            } else {
                aContext.failNow(query.cause());
            }
        });
    }

    /**
     * Tests reading an item whose "access level" has not been set.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetAccessLevelUnset(final VertxTestContext aContext) {
        final String id = "unset";
        final String expected = NULL;

        myDbService.getAccessLevel(id).onFailure(details -> {
            // The get should fail since nothing has been set for the id
            aContext.completeNow();
        }).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    /**
     * Tests reading an item whose "access level" has been set once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetAccessLevelSetOnce(final VertxTestContext aContext) {
        final String id = "setOnce";
        final int expected = 1;
        final Future<Void> setOnce = myDbService.setAccessLevel(id, expected);

        setOnce.compose(put -> myDbService.getAccessLevel(id)).onFailure(aContext::failNow).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    /**
     * Tests reading an item whose "access level" that has been set more than once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetAccessLevelSetTwice(final VertxTestContext aContext) {
        final String id = "setTwice";
        final int expected = 2;
        final Future<Void> setTwice = myDbService.setAccessLevel(id, 1)
                .compose(put -> myDbService.setAccessLevel(id, expected));

        setTwice.compose(put -> myDbService.getAccessLevel(id)).onFailure(aContext::failNow).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    /**
     * Tests reading an origin whose "degraded allowed" has not been set.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedUnset(final VertxTestContext aContext) {
        final URI url = URI.create("https://library.ucla.edu");
        final String expected = NULL;

        myDbService.getDegradedAllowed(url).onFailure(details -> {
            aContext.completeNow();
        }).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    /**
     * Tests reading an origin whose "degraded allowed" has been set once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedSetOnce(final VertxTestContext aContext) {
        final URI url = URI.create("https://iiif.library.ucla.edu");
        final boolean expected = true;
        final Future<Void> setOnce = myDbService.setDegradedAllowed(url, expected);

        setOnce.compose(put -> myDbService.getDegradedAllowed(url)).onFailure(aContext::failNow).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    /**
     * Tests reading an origin whose "degraded allowed" has been set more than once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedSetTwice(final VertxTestContext aContext) {
        final URI url = URI.create("https://iiif.sinaimanuscripts.library.ucla.edu");
        final boolean expected = true;
        final Future<Void> setTwice = myDbService.setDegradedAllowed(url, false)
                .compose(put -> myDbService.setDegradedAllowed(url, expected));

        setTwice.compose(put -> myDbService.getDegradedAllowed(url)).onFailure(aContext::failNow).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Completes the context if the actual result and the expected result are equal, otherwise fails the context.
     *
     * @param <T> The type of result
     * @param aResult The actual result
     * @param aExpected The expected result
     * @param aContext A test context
     */
    private <T> void completeIfExpectedElseFail(final T aResult, final T aExpected,
            final VertxTestContext aContext) {
        if (aResult.equals(aExpected)) {
            aContext.completeNow();
        } else {
            aContext.failNow(LOGGER.getMessage(MessageCodes.AUTH_007, aResult, aExpected));
        }
    }
}
