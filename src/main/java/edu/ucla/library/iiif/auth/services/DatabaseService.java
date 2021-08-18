package edu.ucla.library.iiif.auth.services;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgPool;
import io.vertx.redis.client.Redis;
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
     * @return A Future that resolves to the service instance
     */
    static Future<DatabaseService> create(final Vertx aVertx) {
        return ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            return Future.succeededFuture(new DatabaseServiceImpl(aVertx, config));
        });
    }

    /**
     * Creates an instance of the service proxy. Note that the service itself must have already been instantiated with
     * {@link #create} in order for this method to succeed.
     *
     * @param aVertx A Vert.x instance
     * @return The service proxy instance
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
     * Gets the "access level" of the item with the given identifier.
     *
     * @param aId The item identifier
     * @return A Future that resolves to the access level once it's been fetched
     */
    Future<Integer> getAccessLevel(String aId);

    /**
     * Sets the given "access level" of the item with the given identifier.
     *
     * @param aId The item identifier
     * @param aAccessLevel The access level to set for the item
     * @return A Future that resolves once the access level has been set
     */
    Future<Void> setAccessLevel(String aId, int aAccessLevel);

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

    /**
     * Gets the underlying database connection pool.
     *
     * @return A Future that resolves to the underlying database connection pool
     */
    Future<PgPool> getConnectionPool();

    /**
     * Gets the underlying Redis client.
     *
     * @return A Future that resolves to the underlying Redis client
     */
    Future<Redis> getRedisClient();
}
