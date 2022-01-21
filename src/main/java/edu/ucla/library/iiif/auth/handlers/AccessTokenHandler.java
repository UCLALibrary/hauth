
package edu.ucla.library.iiif.auth.handlers;

import java.util.Base64;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.TokenJsonKeys;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that handles access token requests for IP-restricted content.
 */
public final class AccessTokenHandler extends AbstractAccessTokenHandler {

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public Future<String> createAccessToken(final RoutingContext aContext) {
        final String clientIpAddress = aContext.request().remoteAddress().hostAddress();
        final Cookie cookie = aContext.request().getCookie(CookieNames.HAUTH);
        final String cookieValue = cookie.getValue();

        return getAccessCookieService().decryptCookie(cookieValue, clientIpAddress).compose(cookieData -> {
            final JsonObject unencodedAccessToken =
                    new JsonObject().put(TokenJsonKeys.VERSION, getConfig().getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.CAMPUS_NETWORK, cookieData.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
            final String accessToken = Base64.getEncoder().encodeToString(unencodedAccessToken.encode().getBytes());

            return Future.succeededFuture(accessToken);
        });
    }
}
