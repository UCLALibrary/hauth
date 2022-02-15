
package edu.ucla.library.iiif.auth;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;

/**
 * An admin API authentication provider.
 */
public class AdminAuthenticationProvider implements AuthenticationProvider {

    /**
     * The the authentication provider's logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminAuthenticationProvider.class, MessageCodes.BUNDLE);

    /**
     * The application configuration.
     */
    private final JsonObject myConfig;

    /**
     * Creates an authentication provider for admin APIs.
     *
     * @param aConfig A configuration
     */
    public AdminAuthenticationProvider(final JsonObject aConfig) {
        myConfig = aConfig;
    }

    @Override
    public void authenticate(final JsonObject aCredentialsJson, final Handler<AsyncResult<User>> aResultHandler) {
        final String apiKey = new TokenCredentials(aCredentialsJson).getToken();

        if (myConfig.getValue(Config.API_KEY).equals(apiKey)) {
            aResultHandler.handle(Future.succeededFuture(User.fromToken(apiKey)));
        } else {
            aResultHandler.handle(Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_016)));
        }
    }

}
