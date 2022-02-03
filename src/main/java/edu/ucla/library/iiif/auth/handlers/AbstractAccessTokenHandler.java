
package edu.ucla.library.iiif.auth.handlers;

import java.util.Optional;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.AccessCookieServiceError;
import edu.ucla.library.iiif.auth.services.AccessCookieServiceImpl;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.serviceproxy.ServiceException;

/**
 * An abstract base class for access token request handlers.
 */
public abstract class AbstractAccessTokenHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAccessTokenHandler.class, MessageCodes.BUNDLE);

    /**
     * The application configuration.
     */
    protected final JsonObject myConfig;

    /**
     * See {@link Config#ACCESS_TOKEN_EXPIRES_IN}.
     */
    protected final Optional<Integer> myExpiresIn;

    /**
     * The service proxy for accessing the secret key.
     */
    protected final AccessCookieService myAccessCookieService;

    /**
     * The template engine for rendering the response.
     */
    protected final HandlebarsTemplateEngine myHtmlTemplateEngine;

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AbstractAccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myExpiresIn = Optional.ofNullable(aConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
        myAccessCookieService = AccessCookieService.createProxy(aVertx);
        myHtmlTemplateEngine = HandlebarsTemplateEngine.create(aVertx);
    }

    /**
     * Gets the configuration.
     *
     * @return The configuration
     */
    public JsonObject getConfig() {
        return myConfig;
    }

    /**
     * Gets the access cookie service.
     *
     * @return The access cookie service
     */
    public AccessCookieService getAccessCookieService() {
        return myAccessCookieService;
    }

    @Override
    public final void handle(final RoutingContext aContext) {
        createAccessToken(aContext).onSuccess(token -> {
            // Unless e.g. the Handlebars template rendering fails, we'll return HTTP 200
            final HttpServerResponse response = aContext.response().setStatusCode(HTTP.OK);
            final JsonObject jsonWrapper = new JsonObject().put(ResponseJsonKeys.ACCESS_TOKEN, token);
            final String messageID = aContext.request().getParam(Param.MESSAGE_ID);
            final String origin = aContext.request().getParam(Param.ORIGIN);

            // Token expiry is optional
            myExpiresIn.ifPresent(expiry -> jsonWrapper.put(ResponseJsonKeys.EXPIRES_IN, expiry));

            if (messageID != null && origin != null) {
                // Browser-based client
                final JsonObject templateData = new JsonObject();

                jsonWrapper.put(ResponseJsonKeys.MESSAGE_ID, messageID);
                templateData.put(TemplateKeys.ORIGIN, origin).put(TemplateKeys.ACCESS_TOKEN_OBJECT, jsonWrapper);

                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString());

                myHtmlTemplateEngine.render(templateData, "templates/token.hbs").onSuccess(html -> {
                    response.end(html);
                }).onFailure(details -> {
                    final JsonObject error = new JsonObject().put(ResponseJsonKeys.ERROR, Error.HTML_RENDERING_ERROR)
                            .put(ResponseJsonKeys.MESSAGE, details.getMessage());

                    response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                            .setStatusCode(HTTP.INTERNAL_SERVER_ERROR).end(error.encodePrettily());
                });
            } else {
                // Non browser-based client
                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                        .end(jsonWrapper.encodePrettily());
            }
        }).onFailure(aContext::fail);
    }

    /**
     * Creates a context-dependent access token.
     *
     * @param aContext A routing context
     * @return A Future that resolves to the access token value
     */
    protected abstract Future<String> createAccessToken(RoutingContext aContext);

    /**
     * Handle failure events sent by {@link #handle}.
     *
     * @param aContext the failure event to handle
     */
    public static final void handleFailure(final RoutingContext aContext) {
        final ServiceException error;
        final HttpServerRequest request;
        final HttpServerResponse response;
        final String responseMessage;
        final JsonObject data;
        final AccessCookieServiceError errorCode;

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
        errorCode = AccessCookieServiceImpl.getError(error);

        switch (errorCode) {
            case INVALID_COOKIE:
                response.setStatusCode(HTTP.BAD_REQUEST);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_011);
                break;
            case CONFIGURATION:
            default:
                response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                responseMessage = LOGGER.getMessage(MessageCodes.AUTH_012);
                break;
        }
        data.put(ResponseJsonKeys.ERROR, errorCode).put(ResponseJsonKeys.MESSAGE, responseMessage);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).end(data.encodePrettily());

        LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), responseMessage);
    }
}