
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.HttpException;

/**
 * Handles admin API authentication errors.
 */
public class AdminAuthenticationErrorHandler implements ErrorHandler {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminAuthenticationErrorHandler.class, MessageCodes.BUNDLE);

    @Override
    public void handle(final RoutingContext aContext) {
        final Throwable error = aContext.failure();

        if (error instanceof HttpException && ((HttpException) error).getStatusCode() == HTTP.UNAUTHORIZED) {
            final JsonObject errorBody = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, Error.INVALID_ADMIN_CREDENTIALS) //
                    .put(ResponseJsonKeys.MESSAGE, LOGGER.getMessage(MessageCodes.AUTH_016));

            aContext.response() //
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()) //
                    .setStatusCode(HTTP.UNAUTHORIZED) //
                    .end(errorBody.encodePrettily());
        } else {
            LOGGER.error(MessageCodes.AUTH_010, error.toString());
        }
    }
}
