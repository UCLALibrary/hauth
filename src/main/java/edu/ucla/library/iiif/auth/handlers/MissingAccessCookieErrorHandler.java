
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.AccessTokenError;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.ext.web.validation.ParameterProcessorException;

/**
 * Handles errors resulting from access token requests that don't include all the required access cookies.
 */
public class MissingAccessCookieErrorHandler implements ErrorHandler {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MissingAccessCookieErrorHandler.class, MessageCodes.BUNDLE);

    /**
     * The template engine for rendering the response.
     */
    private final HandlebarsTemplateEngine myHtmlTemplateEngine;

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     */
    public MissingAccessCookieErrorHandler(final Vertx aVertx) {
        myHtmlTemplateEngine = HandlebarsTemplateEngine.create(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final Throwable error = aContext.failure();
        final HttpServerRequest request = aContext.request();

        if (error instanceof ParameterProcessorException && anyRequiredCookiesMissing(request)) {
            final String messageID = request.getParam(Param.MESSAGE_ID);
            final String origin = request.getParam(Param.ORIGIN);
            final boolean isBrowserClient = messageID != null && origin != null;
            final MediaType responseContentType = isBrowserClient ? MediaType.TEXT_HTML : MediaType.APPLICATION_JSON;
            final HttpServerResponse response =
                    aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, responseContentType.toString());
            final JsonObject jsonWrapper = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.missingCredentials);

            if (isBrowserClient) {
                final JsonObject templateData = new JsonObject() //
                        .put(TemplateKeys.ORIGIN, origin) //
                        .put(TemplateKeys.ACCESS_TOKEN_OBJECT, jsonWrapper);

                myHtmlTemplateEngine.render(templateData, "templates/token.hbs").onSuccess(html -> {
                    response.setStatusCode(HTTP.OK).end(html);
                }).onFailure(aContext::fail);
            } else {
                response.setStatusCode(HTTP.BAD_REQUEST).end(jsonWrapper.encodePrettily());
            }

            LOGGER.debug(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), error.getMessage());
        } else {
            aContext.next();
            LOGGER.error(MessageCodes.AUTH_010, error.toString());
        }
    }

    /**
     * Determines whether any required cookies are missing from request (assuming it's an access token request).
     *
     * @param aRequest The request
     * @return true if it's an access token request and is missing cookies, false otherwise
     */
    private boolean anyRequiredCookiesMissing(final HttpServerRequest aRequest) {
        final String requestPath = aRequest.path();

        if (requestPath.startsWith("/token/sinai")) {
            return aRequest.getCookie(CookieNames.SINAI_CIPHERTEXT) == null ||
                    aRequest.getCookie(CookieNames.SINAI_IV) == null;
        }

        if (requestPath.startsWith("/token")) {
            return aRequest.getCookie(CookieNames.HAUTH) == null;
        }

        return false;
    }
}
