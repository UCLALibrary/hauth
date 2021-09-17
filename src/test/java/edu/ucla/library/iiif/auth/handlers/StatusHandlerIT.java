package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link StatusHandler#handle}.
 */
public final class StatusHandlerIT extends AbstractHandlerIT {

    /**
     * Tests the status handler.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetStatus(final Vertx aVertx, final VertxTestContext aContext) {
        myWebClient.get(myPort, TestConstants.INADDR_ANY, "/status").send(get -> {
            if (get.succeeded()) {
                assertEquals(HTTP.OK, get.result().statusCode());
                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }
}
