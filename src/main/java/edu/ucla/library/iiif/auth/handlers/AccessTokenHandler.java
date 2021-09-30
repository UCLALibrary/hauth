
package edu.ucla.library.iiif.auth.handlers;

import java.util.Base64;
import java.util.Optional;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
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
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myExpiresIn = Optional.ofNullable(aConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
        myAccessCookieService = AccessCookieService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final String clientIpAddress = aContext.request().remoteAddress().hostAddress();
        final Cookie cookie = aContext.getCookie("iiif-access");
        final String cookieValue = cookie.getValue();
        final HttpServerRequest request = aContext.request();
        final HttpServerResponse response = aContext.response();

        aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());

        myAccessCookieService.decryptCookie(cookieValue, clientIpAddress).onSuccess(cookieData -> {
            final JsonObject data = new JsonObject();
            final JsonObject accessTokenUnencoded =
                    new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.CAMPUS_NETWORK, cookieData.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
            final String accessToken = Base64.getEncoder().encodeToString(accessTokenUnencoded.encode().getBytes());

            data.put(ResponseJsonKeys.ACCESS_TOKEN, accessToken);

            // Token expiry is optional
            myExpiresIn.ifPresent(expiry -> data.put(ResponseJsonKeys.EXPIRES_IN, expiry));

            response.setStatusCode(HTTP.OK).end(data.encodePrettily());
        }).onFailure(failure -> {
            final ServiceException error = (ServiceException) failure;
            final String responseMessage;
            final JsonObject data = new JsonObject();
            final AccessCookieServiceError errorCode = AccessCookieServiceImpl.getError(error);

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
            response.end(data.encodePrettily());

            LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), responseMessage);
        });
    }
}
