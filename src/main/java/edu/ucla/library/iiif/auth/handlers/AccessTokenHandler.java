package edu.ucla.library.iiif.auth.handlers;

import java.util.Base64;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TokenJsonKeys;
import edu.ucla.library.iiif.auth.services.AccessCookieCryptoService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import info.freelibrary.util.HTTP;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles access token requests.
 */
public class AccessTokenHandler implements Handler<RoutingContext> {

    /**
     * The application configuration.
     */
    private final JsonObject myConfig;

    /**
     * The service proxy for accessing the secret key.
     */
    private final AccessCookieCryptoService myAccessCookieCryptoService;

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myAccessCookieCryptoService = AccessCookieCryptoService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final String clientIpAddress = aContext.request().remoteAddress().hostAddress();
        final Cookie cookie = aContext.getCookie("iiif-access");
        final String cookieValue = cookie.getValue();

        aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());

        myAccessCookieCryptoService.decryptCookie(cookieValue).onSuccess(cookieData -> {
            // if the IP addresses match, send back the access token
            if (clientIpAddress.equals(cookieData.getString(CookieJsonKeys.CLIENT_IP_ADDRESS))) {
                // FIXME: make expiration configurable
                final JsonObject accessTokenUnencoded = new JsonObject()
                        .put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                        .put(TokenJsonKeys.CAMPUS_NETWORK, cookieData.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
                final String accessToken = Base64.getEncoder().encodeToString(accessTokenUnencoded.encode().getBytes());
                final JsonObject data = new JsonObject().put(ResponseJsonKeys.ACCESS_TOKEN, accessToken)
                        .put(ResponseJsonKeys.EXPIRES_IN, 3600);

                aContext.response().setStatusCode(HTTP.OK).end(data.encodePrettily());
            } else {
                // TODO: JSON response
                aContext.response().setStatusCode(HTTP.BAD_REQUEST).end();
            }
        }).onFailure(failure -> {
            final ServiceException error = (ServiceException) failure;
            final HttpServerResponse response = aContext.response();

            switch (error.failureCode()) {
                case AccessCookieCryptoService.TAMPERED_COOKIE_ERROR:
                    response.setStatusCode(HTTP.BAD_REQUEST);
                    break;
                case AccessCookieCryptoService.CONFIGURATION_ERROR:
                default:
                    response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    break;
            }
            response.end();
            // TODO: JSON response
        });
    }
}
