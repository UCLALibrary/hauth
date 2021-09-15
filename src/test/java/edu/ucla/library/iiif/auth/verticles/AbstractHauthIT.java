
package edu.ucla.library.iiif.auth.verticles;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.PoolOptions;

/**
 * An abstract test that other tests can extend.
 */
@ExtendWith(VertxExtension.class)
public abstract class AbstractHauthIT {

    /**
     * A test id.
     */
    protected static final String TEST_ID = "ark:/21198/00000000";

    /**
     * A test origin.
     */
    protected static final String TEST_ORIGIN = "https://iiif-test.library.ucla.edu";

    /**
     * The configuration used to start the integration server.
     */
    private JsonObject myConfig;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeEach
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            final DatabaseService db = DatabaseService.create(aVertx, config);

            myConfig = config;

            // Add some database entries
            return CompositeFuture.all(db.setAccessLevel(TEST_ID, 0), db.setDegradedAllowed(TEST_ORIGIN, false))
                    .compose(result -> db.close());
        }).onSuccess(result -> aContext.completeNow()).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterEach
    public void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        aContext.completeNow();
    }

    /**
     * Returns the logger used by an extending test.
     *
     * @return The test's logger
     */
    protected abstract Logger getLogger();

    /**
     * Gets the pooling options for Vert.x's Postgres client.
     *
     * @return The pooling options for Vert.x's Postgres client
     */
    protected PoolOptions getPoolOpts() {
        return new PoolOptions().setMaxSize(5);
    }

    /**
     * Gets the port number for the test instance of hauth application.
     *
     * @return The application's port number
     */
    protected int getPort() {
        return myConfig.getInteger(Config.HTTP_PORT, 8888);
    }

}
