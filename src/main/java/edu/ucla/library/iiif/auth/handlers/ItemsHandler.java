
package edu.ucla.library.iiif.auth.handlers;

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
        final HttpServerRequest request = aContext.request();
        final JsonArray requestData;
        final HttpServerResponse response = aContext.response() //
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
        final JsonObject responseData;

        try {
            requestData = aContext.getBodyAsJsonArray();
        } catch (final DecodeException details) {
            responseData = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, Error.INVALID_JSONARRAY) //
                    .put(ResponseJsonKeys.MESSAGE, details.getMessage());
            response.setStatusCode(HTTP.BAD_REQUEST).end(responseData.encodePrettily());

            LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), details.getMessage());
            return;
        }

        myDatabaseServiceProxy.setItems(requestData).onSuccess(result -> {
            response.setStatusCode(HTTP.CREATED).end();
        }).onFailure(error -> {
            if (error instanceof ServiceException) {
                final ServiceException details = (ServiceException) error;
                final int statusCode;
                final String errorMessage;
                final JsonObject errorData;

                if (details.failureCode() == Error.MALFORMED_INPUT_DATA.ordinal()) {
                    statusCode = HTTP.BAD_REQUEST;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_014, error.getMessage());
                } else {
                    statusCode = HTTP.INTERNAL_SERVER_ERROR;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_005);
                }

                errorData = new JsonObject() //
                        .put(ResponseJsonKeys.ERROR, Error.values()[details.failureCode()]) //
                        .put(ResponseJsonKeys.MESSAGE, errorMessage);

                response.setStatusCode(statusCode).end(errorData.encodePrettily());

                LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), details.getMessage());
            } else {
                aContext.fail(error);
            }
        });
    }
}
