
package edu.ucla.library.iiif.auth.handlers;

import java.util.Map;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles items requests.
 */
public class ItemsHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsHandler.class, MessageCodes.BUNDLE);

    /**
     * A mapping of the expected potential errors to their status codes. This is something that could potentially be a
     * part of the Error class (if they're consistent), but keeping it simple for now with this isolated map.
     */
    private static final Map<Error, Integer> ERRORS = Map.of(Error.MALFORMED_INPUT_DATA, HTTP.BAD_REQUEST,
            Error.INVALID_JSONARRAY, HTTP.BAD_REQUEST, Error.MISSING_BODY_HANDLER, HTTP.INTERNAL_SERVER_ERROR);

    /**
     * The service proxy for accessing the database.
     */
    private final DatabaseService myDatabaseServiceProxy;

    /**
     * Creates a handler that adds items to the database.
     *
     * @param aVertx The Vert.x instance
     */
    public ItemsHandler(final Vertx aVertx) {
        myDatabaseServiceProxy = DatabaseService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final RequestBody body = aContext.body();

        if (body.available()) {
            try {
                myDatabaseServiceProxy.setItems(body.asJsonArray()).onSuccess(result -> {
                    aContext.response().setStatusCode(HTTP.CREATED).end();
                }).onFailure(error -> {
                    if (error instanceof ServiceException) {
                        final ServiceException details = (ServiceException) error;
                        final int failureCode = details.failureCode();

                        if (failureCode == Error.MALFORMED_INPUT_DATA.ordinal()) {
                            final String errorMessage = LOGGER.getMessage(MessageCodes.AUTH_014, details.getMessage());
                            sendError(aContext, Error.MALFORMED_INPUT_DATA, errorMessage);
                        } else {
                            sendError(aContext, Error.values()[failureCode], LOGGER.getMessage(MessageCodes.AUTH_005));
                        }
                    } else {
                        aContext.fail(error);
                    }
                });
            } catch (final ClassCastException details) {
                sendError(aContext, Error.INVALID_JSONARRAY, LOGGER.getMessage(MessageCodes.AUTH_021, body.asString()));
            }
        } else {
            sendError(aContext, Error.MISSING_BODY_HANDLER, LOGGER.getMessage(MessageCodes.AUTH_002));
        }
    }

    /**
     * Sends an error for an unsuccessful request.
     *
     * @param aContext The handler's routing context
     * @param aError An error type
     * @param aMessage An error message
     */
    private void sendError(final RoutingContext aContext, final Error aError, final String aMessage) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final JsonObject details = new JsonObject();

        details.put(ResponseJsonKeys.ERROR, aError);
        details.put(ResponseJsonKeys.MESSAGE, aMessage);

        LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), aMessage);

        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
        response.setStatusCode(ERRORS.getOrDefault(aError, HTTP.INTERNAL_SERVER_ERROR)).end(details.encodePrettily());
    }
}
