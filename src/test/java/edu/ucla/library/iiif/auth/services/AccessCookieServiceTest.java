
package edu.ucla.library.iiif.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.utils.TestUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.Tuple;

/**
 * Tests the {@link AccessCookieService}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class AccessCookieServiceTest extends AbstractServiceTest {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessCookieServiceTest.class, MessageCodes.BUNDLE);

    /**
     * The IP address 127.0.0.1.
     */
    private static final String LOCALHOST = "127.0.0.1";

    /**
     * The service proxy for testing typical client usage.
     */
    private AccessCookieService myServiceProxy;

    /**
     * Only used for event bus unregistration.
     */
    private MessageConsumer<JsonObject> myService;

    /**
     * Mock values for a pair of Sinai cookies that were created just moments ago.
     */
    private Tuple myMockSinaiCookiesFresh;

    /**
     * MOck values for a pair of Sinai cookies that were created three days ago.
     */
    private Tuple myMockSinaiCookies3DaysOld;

    /**
     * Mock values for a pair of Sinai cookies that were created four days ago.
     */
    private Tuple myMockSinaiCookies4DaysOld;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        ConfigRetriever.create(aVertx).getConfig().onSuccess(config -> {
            try {
                // In order to test the service proxy, we need to instantiate the service first
                final AccessCookieService service = AccessCookieService.create(config);
                final ServiceBinder binder = new ServiceBinder(aVertx);

                // We also need some Sinai cookies to test with
                try {
                    myMockSinaiCookiesFresh = TestUtils.getMockSinaiCookies(config, LocalDate.now());
                    myMockSinaiCookies3DaysOld = TestUtils.getMockSinaiCookies(config, LocalDate.now().minusDays(3));
                    myMockSinaiCookies4DaysOld = TestUtils.getMockSinaiCookies(config, LocalDate.now().minusDays(4));
                } catch (final Exception details) {
                    aContext.failNow(details);
                    return;
                }

                // Register the service on the event bus, and keep a reference to it so it can be unregistered later
                myService = binder.setAddress(AccessCookieService.ADDRESS).register(AccessCookieService.class, service);

                // Now we can instantiate a proxy to the service
                myServiceProxy = AccessCookieService.createProxy(aVertx);

                aContext.completeNow();
            } catch (final GeneralSecurityException details) {
                aContext.failNow(details);
            }
        }).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterAll
    public final void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        // Close the service proxy, then unregister the service (order important)
        myServiceProxy.close().compose(result -> myService.unregister()).onSuccess(success -> aContext.completeNow())
                .onFailure(aContext::failNow);
    }

    /**
     * Tests cookie generation and decryption.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testValidateGeneratedCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String clientIpAddress = LOCALHOST;
        final boolean isCampusNetwork = true;
        final boolean isDegradedAllowed = false;
        final Future<String> generateCookie =
                myServiceProxy.generateCookie(clientIpAddress, isCampusNetwork, isDegradedAllowed);

        generateCookie.compose(cookie -> {
            // The result is base64-encoded JSON with three keys
            final JsonObject decodedCookie = new JsonObject(new String(Base64.getDecoder().decode(cookie.getBytes())));

            for (final String key : List.of(CookieJsonKeys.VERSION, CookieJsonKeys.SECRET, CookieJsonKeys.NONCE)) {
                assertTrue(decodedCookie.containsKey(key));
            }

            return myServiceProxy.decryptCookie(cookie, clientIpAddress);
        }).onSuccess(decryptedCookie -> {
            final JsonObject expected = new JsonObject().put(CookieJsonKeys.CLIENT_IP_ADDRESS, clientIpAddress)
                    .put(CookieJsonKeys.CAMPUS_NETWORK, isCampusNetwork)
                    .put(CookieJsonKeys.DEGRADED_ALLOWED, isDegradedAllowed);

            completeIfExpectedElseFail(decryptedCookie, expected, aContext);
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a tampered cookie will not be accepted.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    public final void testInvalidateTamperedCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final String clientIpAddress = LOCALHOST;
        final boolean isCampusNetwork = false;
        final boolean isDegradedAllowed = false;
        final Future<String> generateCookie =
                myServiceProxy.generateCookie(clientIpAddress, isCampusNetwork, isDegradedAllowed);

        generateCookie.compose(cookie -> {
            final JsonObject decodedCookie = new JsonObject(new String(Base64.getDecoder().decode(cookie.getBytes())));
            final byte[] secret = decodedCookie.getBinary(CookieJsonKeys.SECRET).clone();
            final String tamperedCookie;

            // Flip some bits in the secret
            secret[new Random().nextInt(secret.length)] += 1;

            decodedCookie.put(CookieJsonKeys.SECRET, secret);
            tamperedCookie = Base64.getEncoder().encodeToString(decodedCookie.encode().getBytes());

            return myServiceProxy.decryptCookie(tamperedCookie, clientIpAddress);
        }).onFailure(details -> {
            final ServiceException error = (ServiceException) details;

            assertEquals(Error.INVALID_COOKIE.ordinal(), error.failureCode());

            aContext.completeNow();
        }).onSuccess(decryptedCookie -> {
            // Somehow we still got syntactically valid JSON; make sure the structure is not semantically valid
            if (decryptedCookie.containsKey(CookieJsonKeys.CLIENT_IP_ADDRESS) &&
                    decryptedCookie.getString(CookieJsonKeys.CLIENT_IP_ADDRESS).equals(clientIpAddress) &&
                    decryptedCookie.containsKey(CookieJsonKeys.CAMPUS_NETWORK) &&
                    decryptedCookie.getBoolean(CookieJsonKeys.CAMPUS_NETWORK) == isCampusNetwork &&
                    decryptedCookie.containsKey(CookieJsonKeys.DEGRADED_ALLOWED) &&
                    decryptedCookie.getBoolean(CookieJsonKeys.DEGRADED_ALLOWED) == isDegradedAllowed) {
                aContext.failNow(StringUtils.format(MessageCodes.AUTH_009, decryptedCookie));
            } else {
                aContext.completeNow();
            }
        });
    }

    /**
     * Tests Sinai cookie validation.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testValidateSinaiCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final Future<Void> validateCookieFresh = validateSinaiCookieTuple(myMockSinaiCookiesFresh);
        final Future<Void> validateCookie3DaysOld = validateSinaiCookieTuple(myMockSinaiCookies3DaysOld);

        CompositeFuture.all(validateCookieFresh, validateCookie3DaysOld).onSuccess(result -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that an expired Sinai cookie will not be accepted.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testInvalidateExpiredSinaiCookie(final Vertx aVertx, final VertxTestContext aContext) {
        final Future<Void> validateCookie4DaysOld = validateSinaiCookieTuple(myMockSinaiCookies4DaysOld);

        validateCookie4DaysOld.onFailure(result -> {
            aContext.completeNow();
        }).onSuccess(result -> {
            aContext.failNow(LOGGER.getMessage(MessageCodes.AUTH_019));
        });
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Helper method for validating a pair of Sinai cookies.
     *
     * @param aCookieTuple A pair of Sinai cookies returned from
     *        {@link TestUtils#getMockSinaiCookies(JsonObject, LocalDate)}
     * @return The result of validating the cookies
     */
    private Future<Void> validateSinaiCookieTuple(final Tuple aCookieTuple) {
        return myServiceProxy.validateSinaiCookie(aCookieTuple.getString(0), aCookieTuple.getString(1));
    }
}
