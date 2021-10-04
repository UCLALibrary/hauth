
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TokenJsonKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessTokenHandler#handle}.
 */
public final class AccessTokenHandlerIT extends AbstractHandlerIT {

    /**
     * Tests that a client can use a valid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetToken(final Vertx aVertx, final VertxTestContext aContext) {
        final String getCookieRequestUri =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, getCookieRequestUri);

        getCookie.send().compose(result -> {
            final String cookieHeader = result.cookies().get(0);
            final String cookieValue = cookieHeader.split("=")[1];
            final String clientIpAddress =
                    Jsoup.parse(result.bodyAsString()).getElementById("client-ip-address").text();

            return myAccessCookieService.decryptCookie(cookieValue, clientIpAddress).compose(cookie -> {
                final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, GET_TOKEN_PATH)
                        .putHeader(HttpHeaders.COOKIE.toString(), cookieHeader);

                return getToken.send().onSuccess(response -> {
                    final JsonObject expectedAccessTokenDecoded =
                            new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION)).put(
                                    TokenJsonKeys.CAMPUS_NETWORK, cookie.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
                    final String expectedAccessToken =
                            Base64.getEncoder().encodeToString(expectedAccessTokenDecoded.encode().getBytes());
                    final Optional<Integer> expectedExpiresIn =
                            Optional.ofNullable(myConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
                    final JsonObject expected =
                            new JsonObject().put(ResponseJsonKeys.ACCESS_TOKEN, expectedAccessToken);

                    expectedExpiresIn.ifPresent(expiry -> expected.put(ResponseJsonKeys.EXPIRES_IN, expiry));

                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals(MediaType.APPLICATION_JSON.toString(),
                            response.headers().get(HttpHeaders.CONTENT_TYPE));
                    assertEquals(expected, response.bodyAsJsonObject());

                    aContext.completeNow();
                }).onFailure(aContext::failNow);
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can't use an invalid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenInvalidCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String cookieHeader = "iiif-access=invalid";
        final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, GET_TOKEN_PATH)
                .putHeader(HttpHeaders.COOKIE.toString(), cookieHeader);

        getToken.send().onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
