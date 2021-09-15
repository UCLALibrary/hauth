
package edu.ucla.library.iiif.auth.verticles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * An integration test that runs against a containerized version of the application.
 */
@ExtendWith(VertxExtension.class)
public class MainVerticleIT extends AbstractHauthIT {

    /**
     * A URI template for access cookie requests.
     */
    private static final String GET_COOKIE_URI_TEMPLATE = "/cookie?origin={}";

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleIT.class, MessageCodes.BUNDLE);

    /**
     * Tests the server was started successfully.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(final Vertx aVertx, final VertxTestContext aContext) {
        final WebClient client = WebClient.create(aVertx);

        client.get(getPort(), TestConstants.INADDR_ANY, "/status").send(get -> {
            if (get.succeeded()) {
                assertEquals(HTTP.OK, get.result().statusCode());
                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }

    /**
     * Tests that a client can obtain an access cookie.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testObtainAccessCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final WebClient client = WebClient.create(aVertx);
        final String requestUri = StringUtils.format(GET_COOKIE_URI_TEMPLATE, IIIF_TEST_ORIGIN);

        client.get(getPort(), TestConstants.INADDR_ANY, requestUri).send(get -> {
            if (get.succeeded()) {
                final HttpResponse<?> response = get.result();

                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(1, response.cookies().size());

                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }

    /**
     * Tests that a client can't obtain an access cookie for an unknown origin.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetCookieUnknownOrigin(final Vertx aVertx, final VertxTestContext aContext) {
        final WebClient client = WebClient.create(aVertx);
        final String requestUri = StringUtils.format(GET_COOKIE_URI_TEMPLATE, "https://iiif.unknown.library.ucla.edu");

        client.get(getPort(), TestConstants.INADDR_ANY, requestUri).send(get -> {
            if (get.succeeded()) {
                assertEquals(HTTP.INTERNAL_SERVER_ERROR, get.result().statusCode());
                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
