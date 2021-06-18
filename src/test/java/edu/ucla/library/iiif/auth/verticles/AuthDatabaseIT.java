
package edu.ucla.library.iiif.auth.verticles;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

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
        runQuery(aContext, "select * from pg_catalog.pg_user;", 3);
    }

    /**
     * Tests the number of cookies set up in the testing environment.
     *
     * @param aContext A test context
     */
    @Test
    public final void testDbCookies(final TestContext aContext) {
        runQuery(aContext, "select * from public.cookies;", 2);
    }

    /**
     * Tests the number of items set up in the testing environment.
     *
     * @param aContext A test context
     */
    @Test
    public final void testDbItems(final TestContext aContext) {
        runQuery(aContext, "select * from public.items;", 3);
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
