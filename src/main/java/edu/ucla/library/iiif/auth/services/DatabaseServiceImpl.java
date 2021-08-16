package edu.ucla.library.iiif.auth.services;

import static info.freelibrary.util.Constants.SPACE;

import java.net.URI;

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
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
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
     * The name of the database's "items" table.
     */
    private static final String ITEMS = "items";

    /**
     * The name of the database's "origins" table.
     */
    private static final String ORIGINS = "origins";

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
     * The underlying SQL client.
     */
    private final SqlClient myDbClient;

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
        myDbClient = PgPool.client(aVertx, getConnectionOpts(aConfig), getPoolOpts());
        myDbCacheClient = Redis.createClient(aVertx, getDbCacheClientOpts(aConfig));
    }

    @Override
    public Future<DatabaseService> open() {
        final DatabaseService self = this;

        return getRedisClient().connect().compose(connection -> Future.succeededFuture(self)).recover(error -> {
            return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_009, error.getMessage()));
        });
    }

    @Override
    public Future<Void> close() {
        getRedisClient().close();
        return getSqlClient().close();
    }

    @Override
    public Future<Integer> getAccessLevel(final String aId) {
        final Future<RowSet<Row>> queryResult = getSqlClient().preparedQuery(SELECT_ACCESS_LEVEL)
                .execute(Tuple.of(aId));

        return queryResult.recover(error -> {
            return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_006, SELECT_ACCESS_LEVEL, error));
        }).compose(select -> {
            if (hasSingleRow(select)) {
                return Future.succeededFuture(select.iterator().next().getInteger("access_level"));
            } else {
                return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_004, aId, ITEMS));
            }
        });
    }

    @Override
    public Future<Void> setAccessLevel(final String aId, final int aAccessLevel) {
        final Future<RowSet<Row>> queryResult = getSqlClient().preparedQuery(UPSERT_ACCESS_LEVEL)
                .execute(Tuple.of(aId, aAccessLevel));

        return queryResult.recover(error -> {
            return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_006, UPSERT_ACCESS_LEVEL, error));
        }).compose(upsert -> {
            if (hasSingleRow(upsert)) {
                if (upsert.iterator().hasNext()) {
                    LOGGER.debug(upsert.iterator().next().deepToString());
                }
                return Future.succeededFuture();
            } else {
                return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_005, aId, ITEMS));
            }
        });
    }

    @Override
    public Future<Boolean> getDegradedAllowed(final URI aOrigin) {
        final Future<RowSet<Row>> queryResult = getSqlClient().preparedQuery(SELECT_DEGRADED_ALLOWED)
                .execute(Tuple.of(aOrigin.toString()));

        return queryResult.recover(error -> {
            return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_006, SELECT_DEGRADED_ALLOWED, error));
        }).compose(select -> {
            if (hasSingleRow(select)) {
                return Future.succeededFuture(select.iterator().next().getBoolean("degraded_allowed"));
            } else {
                return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_004, aOrigin, ORIGINS));
            }
        });
    }

    @Override
    public Future<Void> setDegradedAllowed(final URI aOrigin, final boolean aDegradedAllowed) {
        final Future<RowSet<Row>> queryResult = getSqlClient().preparedQuery(UPSERT_DEGRADED_ALLOWED)
                .execute(Tuple.of(aOrigin.toString(), aDegradedAllowed));

        return queryResult.recover(error -> {
            return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_006, UPSERT_DEGRADED_ALLOWED, error));
        }).compose(upsert -> {
            if (hasSingleRow(upsert)) {
                if (upsert.iterator().hasNext()) {
                    LOGGER.debug(upsert.iterator().next().deepToString());
                }
                return Future.succeededFuture();
            } else {
                return Future.failedFuture(LOGGER.getMessage(MessageCodes.AUTH_005, aOrigin, ORIGINS));
            }
        });
    }

    @Override
    public SqlClient getSqlClient() {
        return myDbClient;
    }

    @Override
    public Redis getRedisClient() {
        return myDbCacheClient;
    }

    /**
     * Gets the pooling options for Vert.x's Postgres client.
     *
     * @return The pooling options for Vert.x's Postgres client
     */
    private PoolOptions getPoolOpts() {
        return new PoolOptions().setMaxSize(5);
    }

    /**
     * Gets the database's connection options.
     *
     * @param aConfig A configuration
     * @return The database's connection options
     */
    private PgConnectOptions getConnectionOpts(final JsonObject aConfig) {
        final String dbHost = aConfig.getString(Config.DB_HOST, "localhost");
        final int dbPort = aConfig.getInteger(Config.DB_PORT, 5432);
        final String dbName = aConfig.getString(Config.DB_NAME, POSTGRES);
        final String dbUser = aConfig.getString(Config.DB_USER, POSTGRES);
        final String dbPassword = aConfig.getString(Config.DB_PASSWORD);

        // It's okay to show this automatically-generated password
        LOGGER.debug(MessageCodes.AUTH_003, dbPort, dbPassword);

        return new PgConnectOptions().setPort(dbPort).setHost(dbHost).setDatabase(dbName).setUser(dbUser)
                .setPassword(dbPassword);
    }

    /**
     * Gets the test database cache's client options.
     *
     * @param aConfig A configuration
     * @return The test database cache's client options
     */
    private RedisOptions getDbCacheClientOpts(final JsonObject aConfig) {
        final int dbCachePort = aConfig.getInteger(Config.DB_CACHE_PORT, 6379);
        final String connectionString = StringUtils.format("redis://localhost:{}", dbCachePort);

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
