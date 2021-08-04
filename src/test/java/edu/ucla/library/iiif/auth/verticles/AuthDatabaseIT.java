
package edu.ucla.library.iiif.auth.verticles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.SqlClient;

/**
 * A test of the database connection.
 */
@ExtendWith(VertxExtension.class)
public class AuthDatabaseIT extends AbstractHauthIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDatabaseIT.class, MessageCodes.BUNDLE);

    /**
     * Tests the number of users in the database.
     *
     * @param aVertx A Vert.x instance used to run the tests
     * @param aContext A test context
     */
    @Test
    public final void testDbUserCount(final Vertx aVertx, final VertxTestContext aContext) {
        final SqlClient client = PgPool.client(aVertx, getDbConnectionOpts(), getPoolOpts());

        // Check that the number of users is what we expect it to be
        client.query("select * from pg_catalog.pg_user;").execute(query -> {
            if (query.succeeded()) {
                // Three users tells us our SQL load successfully completed
                assertEquals(3, query.result().size());
                aContext.completeNow();
            } else {
                aContext.failNow(query.cause());
            }

            client.close();
        });
    }

    /**
     * Tests that the database cache is up and usable.
     *
     * @param aVertx A Vert.x instance used to run the tests
     * @param aContext A test context
     */
    @Test
    final void testDbCacheConnection(final Vertx aVertx, final VertxTestContext aContext) {
        final Redis client = Redis.createClient(aVertx, getDbCacheClientOpts());

        client.connect().compose(connection -> {
            return RedisAPI.api(client).lolwut(List.of());
        }).onSuccess(response -> {
            for (final String line : response.toString().split("\\r?\\n")) {
                LOGGER.debug(line);
            }
            aContext.completeNow();
        }).onFailure(aContext::failNow).onComplete(done -> client.close());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
