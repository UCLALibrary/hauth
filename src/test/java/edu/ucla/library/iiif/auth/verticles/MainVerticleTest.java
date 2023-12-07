
package edu.ucla.library.iiif.auth.verticles;

import static info.freelibrary.util.Constants.SLASH;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests the {@link MainVerticle}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class MainVerticleTest {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleTest.class, MessageCodes.BUNDLE);

    /**
     * An instance of the verticle we're testing.
     */
    private final Verticle myVerticle = new MainVerticle();

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        aVertx.deployVerticle(myVerticle).onSuccess(deploymentID -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterAll
    public final void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        final Stream<Future<Void>> undeployAll = aVertx.deploymentIDs().stream().map(id -> aVertx.undeploy(id));

        CompositeFuture.all(undeployAll.collect(Collectors.toList())).onSuccess(result -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the event bus services are registered.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testEventBusServicesAreRegistered(final Vertx aVertx, final VertxTestContext aContext) {
        try {
            final Set<MessageConsumer<?>> services = getEventBusServices((MainVerticle) myVerticle);
            final Set<String> addresses = services.stream().map(MessageConsumer::address).collect(Collectors.toSet());

            aContext.verify(() -> {
                assertNotEquals(services, null);
                assertTrue(addresses.containsAll(Set.of(AccessCookieService.ADDRESS, DatabaseService.ADDRESS)));
                services.forEach(service -> assertTrue(service.isRegistered()));

                aContext.completeNow();
            });
        } catch (final AssertionError | IllegalAccessException | NoSuchFieldException details) {
            aContext.failNow(details);
        }
    }

    /**
     * Test that the HTTP server is listening.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testServerIsListening(final Vertx aVertx, final VertxTestContext aContext) {
        try {
            final HttpServer server = getServer((MainVerticle) myVerticle);

            assertNotEquals(server, null);

            // Just see if we can get a response, even if it's a 4xx or 5xx
            WebClient.create(aVertx).get(server.actualPort(), "0.0.0.0", SLASH).send().onSuccess(result -> {
                aContext.completeNow();
            }).onFailure(aContext::failNow);
        } catch (final AssertionError | IllegalAccessException | NoSuchFieldException details) {
            aContext.failNow(details);
        }
    }

    /**
     * Gets a {@link MainVerticle}'s event bus services via reflection.
     *
     * @param aMainVerticle A verticle instance
     * @return The event bus services
     * @throws IllegalAccessException An exception for illegal access
     * @throws NoSuchFieldException An exception for a field that does not exist
     */
    @SuppressWarnings("unchecked")
    private Set<MessageConsumer<?>> getEventBusServices(final MainVerticle aMainVerticle)
            throws IllegalAccessException, NoSuchFieldException {
        final Field eventBusServices = aMainVerticle.getClass().getDeclaredField("myEventBusServices");

        eventBusServices.setAccessible(true);

        return (Set<MessageConsumer<?>>) eventBusServices.get(aMainVerticle);
    }

    /**
     * Gets a {@link MainVerticle}'s HTTP server via reflection.
     *
     * @param aMainVerticle A verticle instance
     * @return The HTTP server
     * @throws IllegalAccessException An exception for illegal access
     * @throws NoSuchFieldException An exception for a field that does not exist
     */
    private HttpServer getServer(final MainVerticle aMainVerticle) throws IllegalAccessException, NoSuchFieldException {
        final Field server = myVerticle.getClass().getDeclaredField("myServer");

        server.setAccessible(true);

        return (HttpServer) server.get(aMainVerticle);
    }
}
