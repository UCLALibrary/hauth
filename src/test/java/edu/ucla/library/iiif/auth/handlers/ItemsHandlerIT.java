
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.csveed.api.CsvClient;
import org.csveed.api.CsvClientImpl;
import org.csveed.api.Row;
import org.csveed.report.CsvException;

import org.junit.jupiter.api.Test;

import info.freelibrary.util.Constants;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.RequestJsonKeys;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.handlers.AccessModeHandler.AccessMode;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ItemsHandler#handle}.
 */
public final class ItemsHandlerIT extends AbstractHandlerIT {

    /**
     * The test's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsHandlerIT.class, MessageCodes.BUNDLE);

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
                myWebClient.post(myPort, Constants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_TIERED_ACCESS);
        final Checkpoint checkpoint = aContext.checkpoint(2); // Two separate asynchronous checkpoints

        postItems.sendJson(json).compose(response -> {
            final String getAccessModeRequestUri1 =
                    StringUtils.format(GET_ACCESS_MODE_PATH, URLEncoder.encode(TEST_ID_1, StandardCharsets.UTF_8));
            final String getAccessModeRequestUri2 =
                    StringUtils.format(GET_ACCESS_MODE_PATH, URLEncoder.encode(TEST_ID_2, StandardCharsets.UTF_8));

            final HttpRequest<?> getAccessMode1 =
                    myWebClient.get(myPort, Constants.INADDR_ANY, getAccessModeRequestUri1);
            final HttpRequest<?> getAccessMode2 =
                    myWebClient.get(myPort, Constants.INADDR_ANY, getAccessModeRequestUri2);

            aContext.verify(() -> {
                assertEquals(HTTP.CREATED, response.statusCode());
                checkpoint.flag();
            });

            return CompositeFuture.all(getAccessMode1.send(), getAccessMode2.send());
        }).onSuccess(responses -> {
            final HttpResponse<?> response1 = responses.<HttpResponse<?>>resultAt(0);
            final HttpResponse<?> response2 = responses.<HttpResponse<?>>resultAt(1);

            final JsonObject expected1 = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, AccessMode.OPEN);
            final JsonObject expected2 = new JsonObject().put(ResponseJsonKeys.ACCESS_MODE, AccessMode.TIERED);

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response1.statusCode());
                assertEquals(HTTP.OK, response2.statusCode());

                assertEquals(MediaType.APPLICATION_JSON.toString(), response1.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(MediaType.APPLICATION_JSON.toString(), response2.headers().get(HttpHeaders.CONTENT_TYPE));

                assertEquals(expected1, response1.bodyAsJsonObject());
                assertEquals(expected2, response2.bodyAsJsonObject());

                checkpoint.flag();
            });
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
        final HttpRequest<?> postItems = myWebClient.post(myPort, Constants.INADDR_ANY, POST_ITEMS_PATH);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_TIERED_ACCESS);

        postItems.sendJson(json).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(Error.INVALID_ADMIN_CREDENTIALS.toString(),
                        response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

                aContext.completeNow();
            });
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
                myWebClient.post(myPort, Constants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);

        postItems.sendJson(TEST_ITEM_OPEN_ACCESS).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(Error.INVALID_JSONARRAY.toString(),
                        response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the request body must be non-empty.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostItemsMissingRequestBody(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<?> postItems =
                myWebClient.post(myPort, Constants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);

        postItems.send().onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(Error.INVALID_JSONARRAY.toString(),
                        response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

                aContext.completeNow();
            });
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
                myWebClient.post(myPort, Constants.INADDR_ANY, POST_ITEMS_PATH).putHeaders(API_KEY_HEADER);
        final JsonArray json = new JsonArray().add(TEST_ITEM_OPEN_ACCESS).add(TEST_ITEM_MISSING_ACCESS_MODE);

        postItems.sendJson(json).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(Error.MALFORMED_INPUT_DATA.toString(),
                        response.bodyAsJsonObject().getString(ResponseJsonKeys.ERROR));

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that import-items.py (run during the integration-test build phase) worked as intended.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testImportItemsScript(final Vertx aVertx, final VertxTestContext aContext) {
        final Stream<Path> csvPaths;
        final Stream<Future<Void>> csvChecks;

        try {
            csvPaths = Files.walk(Path.of("src/test/resources/csv/")).filter(path -> {
                // Filter out directories; assume regular files are CSV
                return path.toFile().isFile();
            });
        } catch (final IOException | SecurityException details) {
            aContext.failNow(details);
            return;
        }

        csvChecks = csvPaths.map(csvPath -> {
            return aVertx.fileSystem().readFile(csvPath.toString()).compose(data -> {
                final CsvClient<Row> client =
                        new CsvClientImpl<Row>(new StringReader(data.toString())).setSeparator(',').setUseHeader(true);
                final Collection<Row> rows;
                final Stream<Future<Void>> rowChecks;

                try {
                    rows = client.readRows();
                } catch (final CsvException details) {
                    return Future.failedFuture(details);
                }

                rowChecks = rows.parallelStream().map(row -> {
                    // Query Hauth for the access mode of each item and compare value with Visibility field value
                    final String itemARK = row.get("Item ARK");
                    final String visibility = row.get("Visibility");
                    final String requestURI = StringUtils.format(GET_ACCESS_MODE_PATH,
                            URLEncoder.encode(itemARK, StandardCharsets.UTF_8));
                    final HttpRequest<?> getAccessMode = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI);

                    return getExpectedAccessMode(visibility).compose(expectedAccessMode -> {
                        return getAccessMode.send().compose(response -> {
                            final AccessMode actualAccessMode = AccessMode
                                    .valueOf(response.bodyAsJsonObject().getString(ResponseJsonKeys.ACCESS_MODE));

                            if (expectedAccessMode.equals(actualAccessMode)) {
                                return Future.succeededFuture();
                            } else {
                                return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_023, expectedAccessMode,
                                        itemARK, visibility, actualAccessMode));
                            }
                        });
                    });
                });

                return CompositeFuture.all(rowChecks.collect(Collectors.toList())).mapEmpty();
            });
        });

        CompositeFuture.all(csvChecks.collect(Collectors.toList())).onSuccess(nil -> aContext.completeNow())
                .onFailure(aContext::failNow);
    }

    /**
     *  Tests that the given visibility is a valid access mode.
     * 
     * @param aVisibility A Visibility field value
     * @return A Future that succeeds if there is a mapping from the provided visibility to an access mode, else fails
     */
    private static Future<AccessMode> getExpectedAccessMode(final String aVisibility) {
        return switch (aVisibility) {
            case "open" -> Future.succeededFuture(AccessMode.OPEN);
            case "ucla" -> Future.succeededFuture(AccessMode.TIERED);
            case "private" -> Future.succeededFuture(AccessMode.TIERED);
            case "sinai" -> Future.succeededFuture(AccessMode.ALL_OR_NOTHING);
            default -> Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_023, aVisibility));
        };
    }
}
