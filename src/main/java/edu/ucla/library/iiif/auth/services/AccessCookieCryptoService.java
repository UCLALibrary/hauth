package edu.ucla.library.iiif.auth.services;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.NoSuchPaddingException;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for generating and decrypting encrypted access cookies.
 */
@ProxyGen
@VertxGen
public interface AccessCookieCryptoService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = AccessCookieCryptoService.class.getName();

    /**
     * The failure code if the service is configured improperly.
     */
    int CONFIGURATION_ERROR = 1;

    /**
     * The failure code if the service receives a decryption request for a cookie that has been tampered with.
     */
    int TAMPERED_COOKIE_ERROR = 2;

    /**
     * Creates an instance of the service.
     *
     * @param aConfig A configuration
     * @return The service instance
     * @throws ServiceException if the service implementation isn't configured properly
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    static AccessCookieCryptoService create(final JsonObject aConfig) {
        try {
            return new AccessCookieCryptoServiceImpl(aConfig);
        } catch (final InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException details) {
            throw new ServiceException(CONFIGURATION_ERROR, details.getMessage());
        }
    }

    /**
     * Creates an instance of the service proxy. Note that the service itself must have already been instantiated with
     * {@link #create} in order for this method to succeed.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static AccessCookieCryptoService createProxy(final Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(AccessCookieCryptoService.class);
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
     * {@link Cookie#cookie(String, String)}
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
