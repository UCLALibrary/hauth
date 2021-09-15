package edu.ucla.library.iiif.auth.handlers;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;

/**
 * A handler for database service errors.
 */
public class DatabaseAccessFailureHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAccessFailureHandler.class,
            MessageCodes.BUNDLE);

    @Override
    public void handle(final RoutingContext aContext) {
        final ServiceException error;
        final HttpServerRequest request;
        final HttpServerResponse response;
        final String responseMessage;

        try {
            error = (ServiceException) aContext.failure();
        } catch (final ClassCastException details) {
            aContext.fail(HTTP.INTERNAL_SERVER_ERROR, details);
            LOGGER.error(MessageCodes.AUTH_010, details);
            return;
        }

        request = aContext.request();
        response = aContext.response();

        switch (error.failureCode()) {
            case DatabaseService.NOT_FOUND_ERROR:
                response.setStatusCode(HTTP.NOT_FOUND);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_004, error.getMessage());
                break;
            case DatabaseService.INTERNAL_ERROR:
            default:
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_005);
                break;
        }
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN.toString()).end(responseMessage);

        LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), responseMessage);
    }
}
