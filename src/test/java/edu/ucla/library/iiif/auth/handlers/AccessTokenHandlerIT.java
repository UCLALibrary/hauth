
package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.EMPTY;
import static info.freelibrary.util.Constants.EQUALS;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import edu.ucla.library.iiif.auth.AccessTokenError;
import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
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
public final class AccessTokenHandlerIT extends AbstractAccessTokenHandlerIT {

    /**
     * The invalid cookie to test with.
     */
    private final String myInvalidCookieHeader = "iiif-access=invalid";

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
     * Tests that a browser client can use a valid access cookie to obtain an access token when the app is deployed
     * behind a reverse proxy.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowserReverseProxyDeployment(final Vertx aVertx, final VertxTestContext aContext) {
        final String getCookieRequestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, getCookieRequestURI)
                .putHeader(CLIENT_IP_HEADER_NAME, CLIENT_IP_HEADER_VALUE);

        getCookie.send().compose(result -> {
            final String cookieHeader = result.cookies().get(0);
            final String cookieValue = cookieHeader.split(EQUALS)[1];

            return myAccessCookieService.decryptCookie(cookieValue, CLIENT_IP).compose(cookie -> {
                final String getTokenRequestURI = StringUtils.format(GET_TOKEN_PATH, myGetTokenRequestQuery);
                final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, getTokenRequestURI)
                        .putHeader(HttpHeaders.COOKIE.toString(), cookieHeader)
                        .putHeader(CLIENT_IP_HEADER_NAME, CLIENT_IP_HEADER_VALUE);

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
     * Tests that a non-browser client can use a valid access cookie to obtain an access token when the app is deployed
     * behind a reverse proxy.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenNonBrowserReverseProxyDeployment(final Vertx aVertx, final VertxTestContext aContext) {
        final String getCookieRequestURI =
                StringUtils.format(GET_COOKIE_PATH, URLEncoder.encode(TEST_ORIGIN, StandardCharsets.UTF_8));
        final HttpRequest<?> getCookie = myWebClient.get(myPort, TestConstants.INADDR_ANY, getCookieRequestURI)
                .putHeader(CLIENT_IP_HEADER_NAME, CLIENT_IP_HEADER_VALUE);

        getCookie.send().compose(result -> {
            final String cookieHeader = result.cookies().get(0);
            final String cookieValue = cookieHeader.split(EQUALS)[1];

            return myAccessCookieService.decryptCookie(cookieValue, CLIENT_IP).compose(cookie -> {
                final String getTokenRequestURI = StringUtils.format(GET_TOKEN_PATH, EMPTY);
                final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, getTokenRequestURI)
                        .putHeader(HttpHeaders.COOKIE.toString(), cookieHeader)
                        .putHeader(CLIENT_IP_HEADER_NAME, CLIENT_IP_HEADER_VALUE);

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
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);
            final JsonObject templateData = new JsonObject() //
                    .put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedError) //
                    .put(TemplateKeys.ORIGIN, TEST_ORIGIN);
            final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

            templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsBuffer());

                aContext.completeNow();
            }).onFailure(aContext::failNow);
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
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);

            assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(expectedError, response.bodyAsJsonObject());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a browser client must provide an access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowserMissingCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String getTokenRequestURI = StringUtils.format(GET_TOKEN_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, getTokenRequestURI);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.missingCredentials);
            final JsonObject templateData = new JsonObject() //
                    .put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedError) //
                    .put(TemplateKeys.ORIGIN, TEST_ORIGIN);
            final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

            templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsBuffer());

                aContext.completeNow();
            }).onFailure(aContext::failNow);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a non-browser client must provide an access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenNonBrowserMissingCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_TOKEN_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, TestConstants.INADDR_ANY, requestURI);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.missingCredentials);

            assertEquals(HTTP.BAD_REQUEST, response.statusCode());
            assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
            assertEquals(expectedError, response.bodyAsJsonObject());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
