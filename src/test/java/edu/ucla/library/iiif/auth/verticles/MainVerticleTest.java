
package edu.ucla.library.iiif.auth.verticles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests the main verticle of the Vert.x application.
 */
@ExtendWith(VertxExtension.class)
public class MainVerticleTest extends AbstractHauthTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleTest.class, MessageCodes.BUNDLE);

    /**
     * Tests the server can start successfully.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(final Vertx aVertx, final VertxTestContext aContext) {
        final WebClient client = WebClient.create(aVertx);

        client.get(myPort, TestConstants.INADDR_ANY, "/status").send(get -> {
            if (get.succeeded()) {
                assertEquals(HTTP.OK, get.result().statusCode());
                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
