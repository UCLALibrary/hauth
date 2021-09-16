package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessLevelHandler#handle}.
 */
public final class AccessLevelHandlerIT extends AbstractHandlerIT {

    /**
     * A URI template for access level requests.
     */
    private static final String GET_ACCESS_URI_TEMPLATE = "/access/{}";

    /**
     * Tests that a client can get an item's access level.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessLevel(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestUri = StringUtils.format(GET_ACCESS_URI_TEMPLATE,
                URLEncoder.encode(TEST_ID, StandardCharsets.UTF_8));

        myWebClient.get(myPort, TestConstants.INADDR_ANY, requestUri).send(get -> {
            if (get.succeeded()) {
                final HttpResponse<?> response = get.result();
                final JsonObject expected = new JsonObject().put(Param.ID, TEST_ID).put("restricted", false);

                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsJsonObject());

                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }

    /**
     * Tests that a client gets the expected error response when requesting the access level of an unknown item.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetAccessLevelUnknownItem(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestUri = StringUtils.format(GET_ACCESS_URI_TEMPLATE,
                URLEncoder.encode("ark:/21198/unknown", StandardCharsets.UTF_8));

        myWebClient.get(myPort, TestConstants.INADDR_ANY, requestUri).send(get -> {
            if (get.succeeded()) {
                final HttpResponse<?> response = get.result();

                assertEquals(HTTP.NOT_FOUND, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(DatabaseService.NOT_FOUND_ERROR, response.bodyAsJsonObject().getInteger("error"));
                assertTrue(response.bodyAsJsonObject().containsKey("message"));

                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }
}
