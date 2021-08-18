
package edu.ucla.library.iiif.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.client.RedisAPI;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * A test of the database connection.
 */
@ExtendWith(VertxExtension.class)
public class DatabaseServiceIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceIT.class, MessageCodes.BUNDLE);

    /**
     * A string that reads "null" (for logging purposes).
     */
    private static final String NULL = "null";

    /**
     * The service proxy for testing typical client usage.
     */
    private DatabaseService myServiceProxy;

    /**
     * Only used for event bus unregistration.
     */
    private MessageConsumer<JsonObject> myService;

    /**
     * Non-primitive types like {@link PgPool} cannot be sent over the event bus, so instead of registering a message
     * codec for these types, we can call e.g. {@link DatabaseService#getConnectionPool} directly on the proxy's
     * underlying service instance.
     */
    private DatabaseService myUnderlyingService;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeEach
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        // In order to test the service proxy, we need to instantiate the service first
        DatabaseService.create(aVertx).compose(service -> {
            final ServiceBinder binder = new ServiceBinder(aVertx);

            myService = binder.setAddress(DatabaseService.ADDRESS).register(DatabaseService.class, service);
            myUnderlyingService = service;

            // Now we can instantiate a proxy to the service
            return DatabaseService.createProxy(aVertx);
        }).onSuccess(serviceProxy -> {
            myServiceProxy = serviceProxy;
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
        // Close the service proxy, then unregister the service (order important)
        myServiceProxy.close().compose(result -> myService.unregister()).onSuccess(success -> aContext.completeNow())
                .onFailure(aContext::failNow);
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
        myUnderlyingService.getConnectionPool().compose(pool -> pool.withConnection(connection -> {
            final String sql = "select * from pg_catalog.pg_user;";

            return connection.query(sql).execute();
        })).onSuccess(result -> {
            // Three users tells us our SQL load successfully completed
            assertEquals(3, result.size());
            aContext.completeNow();
        }).onFailure(aContext::failNow);
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

        myServiceProxy.getAccessLevel(id).onFailure(details -> {
            // The get should fail since nothing has been set for the id
            aContext.completeNow();
        }).onSuccess(result -> {
            // The following will always fail
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
        final Future<Void> setOnce = myServiceProxy.setAccessLevel(id, expected);

        setOnce.compose(put -> myServiceProxy.getAccessLevel(id)).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        }).onFailure(aContext::failNow);
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
        final Future<Void> setTwice = myServiceProxy.setAccessLevel(id, 1)
                .compose(put -> myServiceProxy.setAccessLevel(id, expected));

        setTwice.compose(put -> myServiceProxy.getAccessLevel(id)).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests reading an origin whose "degraded allowed" has not been set.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedUnset(final VertxTestContext aContext) {
        final String url = "https://library.ucla.edu";
        final String expected = NULL;

        myServiceProxy.getDegradedAllowed(url).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        }).onFailure(details -> {
            aContext.completeNow();
        });
    }

    /**
     * Tests reading an origin whose "degraded allowed" has been set once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedSetOnce(final VertxTestContext aContext) {
        final String url = "https://iiif.library.ucla.edu";
        final boolean expected = true;
        final Future<Void> setOnce = myServiceProxy.setDegradedAllowed(url, expected);

        setOnce.compose(put -> myServiceProxy.getDegradedAllowed(url)).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests reading an origin whose "degraded allowed" has been set more than once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetDegradedAllowedSetTwice(final VertxTestContext aContext) {
        final String url = "https://iiif.sinaimanuscripts.library.ucla.edu";
        final boolean expected = true;
        final Future<Void> setTwice = myServiceProxy.setDegradedAllowed(url, false)
                .compose(put -> myServiceProxy.setDegradedAllowed(url, expected));

        setTwice.compose(put -> myServiceProxy.getDegradedAllowed(url)).onSuccess(result -> {
            completeIfExpectedElseFail(result, expected, aContext);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the database cache is up and usable.
     *
     * @param aVertx A Vert.x instance used to run the tests
     * @param aContext A test context
     */
    @Test
    final void testDbCacheConnection(final Vertx aVertx, final VertxTestContext aContext) {
        myUnderlyingService.getRedisClient().compose(client -> {
            return RedisAPI.api(client).lolwut(List.of());
        }).onSuccess(response -> {
            for (final String line : response.toString().split("\\r?\\n")) {
                LOGGER.debug(line);
            }
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

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
