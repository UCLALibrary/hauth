
package edu.ucla.library.iiif.auth.verticles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.iiif.auth.Config;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * An abstract test that other tests can extend.
 */
@ExtendWith(VertxExtension.class)
public abstract class AbstractBouncerIT {

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
        ConfigRetriever.create(aVertx).getConfig().onSuccess(config -> {
            myConfig = config;
            aContext.completeNow();
        }).onFailure(error -> aContext.failNow(error));
    }

    /**
     * Gets the port number for the test instance of bouncer application.
     *
     * @return The application's port number
     */
    protected int getPort() {
        return myConfig.getInteger(Config.HTTP_PORT, 8888);
    }

}
