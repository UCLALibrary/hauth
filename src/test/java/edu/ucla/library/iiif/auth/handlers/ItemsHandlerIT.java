
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.RequestJsonKeys;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.handlers.AccessModeHandler.AccessMode;
import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import io.vertx.core.CompositeFuture;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ItemsHandler#handle}.
 */
public final class ItemsHandlerIT extends AbstractHandlerIT {

    /**
     * A test ID.
     */
    private static final String TEST_ID_1 = "ark:/12345/00000000";

    /**
     * A test ID.
     */
    private static final String TEST_ID_2 = "ark:/12345/11111111";

    /**
     * Example valid JSON representation of an open access item.
     */
    private static final JsonObject TEST_ITEM_OPEN_ACCESS = new JsonObject() //
            .put(RequestJsonKeys.UID, TEST_ID_1) //
            .put(RequestJsonKeys.ACCESS_MODE, 0);

    /**
     * Example valid JSON representation of a tiered access item.
     */
    private static final JsonObject TEST_ITEM_TIERED_ACCESS = new JsonObject() //
            .put(RequestJsonKeys.UID, TEST_ID_2) //
            .put(RequestJsonKeys.ACCESS_MODE, 1);

    /**
     * Example invalid JSON representation of an item (missing access mode).
     */
    private static final JsonObject TEST_ITEM_MISSING_ACCESS_MODE = new JsonObject() //
            .put(RequestJsonKeys.UID, TEST_ID_1);

    /**
     * An HTTP header for making authorized requests.
     */
    private static final MultiMap API_KEY_HEADER =
            MultiMap.caseInsensitiveMultiMap().add("X-API-KEY", System.getenv(Config.API_KEY));

    /**
     * Tests that items can be POSTed.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostItems(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<?> postItems =
                myWebClient.post(myPort, TestConstants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_TIERED_ACCESS);

        postItems.sendJson(json).compose(response -> {
            final String getAccessModeRequestUri1 =
                    StringUtils.format(GET_ACCESS_MODE_PATH, URLEncoder.encode(TEST_ID_1, StandardCharsets.UTF_8));
            final String getAccessModeRequestUri2 =
                    StringUtils.format(GET_ACCESS_MODE_PATH, URLEncoder.encode(TEST_ID_2, StandardCharsets.UTF_8));

            final HttpRequest<?> getAccessMode1 =
                    myWebClient.get(myPort, TestConstants.INADDR_ANY, getAccessModeRequestUri1);
            final HttpRequest<?> getAccessMode2 =
                    myWebClient.get(myPort, TestConstants.INADDR_ANY, getAccessModeRequestUri2);

            assertEquals(HTTP.CREATED, response.statusCode());

            return CompositeFuture.all(getAccessMode1.send(), getAccessMode2.send());
        }).onSuccess(responses -> {
            final HttpResponse<?> response1 = responses.<HttpResponse<?>>resultAt(0);
            final HttpResponse<?> response2 = responses.<HttpResponse<?>>resultAt(1);

            final JsonObject expected1 = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, AccessMode.OPEN);
            final JsonObject expected2 = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, AccessMode.TIERED);

            assertEquals(HTTP.OK, response1.statusCode());
            assertEquals(HTTP.OK, response2.statusCode());

            assertEquals(MediaType.APPLICATION_JSON.toString(), response1.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(MediaType.APPLICATION_JSON.toString(), response2.headers().get(HttpHeaders.CONTENT_TYPE));

            assertEquals(expected1, response1.bodyAsJsonObject());
            assertEquals(expected2, response2.bodyAsJsonObject());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that an unauthorized request will fail.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostItemsUnauthorized(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<?> postItems = myWebClient.post(myPort, TestConstants.INADDR_ANY, POST_ITEMS_PATH);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_TIERED_ACCESS);

        postItems.sendJson(json).onSuccess(response -> {
            assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(Error.INVALID_ADMIN_CREDENTIALS.toString(),
                    response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the request body must be a JSON array, not a JSON object (or anything else).
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostItemsInvalidRequestBody(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<?> postItems =
                myWebClient.post(myPort, TestConstants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);

        postItems.sendJson(TEST_ITEM_OPEN_ACCESS).onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(Error.INVALID_JSONARRAY.toString(),
                    response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the request is rejected if any of the items are missing a key.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostItemsMissingJsonKey(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<?> postItems =
                myWebClient.post(myPort, TestConstants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_MISSING_ACCESS_MODE);

        postItems.sendJson(json).onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(Error.MALFORMED_INPUT_DATA.toString(),
                    response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
