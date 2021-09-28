
package edu.ucla.library.iiif.auth.services;

import java.security.GeneralSecurityException;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for generating and decrypting encrypted access cookies.
 */
@ProxyGen
@VertxGen
public interface AccessCookieService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = AccessCookieService.class.getName();

    /**
     * Creates an instance of the service.
     *
     * @param aConfig A configuration
     * @return The service instance
     * @throws GeneralSecurityException if the service implementation isn't configured properly
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    static AccessCookieService create(final JsonObject aConfig) throws GeneralSecurityException {
        return new AccessCookieServiceImpl(aConfig);
    }

    /**
     * Creates an instance of the service proxy. Note that the service itself must have already been instantiated with
     * {@link #create} in order for this method to succeed.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static AccessCookieService createProxy(final Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(AccessCookieService.class);
    }

    /**
     * Closes the service.
     *
     * @return A Future that resolves once the service has been closed
     */
    @ProxyClose
    Future<Void> close();

    /**
     * Creates a cookie value with an encrypted secret.
     *
     * @param aClientIpAddress The IP address of the client
     * @param aIsOnCampusNetwork If the client is on a campus network subnet
     * @param aIsDegradedAllowed If the origin allows degraded access to content
     * @return A Future that resolves to a value that can be used to create a cookie with
     *         {@link Cookie#cookie(String, String)}
     */
    Future<String> generateCookie(String aClientIpAddress, boolean aIsOnCampusNetwork, boolean aIsDegradedAllowed);

    /**
     * Decrypts a cookie value's encrypted secret.
     *
     * @param aCookieValue An encrypted cookie value returned from {@link Cookie#getValue()}
     * @return A Future that resolves to the decrypted cookie data unless the cookie has been tampered with
     */
    Future<JsonObject> decryptCookie(String aCookieValue);
}