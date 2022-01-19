
package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.EMPTY;
import static info.freelibrary.util.Constants.EQUALS;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.TokenJsonKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;
import edu.ucla.library.iiif.auth.utils.TestConstants;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link AccessTokenHandler#handle}.
 */
public final class AccessTokenHandlerIT extends AbstractHandlerIT {

    /**
     * Obtains a random unsigned integer by zeroing the sign bit of a random signed integer.
     */
    private final String myMessageID = String.valueOf(new Random().nextInt() & 0x7FFFFFFF);

    /**
     * The query string to use for token requests by browser clients.
     */
    private final String myGetTokenRequestQuery =
            StringUtils.format("{}={}&{}={}", Param.MESSAGE_ID, myMessageID, Param.ORIGIN, TEST_ORIGIN);

    /**
     * The invalid cookie to test with.
     */
    private final String myInvalidCookieHeader = "iiif-access=invalid";

    /**
     * The Handlebars template used by the handler for rendering responses to requests by browser clients.
     */
    private final String myTokenResponseTemplate = "src/main/resources/templates/token.hbs";

    /**
     * The id of the HTML element that contains the client IP address that was put in the cookie.
     */
    private final String myClientIpAddressID = "client-ip-address";

    /**
     * Tests that a browser client can use a valid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowser(final Vertx aVertx, final VertxTestContext aContext) {
        final String getCookieRequestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, getCookieRequestURI);

        getCookie.send().compose(result -> {
            final String cookieHeader = result.cookies().get(0);
            final String cookieValue = cookieHeader.split(EQUALS)[1];
            final String clientIpAddress =
                    Jsoup.parse(result.bodyAsString()).getElementById(myClientIpAddressID).text();

            return myAccessCookieService.decryptCookie(cookieValue, clientIpAddress).compose(cookie -> {
                final String getTokenRequestURI = StringUtils.format(GET_TOKEN_PATH, myGetTokenRequestQuery);
                final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, getTokenRequestURI)
                        .putHeader(HttpHeaders.COOKIE.toString(), cookieHeader);

                return getToken.send().onSuccess(response -> {
                    final JsonObject expectedAccessTokenDecoded =
                            new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION)).put(
                                    TokenJsonKeys.CAMPUS_NETWORK, cookie.getBoolean(CookieJsonKeys.CAMPUS_NETWORK));
                    final String expectedAccessToken =
                            Base64.getEncoder().encodeToString(expectedAccessTokenDecoded.encode().getBytes());
                    final Optional<Integer> expectedExpiresIn =
                            Optional.ofNullable(myConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
                    final JsonObject expectedJsonWrapper = new JsonObject();
                    final JsonObject templateData = new JsonObject();
                    final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

                    // Insertion order must be the same as in the handler
                    expectedJsonWrapper.put(ResponseJsonKeys.ACCESS_TOKEN, expectedAccessToken);
                    expectedExpiresIn.ifPresent(expiry -> expectedJsonWrapper.put(ResponseJsonKeys.EXPIRES_IN, expiry));
                    expectedJsonWrapper.put(ResponseJsonKeys.MESSAGE_ID, myMessageID);

                    templateData.put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedJsonWrapper).put(TemplateKeys.ORIGIN,
                            TEST_ORIGIN);

                    templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                        assertEquals(HTTP.OK, response.statusCode());
                        assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                        assertEquals(expected, response.bodyAsBuffer());

                        aContext.completeNow();
                    }).onFailure(aContext::failNow);
                }).onFailure(aContext::failNow);
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a non-browser client can use a valid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenNonBrowser(final Vertx aVertx, final VertxTestContext aContext) {
        final String getCookieRequestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, getCookieRequestURI);

        getCookie.send().compose(result -> {
            final String cookieHeader = result.cookies().get(0);
            final String cookieValue = cookieHeader.split(EQUALS)[1];
            final String clientIpAddress =
                    Jsoup.parse(result.bodyAsString()).getElementById(myClientIpAddressID).text();

            return myAccessCookieService.decryptCookie(cookieValue, clientIpAddress).compose(cookie -> {
                final String getTokenRequestURI = StringUtils.format(GET_TOKEN_PATH, EMPTY);
                final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, getTokenRequestURI)
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
     * Tests that a browser client can't use an invalid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowserInvalidCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_TOKEN_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), myInvalidCookieHeader);

        getToken.send().onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a non-browser client can't use an invalid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenNonBrowserInvalidCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_TOKEN_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), myInvalidCookieHeader);

        getToken.send().onSuccess(response -> {
            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
