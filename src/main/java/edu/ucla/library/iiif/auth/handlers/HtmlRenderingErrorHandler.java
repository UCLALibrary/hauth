
package edu.ucla.library.iiif.auth.handlers;

import java.io.FileNotFoundException;

import com.github.jknack.handlebars.HandlebarsException;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;

/**
 * A handler for HTML rendering errors via Handlebars.
 */
public class HtmlRenderingErrorHandler implements ErrorHandler {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlRenderingErrorHandler.class, MessageCodes.BUNDLE);

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final Throwable error = aContext.failure();

        // First condition is true if syntax errors within the template
        // Second condition is true if template file is not found at the expected path
        if (error instanceof HandlebarsException || error instanceof FileNotFoundException &&
                ((FileNotFoundException) error).getMessage().endsWith(".hbs")) {
            aContext.response() //
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString()) //
                    .setStatusCode(HTTP.INTERNAL_SERVER_ERROR)
                    .end(LOGGER.getMessage(MessageCodes.AUTH_017, request.method(), request.path()));
        } else {
            aContext.next();
        }
    }
}
