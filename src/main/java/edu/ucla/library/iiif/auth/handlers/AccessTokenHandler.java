
package edu.ucla.library.iiif.auth.handlers;

import java.util.Base64;
import java.util.Optional;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.TokenJsonKeys;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.AccessCookieServiceError;
import edu.ucla.library.iiif.auth.services.AccessCookieServiceImpl;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles access token requests.
 */
public class AccessTokenHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenHandler.class, MessageCodes.BUNDLE);

    /**
     * The application configuration.
     */
    private final JsonObject myConfig;

    /**
     * See {@link Config#ACCESS_TOKEN_EXPIRES_IN}.
     */
    private final Optional<Integer> myExpiresIn;

    /**
     * The service proxy for accessing the secret key.
     */
    private final AccessCookieService myAccessCookieService;

    /**
     * The template engine for rendering the response.
     */
    private final HandlebarsTemplateEngine myHtmlTemplateEngine;

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myExpiresIn = Optional.ofNullable(aConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
        myAccessCookieService = AccessCookieService.createProxy(aVertx);
        myHtmlTemplateEngine = HandlebarsTemplateEngine.create(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final String clientIpAddress = aContext.request().remoteAddress().hostAddress();
        final Cookie cookie = aContext.request().getCookie("iiif-access");
        final String cookieValue = cookie.getValue();

        myAccessCookieService.decryptCookie(cookieValue, clientIpAddress).onSuccess(cookieData -> {
            final String messageID = aContext.request().getParam(Param.MESSAGE_ID);
            final String origin = aContext.request().getParam(Param.ORIGIN);
            final JsonObject jsonWrapper = new JsonObject();
            final JsonObject accessTokenUnencoded =
                    new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.CAMPUS_NETWORK, cookieData.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
            final String accessToken = Base64.getEncoder().encodeToString(accessTokenUnencoded.encode().getBytes());
            // Unless e.g. the Handlebars template rendering fails, we'll return HTTP 200
            final HttpServerResponse response = aContext.response().setStatusCode(HTTP.OK);

            jsonWrapper.put(ResponseJsonKeys.ACCESS_TOKEN, accessToken);

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
