package edu.ucla.library.iiif.auth.services;

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
import io.vertx.sqlclient.PoolOptions;
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
     * The underlying SQL client.
     */
    private final SqlClient myDbClient;

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     * @return The service instance
     */
    public DatabaseServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myDbClient = PgPool.client(aVertx, getConnectionOpts(aConfig), getPoolOpts());
    }

    @Override
    public Future<Void> close() {
        return getSqlClient().close();
    }

    @Override
    public Future<Integer> getAccessLevel(final String aId) {
        final String selectSql = "SELECT access_level FROM items WHERE uid = $1";

        return getSqlClient().preparedQuery(selectSql).execute(Tuple.of(aId)).recover(error -> {
            return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_006, selectSql, error));
        }).compose(select -> {
            if (select.rowCount() == 1) {
                return Future.succeededFuture(select.iterator().next().getInteger("access_level"));
            } else {
                return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_004, aId, "items"));
            }
        });
    }

    @Override
    public Future<Void> setAccessLevel(final String aId, final int aAccessLevel) {
        final String upsertSql = "INSERT INTO items VALUES ($1, $2) ON CONFLICT (uid) DO UPDATE SET access_level = EXCLUDED.access_level";

        return getSqlClient().preparedQuery(upsertSql).execute(Tuple.of(aId, aAccessLevel)).recover(error -> {
            return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_006, upsertSql, error));
        }).compose(upsert -> {
            if (upsert.rowCount() == 1) {
                if (upsert.iterator().hasNext()) {
                    LOGGER.debug(upsert.iterator().next().deepToString());
                }
                return Future.succeededFuture();
            } else {
                return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_005, aId, "items"));
            }
        });
    }

    @Override
    public Future<Boolean> getDegradedAllowed(final URI aOrigin) {
        final String selectSql = "SELECT degraded_allowed FROM origins WHERE url = $1";

        return getSqlClient().preparedQuery(selectSql).execute(Tuple.of(aOrigin.toString())).recover(error -> {
            return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_006, selectSql, error));
        }).compose(select -> {
            if (select.rowCount() == 1) {
                return Future.succeededFuture(select.iterator().next().getBoolean("degraded_allowed"));
            } else {
                return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_004, aOrigin, "origins"));
            }
        });
    }

    @Override
    public Future<Void> setDegradedAllowed(final URI aOrigin, final boolean aDegradedAllowed) {
        final String upsertSql = "INSERT INTO origins VALUES ($1, $2) ON CONFLICT (url) DO UPDATE SET degraded_allowed = EXCLUDED.degraded_allowed";
        final Tuple queryArgs = Tuple.of(aOrigin.toString(), aDegradedAllowed);

        return getSqlClient().preparedQuery(upsertSql).execute(queryArgs).recover(error -> {
            return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_006, upsertSql, error));
        }).compose(upsert -> {
            if (upsert.rowCount() == 1) {
                if (upsert.iterator().hasNext()) {
                    LOGGER.debug(upsert.iterator().next().deepToString());
                }
                return Future.succeededFuture();
            } else {
                return Future.failedFuture(StringUtils.format(MessageCodes.AUTH_005, aOrigin, "origins"));
            }
        });
    }

    @Override
    public SqlClient getSqlClient() {
        return myDbClient;
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
     * @return The database's connection options
     */
    private PgConnectOptions getConnectionOpts(final JsonObject myConfig) {
        final String dbHost = myConfig.getString(Config.DB_HOST, "localhost");
        final int dbPort = myConfig.getInteger(Config.DB_PORT, 5432);
        final String dbName = myConfig.getString(Config.DB_NAME, POSTGRES);
        final String dbUser = myConfig.getString(Config.DB_USER, POSTGRES);
        final String dbPassword = myConfig.getString(Config.DB_PASSWORD);

        // It's okay to show this automatically-generated password
        LOGGER.debug(MessageCodes.AUTH_003, dbPort, dbPassword);

        return new PgConnectOptions().setPort(dbPort).setHost(dbHost).setDatabase(dbName).setUser(dbUser)
                .setPassword(dbPassword);
    }
}
