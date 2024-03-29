
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;

import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import info.freelibrary.util.Constants;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessCookieHandler#handle}.
 */
public final class AccessCookieHandlerIT extends AbstractHandlerIT {

    /**
     * Tests that a client can obtain an access cookie.
     *
     * @param aReverseProxyDeployment Whether or not to simulate app deployment behind a reverse proxy
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testGetCookie(final boolean aReverseProxyDeployment, final Vertx aVertx,
            final VertxTestContext aContext) {
        final String requestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI);
        final String explicitCookieDomain = myConfig.getString(Config.ACCESS_COOKIE_DOMAIN);

        if (aReverseProxyDeployment) {
            getCookie.putHeader(X_FORWARDED_FOR, FORWARDED_IP_ADDRESSES);
        }

        getCookie.send().onSuccess(response -> {
            aContext.verify(() -> {
                final Cookie cookie;

                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(1, response.cookies().size());

                if (aReverseProxyDeployment) {
                    assertEquals(FORWARDED_CLIENT_IP,
                            Jsoup.parse(response.bodyAsString()).getElementById("client-ip-address").text());
                }

                cookie = ClientCookieDecoder.STRICT.decode(response.cookies().get(0));

                if (explicitCookieDomain != null) {
                    assertEquals(explicitCookieDomain, cookie.domain());
                } else {
                    assertNull(cookie.domain());
                }

                assertEquals(SameSite.None, ((DefaultCookie) cookie).sameSite());
                assertTrue(cookie.isSecure());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }
}
