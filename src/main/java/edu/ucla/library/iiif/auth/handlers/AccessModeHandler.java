
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles item access mode requests.
 */
public class AccessModeHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessModeHandler.class, MessageCodes.BUNDLE);

    /**
     * The service proxy for accessing the database.
     */
    private final DatabaseService myDatabaseServiceProxy;

    /**
     * Creates a handler that checks the access mode of an ID.
     *
     * @param aVertx The Vert.x instance
     */
    public AccessModeHandler(final Vertx aVertx) {
        myDatabaseServiceProxy = DatabaseService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final String id = request.getParam(Param.ID);
        final HttpServerResponse response = aContext.response() //
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());

        myDatabaseServiceProxy.getAccessMode(id).onSuccess(accessMode -> {
            final JsonObject responseData = new JsonObject() //
                    .put(ResponseJsonKeys.ACCESS_MODE, AccessMode.values()[accessMode]);

            response.setStatusCode(HTTP.OK).end(responseData.encodePrettily());
        }).onFailure(error -> {
            if (error instanceof ServiceException) {
                final ServiceException details = (ServiceException) error;
                final int statusCode;
                final String errorMessage;
                final JsonObject errorData;

                if (details.failureCode() == Error.NOT_FOUND.ordinal()) {
                    statusCode = HTTP.NOT_FOUND;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_004, id);
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

    /**
     * Expected access mode values: OPEN, TIERED, or ALL_OR_NOTHING. These determine the level of access a requested
     * item is allowed.
     *
     * @see <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">Interaction with
     *      Access-Controlled Resources </a>
     */
    public enum AccessMode {
        OPEN, TIERED, ALL_OR_NOTHING;
    }
}
