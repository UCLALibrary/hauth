
package edu.ucla.library.iiif.auth.services;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for accessing the Hauth database.
 */
@ProxyGen
@VertxGen
public interface DatabaseService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = DatabaseService.class.getName();

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     * @return The service instance
     */
    static DatabaseService create(final Vertx aVertx, final JsonObject aConfig) {
        return new DatabaseServiceImpl(aVertx, aConfig);
    }

    /**
     * Creates an instance of the service proxy. Note that the service itself must have already been instantiated with
     * {@link #create} in order for this method to succeed.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static DatabaseService createProxy(final Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(DatabaseService.class);
    }

    /**
     * Closes the underlying connections required by this service.
     *
     * @return A Future that resolves once the connections have been closed
     */
    @ProxyClose
    Future<Void> close();

    /**
     * Gets the "access mode" of the item with the given identifier.
     *
     * @param aID The item identifier
     * @return A Future that resolves to the access mode once it's been fetched
     */
    Future<Integer> getAccessMode(String aID);

    /**
     * Sets the given "access mode" of the item with the given identifier.
     *
     * @param aID The item identifier
     * @param aAccessMode The access mode to set for the item
     * @return A Future that resolves once the access mode has been set
     */
    Future<Void> setAccessMode(String aID, int aAccessMode);

    /**
     * Sets the given items.
     *
     * @param aItems An array of objects that conforms to the request body schema of the "postItems" OpenAPI operation
     * @return A Future that resolves once the items have been set
     */
    Future<Void> setItems(JsonArray aItems);

    /**
     * Gets the "degraded allowed" for content hosted at the given origin.
     *
     * @param aOrigin The origin
     * @return A Future that resolves to the degraded allowed once it's been fetched
     */
    Future<Boolean> getDegradedAllowed(String aOrigin);

    /**
     * Sets the given "degraded allowed" for content hosted at the given origin.
     *
     * @param aOrigin The origin
     * @param aDegradedAllowed The degraded allowed to set for the origin
     * @return A Future that resolves once the degraded allowed has been set
     */
    Future<Void> setDegradedAllowed(String aOrigin, boolean aDegradedAllowed);
}
