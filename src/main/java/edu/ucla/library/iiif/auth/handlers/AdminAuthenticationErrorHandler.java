
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
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

        if (error instanceof HttpException) {
            final HttpException details = (HttpException) error;
            final int statusCode = details.getStatusCode();

            final HttpServerResponse response =
                    aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
            final JsonObject responseBody = new JsonObject();

            response.setStatusCode(statusCode);

            if (statusCode == HTTP.UNAUTHORIZED) {
                responseBody.put(ResponseJsonKeys.ERROR, Error.INVALID_ADMIN_CREDENTIALS).put(ResponseJsonKeys.MESSAGE,
                        LOGGER.getMessage(MessageCodes.AUTH_016));
            } else {
                responseBody.put(ResponseJsonKeys.ERROR, StringUtils.format("HTTP {}", statusCode))
                        .put(ResponseJsonKeys.MESSAGE, details.getMessage());
            }

            response.end(responseBody.encodePrettily());
        } else {
            aContext.next();
        }
    }
}
