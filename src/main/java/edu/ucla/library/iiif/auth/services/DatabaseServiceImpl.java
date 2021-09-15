package edu.ucla.library.iiif.auth.services;

import static info.freelibrary.util.Constants.SPACE;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

/**
 * The implementation of DatabaseService.
 */
public class DatabaseServiceImpl implements DatabaseService {

    /**
     * The database service's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class, MessageCodes.BUNDLE);

    /**
     * The postgres database (and default user) name.
     */
    private static final String POSTGRES = "postgres";

    /**
     * The PreparedQuery template for selecting an item's "access level".
     */
    private static final String SELECT_ACCESS_LEVEL = "SELECT access_level FROM items WHERE uid = $1";

    /**
     * The PreparedQuery template for upserting an item's "access level".
     */
    private static final String UPSERT_ACCESS_LEVEL = String.join(SPACE, "INSERT INTO items VALUES ($1, $2)",
            "ON CONFLICT (uid) DO", "UPDATE SET access_level = EXCLUDED.access_level");

    /**
     * The PreparedQuery template for selecting an origin's "degraded allowed".
     */
    private static final String SELECT_DEGRADED_ALLOWED = "SELECT degraded_allowed FROM origins WHERE url = $1";

    /**
     * The PreparedQuery template for upserting an origin's "degraded allowed".
     */
    private static final String UPSERT_DEGRADED_ALLOWED = String.join(SPACE, "INSERT INTO origins VALUES ($1, $2)",
            "ON CONFLICT (url) DO", "UPDATE SET degraded_allowed = EXCLUDED.degraded_allowed");

    /**
     * The database's default hostname.
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The underlying SQL client.
     */
    private final PgPool myDbConnectionPool;

    /**
     * The underlying Redis client.
     */
    private final Redis myDbCacheClient;

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     */
    public DatabaseServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myDbConnectionPool = PgPool.pool(aVertx, getConnectionOpts(aConfig), getPoolOpts(aConfig));
        myDbCacheClient = Redis.createClient(aVertx, getDbCacheClientOpts(aConfig));
    }

    @Override
    public Future<Void> close() {
        myDbCacheClient.close();
        return myDbConnectionPool.close();
    }

    @Override
    public Future<Integer> getAccessLevel(final String aID) {
        return myDbConnectionPool.withConnection(connection -> {
            return connection.preparedQuery(SELECT_ACCESS_LEVEL).execute(Tuple.of(aID));
        }).recover(error -> {
            return Future.failedFuture(new ServiceException(INTERNAL_ERROR, error.getMessage()));
        }).compose(select -> {
            if (hasSingleRow(select)) {
                return Future.succeededFuture(select.iterator().next().getInteger("access_level"));
            } else {
                return Future.failedFuture(new ServiceException(NOT_FOUND_ERROR, aID));
            }
        });
    }

    @Override
    public Future<Void> setAccessLevel(final String aID, final int aAccessLevel) {
        return myDbConnectionPool.withConnection(connection -> {
            return connection.preparedQuery(UPSERT_ACCESS_LEVEL).execute(Tuple.of(aID, aAccessLevel));
        }).recover(error -> {
            return Future.failedFuture(new ServiceException(INTERNAL_ERROR, error.getMessage()));
        }).compose(upsert -> {
            return Future.succeededFuture();
        });
    }

    @Override
    public Future<Boolean> getDegradedAllowed(final String aOrigin) {
        return myDbConnectionPool.withConnection(connection -> {
            return connection.preparedQuery(SELECT_DEGRADED_ALLOWED).execute(Tuple.of(aOrigin));
        }).recover(error -> {
            return Future.failedFuture(new ServiceException(INTERNAL_ERROR, error.getMessage()));
        }).compose(select -> {
            if (hasSingleRow(select)) {
                return Future.succeededFuture(select.iterator().next().getBoolean("degraded_allowed"));
            } else {
                return Future.failedFuture(new ServiceException(NOT_FOUND_ERROR, aOrigin));
            }
        });
    }

    @Override
    public Future<Void> setDegradedAllowed(final String aOrigin, final boolean aDegradedAllowed) {
        return myDbConnectionPool.withConnection(connection -> {
            return connection.preparedQuery(UPSERT_DEGRADED_ALLOWED).execute(Tuple.of(aOrigin, aDegradedAllowed));
        }).recover(error -> {
            return Future.failedFuture(new ServiceException(INTERNAL_ERROR, error.getMessage()));
        }).compose(upsert -> {
            return Future.succeededFuture();
        });
    }

    /**
     * Gets the options for the database connection pool.
     *
     * @param aConfig A configuration
     * @return The options for the database connection pool
     */
    private PoolOptions getPoolOpts(final JsonObject aConfig) {
        final int maxSize = aConfig.getInteger(Config.DB_CONNECTION_POOL_MAX_SIZE, 5);

        return new PoolOptions().setMaxSize(maxSize);
    }

    /**
     * Gets the database's connection options.
     *
     * @param aConfig A configuration
     * @return The database's connection options
     */
    private PgConnectOptions getConnectionOpts(final JsonObject aConfig) {
        final String dbHost = aConfig.getString(Config.DB_HOST, DEFAULT_HOSTNAME);
        final int dbPort = aConfig.getInteger(Config.DB_PORT, 5432);
        final String dbName = aConfig.getString(Config.DB_NAME, POSTGRES);
        final String dbUser = aConfig.getString(Config.DB_USER, POSTGRES);
        final String dbPassword = aConfig.getString(Config.DB_PASSWORD);
        final int dbReconnectAttempts = aConfig.getInteger(Config.DB_RECONNECT_ATTEMPTS, 2);
        final long dbReconnectInterval = aConfig.getInteger(Config.DB_RECONNECT_INTERVAL, 1000);

        // It's okay to show this automatically-generated password
        LOGGER.debug(MessageCodes.AUTH_003, dbPort, dbPassword);

        return new PgConnectOptions().setPort(dbPort).setHost(dbHost).setDatabase(dbName).setUser(dbUser)
                .setPassword(dbPassword).setReconnectAttempts(dbReconnectAttempts)
                .setReconnectInterval(dbReconnectInterval);
    }

    /**
     * Gets the test database cache's client options.
     *
     * @param aConfig A configuration
     * @return The test database cache's client options
     */
    private RedisOptions getDbCacheClientOpts(final JsonObject aConfig) {
        final String dbCacheHost = aConfig.getString(Config.DB_CACHE_HOST, DEFAULT_HOSTNAME);
        final int dbCachePort = aConfig.getInteger(Config.DB_CACHE_PORT, 6379);
        final String connectionString = StringUtils.format("redis://{}:{}", dbCacheHost, dbCachePort);

        LOGGER.debug(MessageCodes.AUTH_008, dbCachePort);

        return new RedisOptions().setConnectionString(connectionString);
    }

    /**
     * Checks if the given RowSet consists of a single row or not.
     *
     * @param aRowSet A RowSet representing the response to a database query
     * @return true if it has a single row, false otherwise
     */
    private static boolean hasSingleRow(final RowSet<Row> aRowSet) {
        return aRowSet.rowCount() == 1;
    }
}
