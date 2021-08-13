package edu.ucla.library.iiif.auth.services;

import java.net.URI;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;

/**
 * A service for accessing the Hauth SQL database.
 */
public interface DatabaseService {

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @return The service instance
     */
    static Future<DatabaseService> create(final Vertx aVertx) {
        return ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            return Future.succeededFuture(new DatabaseServiceImpl(aVertx, config));
        });
    }

    /**
     * Closes the underlying connections required by this service.
     *
     * @return A Future that resolves once the connections have been closed
     */
    Future<Void> close();

    /**
     * Gets the "access level" of the item with the given identifier.
     *
     * @param aId The item identifier
     * @return A Future that resolves to the access level once it's been fetched
     */
    Future<Integer> getAccessLevel(final String aId);

    /**
     * Sets the given "access level" of the item with the given identifier.
     *
     * @param aId The item identifier
     * @param aAccessLevel The access level to set for the item
     * @return A Future that resolves once the access level has been set
     */
    Future<Void> setAccessLevel(final String aId, final int aAccessLevel);

    /**
     * Gets the "degraded allowed" for content hosted at the given origin.
     *
     * @param aOrigin The origin
     * @return A Future that resolves to the degraded allowed once it's been fetched
     */
    Future<Boolean> getDegradedAllowed(final URI aOrigin);

    /**
     * Sets the given "degraded allowed" for content hosted at the given origin.
     *
     * @param aOrigin The origin
     * @param aDegradedAllowed The degraded allowed to set for the origin
     * @return A Future that resolves once the degraded allowed has been set
     */
    Future<Void> setDegradedAllowed(final URI aOrigin, final boolean aDegradedAllowed);

    /**
     * Gets the underlying SQL client.
     *
     * @return The underlying SQL client
     */
    SqlClient getSqlClient();
}
