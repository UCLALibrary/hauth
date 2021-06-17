
package edu.ucla.library.iiif.auth.verticles;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlClient;

/**
 * A test of the database connection.
 */
@RunWith(VertxUnitRunner.class)
public class AuthDatabaseIT extends AbstractBouncerIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDatabaseIT.class, MessageCodes.BUNDLE);

    /**
     * Tests the number of users in the database.
     *
     * @param aContext A test context
     */
    @Test
    public final void testDbUserCount(final TestContext aContext) {
        final SqlClient client = PgPool.client(myContext.vertx(), getConnectionOpts(), getPoolOpts());
        final Async asyncTask = aContext.async();

        // Check that the number of users is what we expect it to be
        client.query("select * from pg_catalog.pg_user;").execute(query -> {
            if (query.succeeded()) {
                // Three users tells us our SQL load successfully completed
                aContext.assertEquals(3, query.result().size());
                complete(asyncTask);
            } else {
                aContext.fail(query.cause());
            }

            client.close();
        });
    }

    /**
     * Returns the logger used by these tests.
     *
     * @return The logger used by these tests
     */
    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
