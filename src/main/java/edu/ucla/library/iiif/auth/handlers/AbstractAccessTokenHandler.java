
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
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
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
        final String messageID = aContext.request().getParam(Param.MESSAGE_ID);
        final String origin = aContext.request().getParam(Param.ORIGIN);
        final boolean isBrowserClient = messageID != null && origin != null;
        final MediaType responseContentType = isBrowserClient ? MediaType.TEXT_HTML : MediaType.APPLICATION_JSON;
        final HttpServerResponse response =
                aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, responseContentType.toString());

        createAccessToken(aContext).compose(token -> {
            final JsonObject jsonWrapper = new JsonObject().put(ResponseJsonKeys.ACCESS_TOKEN, token);

            // Unless e.g. the Handlebars template rendering fails, we'll return HTTP 200
            response.setStatusCode(HTTP.OK);

            // Token expiry is optional
            myExpiresIn.ifPresent(expiry -> jsonWrapper.put(ResponseJsonKeys.EXPIRES_IN, expiry));

            if (isBrowserClient) {
                // Browser-based client
                final JsonObject templateData = new JsonObject();

                jsonWrapper.put(ResponseJsonKeys.MESSAGE_ID, messageID);
                templateData.put(TemplateKeys.ORIGIN, origin).put(TemplateKeys.ACCESS_TOKEN_OBJECT, jsonWrapper);

                response.putHeader(HttpHeaders.CONTENT_TYPE, responseContentType.toString());

                return myHtmlTemplateEngine.render(templateData, "templates/token.hbs");
            }

            // Non browser-based client
            return Future.succeededFuture(jsonWrapper.toBuffer());
        }).onSuccess(response::end).onFailure(error -> {
            if (error instanceof ServiceException) {
                final ServiceException details = (ServiceException) error;
                final int statusCode;
                final String errorMessage;

                if (details.failureCode() == Error.INVALID_COOKIE.ordinal()) {
                    statusCode = HTTP.BAD_REQUEST;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_012);
                } else {
                    statusCode = HTTP.INTERNAL_SERVER_ERROR;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_018);
                }

                response.setStatusCode(statusCode);

                if (isBrowserClient) {
                    response.end(errorMessage);
                } else {
                    final JsonObject errorData = new JsonObject() //
                            .put(ResponseJsonKeys.ERROR, Error.values()[details.failureCode()]) //
                            .put(ResponseJsonKeys.MESSAGE, errorMessage);

                    response.end(errorData.encodePrettily());
                }
            } else {
                aContext.next();
            }
        });
    }

    /**
     * Creates a context-dependent access token.
     *
     * @param aContext A routing context
     * @return A Future that resolves to the access token value
     */
    protected abstract Future<String> createAccessToken(RoutingContext aContext);
}
