
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import info.freelibrary.util.Constants;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessModeHandler#handle}.
 */
public final class AccessModeHandlerIT extends AbstractHandlerIT {

    /**
     * The test's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessModeHandlerIT.class, MessageCodes.BUNDLE);

    /**
     * Tests that a client can get the access mode of an item with open access.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessModeOpen(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_ACCESS_MODE_PATH,
                URLEncoder.encode(TEST_ID_OPEN_ACCESS, StandardCharsets.UTF_8));
        final HttpRequest<?> getAccessMode = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI);

        getAccessMode.send().onSuccess(response -> {
            final JsonObject expected = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, "OPEN");

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsJsonObject());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can get the access mode of an item with tiered access.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessModeTiered(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestUri = StringUtils.format(GET_ACCESS_MODE_PATH,
                URLEncoder.encode(TEST_ID_TIERED_ACCESS, StandardCharsets.UTF_8));
        final HttpRequest<?> getAccessMode = myWebClient.get(myPort, Constants.INADDR_ANY, requestUri);

        getAccessMode.send().onSuccess(response -> {
            final JsonObject expected = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, "TIERED");

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsJsonObject());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can get the access mode of an item with all-or-nothing access.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessModeAllOrNothing(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestUri = StringUtils.format(GET_ACCESS_MODE_PATH,
                URLEncoder.encode(TEST_ID_ALL_OR_NOTHING_ACCESS, StandardCharsets.UTF_8));
        final HttpRequest<?> getAccessMode = myWebClient.get(myPort, Constants.INADDR_ANY, requestUri);

        getAccessMode.send().onSuccess(response -> {
            final JsonObject expected = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, "ALL_OR_NOTHING");

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsJsonObject());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client gets the expected error response when requesting the access mode of an unknown item.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessModeUnknownItem(final Vertx aVertx, final VertxTestContext aContext) {
        final String id = "ark:/21198/unknown";
        final String requestURI =
                StringUtils.format(GET_ACCESS_MODE_PATH, URLEncoder.encode(id, StandardCharsets.UTF_8));
        final HttpRequest<?> getAccessMode = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI);

        getAccessMode.send().onSuccess(response -> {
            final JsonObject responseBody = response.bodyAsJsonObject();
            final JsonObject expected = new JsonObject().put(ResponseJsonKeys.ERROR, Error.NOT_FOUND.toString())
                    .put(ResponseJsonKeys.MESSAGE, LOGGER.getMessage(MessageCodes.AUTH_004, id));

            aContext.verify(() -> {
                assertEquals(HTTP.NOT_FOUND, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, responseBody);

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }
}
