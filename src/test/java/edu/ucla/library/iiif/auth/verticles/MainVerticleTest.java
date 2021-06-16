
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
 * Tests the main verticle of the Vert.x application.
 */
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest extends AbstractBouncerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleTest.class, MessageCodes.BUNDLE);

    /**
     * Tests the server can start successfully.
     *
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(final TestContext aContext) {
        final WebClient client = WebClient.create(myContext.vertx());
        final Async asyncTask = aContext.async();

        client.get(myPort, TestConstants.INADDR_ANY, "/status").send(get -> {
            if (get.succeeded()) {
                aContext.assertEquals(HTTP.OK, get.result().statusCode());
                complete(asyncTask);
            } else {
                aContext.fail(get.cause());
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
