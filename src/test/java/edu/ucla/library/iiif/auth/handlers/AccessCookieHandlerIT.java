
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessCookieHandler#handle}.
 */
public final class AccessCookieHandlerIT extends AbstractHandlerIT {

    /**
     * Tests that a client can obtain an access cookie.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestUri =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));

        myWebClient.get(myPort, TestConstants.INADDR_ANY, requestUri).send(get -> {
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
        final String requestUri = StringUtils.format(GET_COOKIE_PATH,
                URLEncoder.encode("https://iiif.unknown.library.ucla.edu", StandardCharsets.UTF_8));

        myWebClient.get(myPort, TestConstants.INADDR_ANY, requestUri).send(get -> {
            if (get.succeeded()) {
                assertEquals(HTTP.INTERNAL_SERVER_ERROR, get.result().statusCode());
                aContext.completeNow();
            } else {
                aContext.failNow(get.cause());
            }
        });
    }
}
