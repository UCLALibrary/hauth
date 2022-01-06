
package edu.ucla.library.iiif.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceException;

/**
 * A test of the database connection.
 */
public class DatabaseServiceIT extends AbstractServiceTest {

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
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeEach
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        ConfigRetriever.create(aVertx).getConfig().onSuccess(config -> {
            // In order to test the service proxy, we need to instantiate the service first
            final DatabaseService service = DatabaseService.create(aVertx, config);
            final ServiceBinder binder = new ServiceBinder(aVertx);

            // Register the service on the event bus, and keep a reference to it so it can be unregistered later
            myService = binder.setAddress(DatabaseService.ADDRESS).register(DatabaseService.class, service);

            // Now we can instantiate a proxy to the service
            myServiceProxy = DatabaseService.createProxy(aVertx);

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
            final ServiceException error = (ServiceException) details;

            assertEquals(DatabaseServiceError.NOT_FOUND, DatabaseServiceImpl.getError(error));
            assertEquals(id, error.getMessage());

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
     * Tests reading an item whose "access level" has been set more than once.
     *
     * @param aContext A test context
     */
    @Test
    final void testGetAccessLevelSetTwice(final VertxTestContext aContext) {
        final String id = "setTwice";
        final int expected = 2;
        final Future<Void> setTwice =
                myServiceProxy.setAccessLevel(id, 1).compose(put -> myServiceProxy.setAccessLevel(id, expected));

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
            final ServiceException error = (ServiceException) details;

            assertEquals(DatabaseServiceError.NOT_FOUND, DatabaseServiceImpl.getError(error));
            assertEquals(url, error.getMessage());

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

    protected Logger getLogger() {
        return LOGGER;
    }
}
