
package edu.ucla.library.iiif.auth.utils;

import static info.freelibrary.util.Constants.SPACE;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieNames;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.services.AccessCookieServiceImpl;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

/**
 * Utilities to assist with testing.
 */
public final class TestUtils {

    /**
     * The TestUtils' logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class, MessageCodes.BUNDLE);

    /**
     * Creates a utilities class for testing purposes.
     */
    private TestUtils() {
        // This is intentionally left empty.
    }

    /**
     * Gets a pair of Sinai cookies by mocking the cookie generation code of the Sinai application.
     *
     * @param aConfig The application configuration
     * @param aLocalDate The date value to put inside the cookie
     * @return A tuple of size 2 whose first element is a {@link CookieNames#SINAI_CIPHERTEXT} cookie and whose second
     *         is a {@link CookieNames#SINAI_IV} cookie
     * @throws Exception If there is an issue generating the cookies
     */
    @SuppressWarnings({ "checkstyle:LineLength" })
    public static Tuple getMockSinaiCookies(final JsonObject aConfig, final LocalDate aLocalDate) throws Exception {
        // This code mirrors the front-end Ruby code that creates encrypted cookies; see below for the implementation:
        // https://github.com/UCLALibrary/sinaimanuscripts/blob/v2.15.7/app/controllers/application_controller.rb#L98-L103

        final String clearTextPrefix = aConfig.getString(Config.SINAI_COOKIE_VALID_PREFIX);
        final String clearTextSuffix =
                aLocalDate.format(DateTimeFormatter.ofPattern(AccessCookieServiceImpl.SINAI_COOKIE_DATE_FORMAT));
        final String clearText = String.join(SPACE, clearTextPrefix, clearTextSuffix);

        final Cipher cipher = Cipher.getInstance(AccessCookieServiceImpl.CIPHER_TRANSFORMATION);
        final SecretKey key = new SecretKeySpec(aConfig.getString(Config.SINAI_COOKIE_SECRET_KEY_PASSWORD).getBytes(),
                AccessCookieServiceImpl.KEY_ALGORITHM);

        final byte[] cipherText;
        final String sinaiAuthenticated3Day;
        final String initializationVector;

        cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());

        cipherText = cipher.doFinal(clearText.getBytes());
        sinaiAuthenticated3Day = Hex.encodeHexString(cipherText);
        initializationVector = Hex.encodeHexString(cipher.getIV());

        LOGGER.debug(MessageCodes.AUTH_020, CookieNames.SINAI_CIPHERTEXT, sinaiAuthenticated3Day, CookieNames.SINAI_IV,
                initializationVector, clearText);

        return Tuple.of(sinaiAuthenticated3Day, initializationVector);
    }
}
