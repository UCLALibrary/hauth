
package edu.ucla.library.iiif.auth.verticles;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import info.freelibrary.util.Logger;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

/**
 * An abstract test that other tests can extend.
 */
public abstract class AbstractBouncerIT {

    /**
     * The postgres database (and default user) name.
     */
    private static final String POSTGRES = "postgres";

    /**
     * Rule that creates the test context.
     */
    @Rule
    public RunTestOnContext myContext = new RunTestOnContext();

    /**
     * Rule that provides access to the test method name.
     */
    @Rule
    public TestName myNames = new TestName();

    /**
     * The configuration used to start the integration server.
     */
    private JsonObject myConfig;

    /**
     * Sets up the test.
     *
     * @param aContext A test context
     */
    @Before
    public void setUp(final TestContext aContext) {
        final Async asyncTask = aContext.async();

        ConfigRetriever.create(myContext.vertx()).getConfig().onSuccess(config -> {
            myConfig = config;
            complete(asyncTask);
        }).onFailure(error -> aContext.fail(error));
    }

    /**
     * Returns the logger used by an extending test.
     *
     * @return The test's logger
     */
    protected abstract Logger getLogger();

    /**
     * Gets the port number for the test instance of bouncer application.
     *
     * @return The application's port number
     */
    protected int getPort() {
        return myConfig.getInteger(Config.HTTP_PORT, 8888);
    }

    /**
     * Gets the pooling options for Vert.x's Postgres client.
     *
     * @return The pooling options for Vert.x's Postgres client
     */
    protected PoolOptions getPoolOpts() {
        return new PoolOptions().setMaxSize(5);
    }

    /**
     * Gets the test database's connection options.
     *
     * @return The test database's connection options
     */
    protected PgConnectOptions getConnectionOpts() {
        final String dbPassword = myConfig.getString(Config.DB_PASSWORD);

        // It's okay to show this automatically-generated password
        getLogger().trace(MessageCodes.BNCR_003, dbPassword);

        return new PgConnectOptions().setPort(5432).setHost(TestConstants.INADDR_ANY).setDatabase(POSTGRES)
                .setUser(POSTGRES).setPassword(dbPassword);
    }

    /**
     * A convenience method to end asynchronous tasks.
     *
     * @param aAsyncTask A task to complete
     */
    protected void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }

}
