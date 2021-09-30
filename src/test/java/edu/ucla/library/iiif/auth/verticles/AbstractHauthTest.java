
package edu.ucla.library.iiif.auth.verticles;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Constants;
import info.freelibrary.util.FileUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.PortUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * An abstract test that other tests can extend.
 */
@ExtendWith(VertxExtension.class)
public abstract class AbstractHauthTest {

    /**
     * The port at which our test instances listen.
     */
    protected int myPort;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeEach
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        final DeploymentOptions options = new DeploymentOptions();

        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            myPort = PortUtils.getPort();
            options.setConfig(config.put(Config.HTTP_PORT, myPort));

            return aVertx.deployVerticle(MainVerticle.class.getName(), options);
        }).onSuccess(result -> aContext.completeNow()).onFailure(aContext::failNow);
    }

    /**
     * Returns the logger used by an extending test.
     *
     * @return The test's logger
     */
    protected abstract Logger getLogger();

    /**
     * Undeploy a verticle so that a test can swap in a mock.
     *
     * @param aVertx A Vert.x instance
     * @param aVerticleName The name of the verticle to undeploy
     * @return A future removal of the verticle
     */
    protected Future<Void> undeployVerticle(final Vertx aVertx, final String aVerticleName) {
        return aVertx.sharedData().getLocalAsyncMap(MainVerticle.VERTICLES_MAP).compose(map -> {
            return map.get(aVerticleName);
        }).compose(deploymentID -> {
            return aVertx.undeploy(deploymentID.toString()).onSuccess(result -> {
                getLogger().debug(MessageCodes.AUTH_002, aVerticleName, deploymentID);
            });
        });
    }

    /**
     * Takes a supplied file path and makes it unique so it can be used across different simultaneous tests.
     *
     * @param aFilePath A full file path
     * @return A unique version of the supplied file path
     */
    protected String getUniqueFileName(final String aFilePath) {
        final String filePath = FileUtils.stripExt(aFilePath);
        final String fileExt = FileUtils.getExt(aFilePath);

        return filePath + UUID.randomUUID().toString() + Constants.PERIOD + fileExt;
    }
}
