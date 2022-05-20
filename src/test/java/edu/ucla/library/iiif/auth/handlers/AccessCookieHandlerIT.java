
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessCookieHandler#handle}.
 */
public final class AccessCookieHandlerIT extends AbstractHandlerIT {

    /**
     * The id of the HTML element that contains the client IP address that was put in the cookie.
     */
    private final String myClientIpAddressID = "client-ip-address";

    /**
     * Tests that a client can obtain an access cookie.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI);

        getCookie.send().onSuccess(response -> {
            assertEquals(HTTP.OK, response.statusCode());
            assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(1, response.cookies().size());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can obtain an access cookie with the correct IP address when the app is deployed behind a
     * reverse proxy.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetCookieReverseProxyDeployment(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final String expectedClientIpAddress = "1.1.1.1";
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI)
                .putHeader(CLIENT_IP_HEADER_NAME, CLIENT_IP_HEADER_VALUE);

        getCookie.send().onSuccess(response -> {
            final String actualClientIpAddress;

            assertEquals(HTTP.OK, response.statusCode());
            assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(1, response.cookies().size());
            assertEquals(CLIENT_IP, Jsoup.parse(response.bodyAsString()).getElementById(myClientIpAddressID).text());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can't obtain an access cookie for an unknown origin.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetCookieUnknownOrigin(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_COOKIE_PATH,
                URLEncoder.encode("https://iiif.unknown.library.ucla.edu", StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI);

        getCookie.send().onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
