
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
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
        final HttpRequest<?> getStatus = myWebClient.get(myPort, TestConstants.INADDR_ANY, GET_STATUS_PATH);

        getStatus.send().onSuccess(response -> {
            assertEquals(HTTP.OK, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
