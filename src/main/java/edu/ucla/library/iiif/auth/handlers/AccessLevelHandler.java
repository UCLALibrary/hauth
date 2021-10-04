
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles item access level requests.
 */
public class AccessLevelHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLevelHandler.class, MessageCodes.BUNDLE);

    /**
     * The service proxy for accessing the database.
     */
    private final DatabaseService myDatabaseServiceProxy;

    /**
     * Creates a handler that checks the access level of an ID.
     *
     * @param aVertx The Vert.x instance
     */
    public AccessLevelHandler(final Vertx aVertx) {
        myDatabaseServiceProxy = DatabaseService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final String id = aContext.request().getParam(Param.ID);

        myDatabaseServiceProxy.getAccessLevel(id).onSuccess(accessLevel -> {
            final boolean isRestricted = accessLevel == 0 ? false : true;
            final JsonObject data =
                    new JsonObject().put(ResponseJsonKeys.ID, id).put(ResponseJsonKeys.RESTRICTED, isRestricted);

            aContext.response().setStatusCode(HTTP.OK)
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .end(data.encodePrettily());
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
            aContext.fail(HTTP.INTERNAL_SERVER_ERROR, details);
            LOGGER.error(MessageCodes.AUTH_010, details);
            return;
        }

        request = aContext.request();
        response = aContext.response();
        data = new JsonObject();
        errorCode = DatabaseServiceImpl.getError(error);

        switch (errorCode) {
            case NOT_FOUND:
                final String id = error.getMessage();
                response.setStatusCode(HTTP.NOT_FOUND);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_004, id);
                data.put(ResponseJsonKeys.ID, id);
                break;
            case INTERNAL:
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
