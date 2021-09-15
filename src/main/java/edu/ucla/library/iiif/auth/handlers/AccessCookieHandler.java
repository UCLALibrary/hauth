package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.COMMA;

import java.net.URI;

import com.github.veqryn.collect.Cidr4Trie;
import com.github.veqryn.collect.Trie;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ip4;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.services.AccessCookieCryptoService;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import info.freelibrary.util.HTTP;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

/**
 * Handler that handles access cookie requests.
 */
public class AccessCookieHandler implements Handler<RoutingContext> {

    /**
     * The application configuration.
     */
    private final JsonObject myConfig;

    /**
     * The service proxy for accessing the database.
     */
    private final DatabaseService myDatabaseServiceProxy;

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
    private final AccessCookieCryptoService myAccessCookieCryptoService;

    /**
     * Creates a handler that retrieves an access cookie for the client.
     *
     * @param aVertx The Vert.x instance
     * @param aConfig A configuration
     */
    public AccessCookieHandler(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myDatabaseServiceProxy = DatabaseService.createProxy(aVertx);
        myHtmlTemplateEngine = HandlebarsTemplateEngine.create(aVertx);
        myCampusNetworkSubnets = new Cidr4Trie<>();
        myAccessCookieCryptoService = AccessCookieCryptoService.createProxy(aVertx);

        for (final String subnet : aConfig.getString(Config.CAMPUS_NETWORK_SUBNETS).split(COMMA)) {
            final Cidr4 cidr = new Cidr4(subnet);

            // The value of the key doesn't particularly matter, since the methods we call on the Trie don't use it
            myCampusNetworkSubnets.put(cidr, cidr.getAddressRange());
        }
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();

        try {
            final Ip4 clientIpAddress = new Ip4(request.remoteAddress().hostAddress());
            final URI origin = URI.create(request.getParam(Param.ORIGIN));
            final boolean isOnCampusNetwork = isOnNetwork(clientIpAddress, myCampusNetworkSubnets);

            myDatabaseServiceProxy.getDegradedAllowed(origin.toString()).compose(isDegradedAllowed -> {
                final Future<String> cookieGeneration = myAccessCookieCryptoService
                        .generateCookie(clientIpAddress.getAddress(), isOnCampusNetwork, isDegradedAllowed);

                return cookieGeneration.compose(cookieValue -> {
                    final Cookie cookie = Cookie.cookie("iiif-access", cookieValue);

                    // Along with the origin, pass all the cookie data to the HTML template
                    final JsonObject htmlTemplateData = new JsonObject().put(Param.ORIGIN, origin)
                            .put(CookieJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                            .put(CookieJsonKeys.CLIENT_IP_ADDRESS, clientIpAddress)
                            .put(CookieJsonKeys.CAMPUS_NETWORK, isOnCampusNetwork)
                            .put(CookieJsonKeys.DEGRADED_ALLOWED, isDegradedAllowed);

                    aContext.addCookie(cookie);

                    return myHtmlTemplateEngine.render(htmlTemplateData.getMap(), "templates/cookie.hbs");
                });
            }).onSuccess(renderedHtmlTeplate -> {
                aContext.response().setStatusCode(HTTP.OK)
                        .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString()).end(renderedHtmlTeplate);
            }).onFailure(error -> {
                aContext.fail(HTTP.INTERNAL_SERVER_ERROR, error);
            });
        } catch (final IllegalArgumentException details) {
            aContext.fail(HTTP.BAD_REQUEST, details);
        }
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
