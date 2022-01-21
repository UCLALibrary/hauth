
package edu.ucla.library.iiif.auth.services;

import java.security.GeneralSecurityException;

import edu.ucla.library.iiif.auth.Config;

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
     * Creates an encrypted UCLA access cookie value.
     *
     * @param aClientIpAddress The IP address of the client
     * @param aIsOnCampusNetwork If the client is on a campus network subnet
     * @param aIsDegradedAllowed If the origin allows degraded access to content
     * @return A Future that resolves to a value that can be used to create a cookie with
     *         {@link Cookie#cookie(String, String)}
     */
    Future<String> generateUclaAccessCookie(String aClientIpAddress, boolean aIsOnCampusNetwork,
            boolean aIsDegradedAllowed);

    /**
     * Decrypts a UCLA access cookie value.
     *
     * @param aCookieValue An encrypted cookie value returned from {@link Cookie#getValue()}
     * @param aClientIpAddress The IP address of the client, which a vaild (encrypted) cookie value will contain
     * @return A Future that resolves to the decrypted cookie data unless the cookie has been tampered with (invalid
     *         JSON), stolen (IP address mismatch), or otherwise invalidated
     */
    Future<JsonObject> decryptUclaAccessCookie(String aCookieValue, String aClientIpAddress);

    /**
     * Validates a Sinai access cookie pair.
     *
     * @param aCipherText An encrypted cookie value returned from {@link Cookie#getValue()}
     * @param anInitializationVector The initialization vector used to encrypt {@code aCipherText}
     * @return A Future that resolves to whether the decrypted cookie value starts with
     *         {@link Config#SINAI_COOKIE_VALID_PREFIX}, or fails if the cookie has been tampered with, cannot be
     *         decrypted, or is otherwise invalid
     */
    Future<Boolean> validateSinaiAccessCookie(String aCipherText, String anInitializationVector);
}
