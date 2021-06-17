
package edu.ucla.library.iiif.auth.verticles;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

/**
 * An integration test that runs against a containerized version of the application.
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleIT extends AbstractBouncerIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleIT.class, MessageCodes.BUNDLE);

    /**
     * Tests the server was started successfully.
     *
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(final TestContext aContext) {
        final WebClient client = WebClient.create(myContext.vertx());
        final Async asyncTask = aContext.async();

        client.get(getPort(), TestConstants.INADDR_ANY, "/status").send(get -> {
            if (get.succeeded()) {
                aContext.assertEquals(HTTP.OK, get.result().statusCode());
                complete(asyncTask);
            } else {
                aContext.fail(get.cause());
            }
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
