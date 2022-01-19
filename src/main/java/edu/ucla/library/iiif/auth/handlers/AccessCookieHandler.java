
package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.COMMA;

import java.net.URI;

import com.github.veqryn.collect.Cidr4Trie;
import com.github.veqryn.collect.Trie;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ip4;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
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
    private final AccessCookieService myAccessCookieService;

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
        myAccessCookieService = AccessCookieService.createProxy(aVertx);

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

        try {
            clientIpAddress = new Ip4(request.remoteAddress().hostAddress());
            origin = URI.create(request.getParam(Param.ORIGIN));
        } catch (final IllegalArgumentException details) {
            aContext.fail(HTTP.BAD_REQUEST, details);
            return;
        }

        isOnCampusNetwork = isOnNetwork(clientIpAddress, myCampusNetworkSubnets);

        myDatabaseServiceProxy.getDegradedAllowed(origin.toString()).compose(isDegradedAllowed -> {
            final Future<String> cookieGeneration = myAccessCookieService.generateCookie(clientIpAddress.getAddress(),
                    isOnCampusNetwork, isDegradedAllowed);

            return cookieGeneration.compose(cookieValue -> {
                final Cookie cookie = Cookie.cookie("iiif-access", cookieValue);

                // Along with the origin, pass all the cookie data to the HTML template
                final JsonObject htmlTemplateData = new JsonObject().put(TemplateKeys.ORIGIN, origin)
                        .put(TemplateKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                        .put(TemplateKeys.CLIENT_IP_ADDRESS, clientIpAddress)
                        .put(TemplateKeys.CAMPUS_NETWORK, isOnCampusNetwork)
                        .put(TemplateKeys.DEGRADED_ALLOWED, isDegradedAllowed);

                aContext.response().addCookie(cookie);

                return myHtmlTemplateEngine.render(htmlTemplateData.getMap(), "templates/cookie.hbs");
            });
        }).onSuccess(renderedHtmlTemplate -> {
            aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString())
                    .setStatusCode(HTTP.OK).end(renderedHtmlTemplate);
        }).onFailure(details -> {
            aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString())
                    .setStatusCode(HTTP.INTERNAL_SERVER_ERROR).end(details.getMessage());
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
