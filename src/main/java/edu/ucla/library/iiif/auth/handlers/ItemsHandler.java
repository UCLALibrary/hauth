
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.services.DatabaseServiceError;
import edu.ucla.library.iiif.auth.services.DatabaseServiceImpl;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
        final JsonArray requestData;

        try {
            requestData = aContext.getBodyAsJsonArray();
        } catch (final DecodeException details) {
            final JsonObject error = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, Error.INVALID_JSONARRAY) //
                    .put(ResponseJsonKeys.MESSAGE, details.getMessage());

            aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .setStatusCode(HTTP.BAD_REQUEST).end(error.encodePrettily());
            return;
        }

        myDatabaseServiceProxy.setItems(requestData).onSuccess(result -> {
            aContext.response().setStatusCode(HTTP.CREATED).end();
        }).onFailure(aContext::fail);
    }

    /**
     * Handle failure events sent by {@link #handle}.
     *
     * @param aContext the failure event to handle
     */
    public static void handleFailure(final RoutingContext aContext) {
        final ServiceException error;
        final HttpServerRequest request;
        final HttpServerResponse response;
        final String responseMessage;
        final JsonObject data;
        final DatabaseServiceError errorCode;

        try {
            error = (ServiceException) aContext.failure();
        } catch (final ClassCastException details) {
            aContext.next();
            return;
        }

        request = aContext.request();
        response = aContext.response();
        data = new JsonObject();
        errorCode = DatabaseServiceImpl.getError(error);

        switch (errorCode) {
            case MALFORMED_INPUT_DATA:
                final String msg = error.getMessage();
                response.setStatusCode(HTTP.BAD_REQUEST);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_014, msg);
                break;
            case INTERNAL_ERROR:
            default:
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_005);
                break;
        }

        data.put(ResponseJsonKeys.ERROR, errorCode).put(ResponseJsonKeys.MESSAGE, responseMessage);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).end(data.encodePrettily());

        LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), responseMessage);
    }
}
