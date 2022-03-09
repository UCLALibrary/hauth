
package edu.ucla.library.iiif.auth.handlers;

import java.util.Base64;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.TokenJsonKeys;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that handles access token requests for Sinai affiliate-restricted content.
 */
public class SinaiAccessTokenHandler extends AbstractAccessTokenHandler {

    /**
     * Creates a handler that exchanges access cookies for access tokens.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public SinaiAccessTokenHandler(final Vertx aVertx, final JsonObject aConfig) {
        super(aVertx, aConfig);
    }

    @Override
    public Future<String> createAccessToken(final RoutingContext aContext) {
        // The latter cookie stores the initialization vector for decrypting the former
        final String authCookie = aContext.request().getCookie(CookieNames.SINAI_CIPHERTEXT).getValue();
        final String ivCookie = aContext.request().getCookie(CookieNames.SINAI_IV).getValue();

        return getAccessCookieService().validateSinaiCookie(authCookie, ivCookie).compose(unused -> {
            final JsonObject unencodedAccessToken =
                    new JsonObject().put(TokenJsonKeys.VERSION, getConfig().getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.SINAI_AFFILIATE, true);
            final String accessToken = Base64.getEncoder().encodeToString(unencodedAccessToken.encode().getBytes());

            return Future.succeededFuture(accessToken);
        });
    }
}
