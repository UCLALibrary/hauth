
package edu.ucla.library.iiif.auth.handlers;

import static info.freelibrary.util.Constants.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import info.freelibrary.util.Constants;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.AccessTokenError;
import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.TemplateKeys;
import edu.ucla.library.iiif.auth.TokenJsonKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Tuple;

/**
 * Tests {@link SinaiAccessTokenHandler#handle}.
 */
public final class SinaiAccessTokenHandlerIT extends AbstractAccessTokenHandlerIT {

    /**
     * The cookie header template to use in Sinai access token requests.
     */
    private final String mySinaiCookieHeaderTemplate = "{}={}; {}={}";

    /**
     * The invalid cookie to test with.
     */
    private final String myInvalidCookieHeader =
            StringUtils.format(mySinaiCookieHeaderTemplate, CookieNames.SINAI_CIPHERTEXT,
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                    CookieNames.SINAI_IV, "30313233343536373839414243444546");

    /**
     * Tests that a browser client can use a valid access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowser(final Vertx aVertx, final VertxTestContext aContext) {
        final String getTokenRequestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, getTokenRequestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), getSinaiCookieHeader(myMockSinaiCookies));

        getToken.send().onSuccess(response -> {
            final JsonObject expectedAccessTokenDecoded =
                    new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.SINAI_AFFILIATE, true);
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
                aContext.verify(() -> {
                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                    assertEquals(expected, response.bodyAsBuffer());

                    aContext.completeNow();
                });
            }).onFailure(aContext::failNow);
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
        final String getTokenRequestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, getTokenRequestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), getSinaiCookieHeader(myMockSinaiCookies));

        getToken.send().onSuccess(response -> {
            final JsonObject expectedAccessTokenDecoded =
                    new JsonObject().put(TokenJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                            .put(TokenJsonKeys.SINAI_AFFILIATE, true);
            final String expectedAccessToken =
                    Base64.getEncoder().encodeToString(expectedAccessTokenDecoded.encode().getBytes());
            final Optional<Integer> expectedExpiresIn =
                    Optional.ofNullable(myConfig.getInteger(Config.ACCESS_TOKEN_EXPIRES_IN));
            final JsonObject expected = new JsonObject().put(ResponseJsonKeys.ACCESS_TOKEN, expectedAccessToken);

            expectedExpiresIn.ifPresent(expiry -> expected.put(ResponseJsonKeys.EXPIRES_IN, expiry));

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expected, response.bodyAsJsonObject());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a browser client can't use an expired access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenBrowserExpiredCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), getSinaiCookieHeader(myMockSinaiCookiesExpired));

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);
            final JsonObject templateData = new JsonObject() //
                    .put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedError) //
                    .put(TemplateKeys.ORIGIN, TEST_ORIGIN);
            final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

            templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                aContext.verify(() -> {
                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                    assertEquals(expected, response.bodyAsBuffer());

                    aContext.completeNow();
                });
            }).onFailure(aContext::failNow);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a non-browser client can't use an expired access cookie to obtain an access token.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetTokenNonBrowserExpiredCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), getSinaiCookieHeader(myMockSinaiCookiesExpired));

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);

            aContext.verify(() -> {
                assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expectedError, response.bodyAsJsonObject());

                aContext.completeNow();
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
        final String requestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), myInvalidCookieHeader);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);
            final JsonObject templateData = new JsonObject() //
                    .put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedError) //
                    .put(TemplateKeys.ORIGIN, TEST_ORIGIN);
            final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

            templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                aContext.verify(() -> {
                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                    assertEquals(expected, response.bodyAsBuffer());

                    aContext.completeNow();
                });
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
        final String requestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI)
                .putHeader(HttpHeaders.COOKIE.toString(), myInvalidCookieHeader);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.invalidCredentials);

            aContext.verify(() -> {
                assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expectedError, response.bodyAsJsonObject());

                aContext.completeNow();
            });
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
        final String getTokenRequestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, myGetTokenRequestQuery);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, getTokenRequestURI);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.missingCredentials);
            final JsonObject templateData = new JsonObject() //
                    .put(TemplateKeys.ACCESS_TOKEN_OBJECT, expectedError) //
                    .put(TemplateKeys.ORIGIN, TEST_ORIGIN);
            final HandlebarsTemplateEngine templateEngine = HandlebarsTemplateEngine.create(aVertx);

            templateEngine.render(templateData, myTokenResponseTemplate).onSuccess(expected -> {
                aContext.verify(() -> {
                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals(MediaType.TEXT_HTML.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                    assertEquals(expected, response.bodyAsBuffer());

                    aContext.completeNow();
                });
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
        final String requestURI = StringUtils.format(GET_TOKEN_SINAI_PATH, EMPTY);
        final HttpRequest<?> getToken = myWebClient.get(myPort, Constants.INADDR_ANY, requestURI);

        getToken.send().onSuccess(response -> {
            final JsonObject expectedError = new JsonObject() //
                    .put(ResponseJsonKeys.ERROR, AccessTokenError.missingCredentials);

            aContext.verify(() -> {
                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().get(HttpHeaders.CONTENT_TYPE));
                assertEquals(expectedError, response.bodyAsJsonObject());

                aContext.completeNow();
            });
        }).onFailure(aContext::failNow);
    }

    /**
     * Gets a cookie header to use in Sinai access token requests.
     *
     * @param aSinaiCookieTuple A tuple that contains all cookies required for Sinai authentication
     * @return The cookie header
     */
    private String getSinaiCookieHeader(final Tuple aSinaiCookieTuple) {
        return StringUtils.format(mySinaiCookieHeaderTemplate, CookieNames.SINAI_CIPHERTEXT,
                aSinaiCookieTuple.getString(0), CookieNames.SINAI_IV, aSinaiCookieTuple.getString(1));
    }
}
