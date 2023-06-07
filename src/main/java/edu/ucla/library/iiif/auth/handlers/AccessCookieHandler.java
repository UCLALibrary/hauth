
package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.COMMA;

import java.net.URI;
import java.util.Optional;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.veqryn.collect.Cidr4Trie;
import com.github.veqryn.collect.Trie;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ip4;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.serviceproxy.ServiceException;

/**
 * Handler that handles access cookie requests.
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class AccessCookieHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessCookieHandler.class, MessageCodes.BUNDLE);

    /**
     * The application configuration.
     */
    private final JsonObject myConfig;

    /**
     * The template engine for rendering the response.
     */
    private final HandlebarsTemplateEngine myHtmlTemplateEngine;

    /**
     * A {@link Trie} of Campus Network subnets.
     */
    private final Cidr4Trie<String> myCampusNetworkSubnets;

    /**
     * A service for generating and decrypting encrypted access cookies.
     */
    private final AccessCookieService myAccessCookieService;

    /**
     * See {@link Config#ACCESS_COOKIE_DOMAIN}.
     */
    private final Optional<String> myCookieDomain;

    /**
     * See {@link Config#ACCESS_COOKIE_WINDOW_CLOSE_DELAY}.
     */
    private final Optional<Integer> myWindowCloseDelay;

    /**
     * Creates a handler that retrieves an access cookie for the client.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessCookieHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myHtmlTemplateEngine = HandlebarsTemplateEngine.create(aVertx);
        myCampusNetworkSubnets = new Cidr4Trie<>();
        myAccessCookieService = AccessCookieService.createProxy(aVertx);
        myWindowCloseDelay = Optional.ofNullable(aConfig.getInteger(Config.ACCESS_COOKIE_WINDOW_CLOSE_DELAY));
        myCookieDomain = Optional.ofNullable(aConfig.getString(Config.ACCESS_COOKIE_DOMAIN));

        // Register the neq helper
        ((Handlebars) myHtmlTemplateEngine.unwrap()).registerHelpers(ConditionalHelpers.class);

        for (final String subnet : aConfig.getString(Config.CAMPUS_NETWORK_SUBNETS).split(COMMA)) {
            final Cidr4 cidr = new Cidr4(subnet);

            // The value associated with each key doesn't particularly matter, since the methods we call on the Trie
            // don't use it
            myCampusNetworkSubnets.put(cidr, cidr.getAddressRange());
        }
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final Ip4 clientIpAddress;
        final URI origin;
        final boolean isOnCampusNetwork;
        final HttpServerResponse response =
                aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString());

        LOGGER.debug(MessageCodes.AUTH_021, request.headers().entries());

        try {
            clientIpAddress = new Ip4(request.remoteAddress().hostAddress());
            origin = URI.create(request.getParam(Param.ORIGIN));
        } catch (final IllegalArgumentException details) {
            response.setStatusCode(HTTP.BAD_REQUEST).end(details.getMessage());

            LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), details.getMessage());
            return;
        }

        isOnCampusNetwork = isOnNetwork(clientIpAddress, myCampusNetworkSubnets);

        myAccessCookieService.generateCookie(clientIpAddress.getAddress(), isOnCampusNetwork).compose(cookieValue -> {
            final Cookie cookie =
                    Cookie.cookie(CookieNames.HAUTH, cookieValue).setSameSite(CookieSameSite.NONE).setSecure(true);

            // Along with the origin, pass all the cookie data to the HTML template
            final JsonObject templateData = new JsonObject().put(TemplateKeys.ORIGIN, origin)
                    .put(TemplateKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                    .put(TemplateKeys.CLIENT_IP_ADDRESS, clientIpAddress)
                    .put(TemplateKeys.CAMPUS_NETWORK, isOnCampusNetwork);

            myWindowCloseDelay.ifPresent(delay -> {
                if (delay >= 0) {
                    templateData.put(TemplateKeys.WINDOW_CLOSE_DELAY, delay);
                }
            });
            myCookieDomain.ifPresent(cookie::setDomain);

            response.addCookie(cookie);

            return myHtmlTemplateEngine.render(templateData, "templates/cookie.hbs");
        }).onSuccess(renderedHtmlTemplate -> {
            response.setStatusCode(HTTP.OK).end(renderedHtmlTemplate);
        }).onFailure(error -> {
            if (error instanceof ServiceException) {
                final ServiceException details = (ServiceException) error;
                final int statusCode;
                final String errorMessage;

                if (details.failureCode() == Error.NOT_FOUND.ordinal()) {
                    statusCode = HTTP.BAD_REQUEST;
                    errorMessage = details.getMessage();
                } else {
                    statusCode = HTTP.INTERNAL_SERVER_ERROR;
                    errorMessage = LOGGER.getMessage(MessageCodes.AUTH_018);
                }
                response.setStatusCode(statusCode).end(errorMessage);

                LOGGER.error(MessageCodes.AUTH_006, request.method(), request.absoluteURI(), details.getMessage());
            } else {
                aContext.fail(error);
            }
        });
    }

    /**
     * Checks if an IP address belongs to a network.
     *
     * @param aIpAddress The IP address
     * @param aNetworkSubnets The collection of subnets that defines a network
     * @return Whether the IP address belongs to any subnet in the collection
     */
    public static boolean isOnNetwork(final Ip4 aIpAddress, final Cidr4Trie<String> aNetworkSubnets) {
        return aNetworkSubnets.shortestPrefixOfValue(new Cidr4(aIpAddress), true) != null;
    }
}
