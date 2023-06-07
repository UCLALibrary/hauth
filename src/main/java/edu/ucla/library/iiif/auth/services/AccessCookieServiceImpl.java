
package edu.ucla.library.iiif.auth.services;

import static info.freelibrary.util.Constants.SPACE;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

/**
 * The implementation of AccessCookieService.
 * <p>
 * Standard algorithm names are defined in Java's
 * <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">documentation</a>.
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class AccessCookieServiceImpl implements AccessCookieService {

    /**
     * The name of the algorithm to use for key derivation.
     */
    public static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA256";

    /**
     * The name of the secret key algorithm.
     */
    public static final String KEY_ALGORITHM = "AES";

    /**
     * The name of the cipher transformation.
     */
    public static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /**
     * The date format used in Sinai cookies.
     */
    public static final String SINAI_COOKIE_DATE_FORMAT = "EEE, dd MMM yyyy";

    /**
     * The access cookie service impl's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessCookieServiceImpl.class, MessageCodes.BUNDLE);

    /**
     * The failure code to use for a ServiceException that represents {@link Error#CONFIGURATION}.
     */
    private static final int CONFIGURATION_ERROR = Error.CONFIGURATION.ordinal();

    /**
     * The failure code to use for a ServiceException that represents {@link Error#INVALID_COOKIE}.
     */
    private static final int INVALID_COOKIE_ERROR = Error.INVALID_COOKIE.ordinal();

    /**
     * A reference to the configuration.
     */
    private final JsonObject myConfig;

    /**
     * The cryptographic function to use.
     */
    private final Cipher myCipher;

    /**
     * The secret key used for encryption and decryption of cookies created by this application.
     */
    private final Key mySecretKey;

    /**
     * The RNG used for generating initialization vectors for encryption.
     */
    private final SecureRandom myInitializationVectorRng;

    /**
     * The secret key used for decrypting Sinai cookies.
     */
    private final Key mySecretKeySinai;

    /**
     * The string that must prefix each decrypted Sinai cookie value.
     */
    private final String mySinaiCookieValidPrefix;

    /**
     * A formatter for the date in a Sinai cookie.
     */
    private final DateTimeFormatter mySinaiCookieDateFormatter;

    /**
     * Creates an instance of the service. For reference material on the key derivation crypto, see RFC 8018:
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-4.1">Salt</a>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-4.2">Iteration count</a>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-5.2">PBKDF2</a>
     * </ul>
     *
     * @param aConfig A configuration
     * @throws InvalidKeySpecException if the {@link KeySpec} was not instantiated correctly
     * @throws NoSuchAlgorithmException if either {@link KEY_DERIVATION_FUNCTION} or {@link CIPHER_TRANSFORMATION} are
     *         not valid algorithms
     * @throws NoSuchPaddingException if {@link CIPHER_TRANSFORMATION} contains a padding scheme that is not available
     */
    AccessCookieServiceImpl(final JsonObject aConfig)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException {
        final String password = aConfig.getString(Config.SECRET_KEY_PASSWORD);
        final String salt = aConfig.getString(Config.SECRET_KEY_SALT);
        final int iterationCount = 65_536;
        final int keyLength = 256;

        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION);
            final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterationCount, keyLength);

            mySecretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_ALGORITHM);
            myCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        } catch (final InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException details) {
            LOGGER.error(MessageCodes.AUTH_010, details.getMessage());

            throw details;
        }

        myConfig = aConfig;
        myInitializationVectorRng = new SecureRandom();
        mySecretKeySinai =
                new SecretKeySpec(aConfig.getString(Config.SINAI_COOKIE_SECRET_KEY_PASSWORD).getBytes(), KEY_ALGORITHM);
        mySinaiCookieValidPrefix = aConfig.getString(Config.SINAI_COOKIE_VALID_PREFIX);
        mySinaiCookieDateFormatter = DateTimeFormatter.ofPattern(SINAI_COOKIE_DATE_FORMAT);
    }

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }

    @Override
    public Future<String> generateCookie(final String aClientIpAddress, final boolean aIsOnCampusNetwork) {
        final JsonObject cookieData = new JsonObject().put(CookieJsonKeys.CLIENT_IP_ADDRESS, aClientIpAddress)
                .put(CookieJsonKeys.CAMPUS_NETWORK, aIsOnCampusNetwork);
        final byte[] encryptedCookieData;
        final JsonObject unencodedCookie;
        final String cookie;

        try {
            myCipher.init(Cipher.ENCRYPT_MODE, mySecretKey, myInitializationVectorRng);
            encryptedCookieData = myCipher.doFinal(cookieData.encode().getBytes());
        } catch (final BadPaddingException | IllegalBlockSizeException | InvalidKeyException details) {
            return Future.failedFuture(new ServiceException(CONFIGURATION_ERROR, details.getMessage()));
        }

        // Vert.x JsonObject knows how to encode/decode byte arrays, so we can use them as-is
        unencodedCookie = new JsonObject().put(CookieJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                .put(CookieJsonKeys.SECRET, encryptedCookieData).put(CookieJsonKeys.NONCE, myCipher.getIV());
        cookie = Base64.getEncoder().encodeToString(unencodedCookie.encode().getBytes());

        return Future.succeededFuture(cookie);
    }

    @Override
    public Future<JsonObject> decryptCookie(final String aCookieValue, final String aClientIpAddress) {
        final JsonObject cookieData;
        final String expectedClientIpAddress;

        try {
            final JsonObject decodedCookie = new JsonObject(new String(Base64.getDecoder().decode(aCookieValue)));
            final byte[] encryptedCookieData = decodedCookie.getBinary(CookieJsonKeys.SECRET);
            final byte[] nonce = decodedCookie.getBinary(CookieJsonKeys.NONCE);
            final byte[] serializedCookieData;

            myCipher.init(Cipher.DECRYPT_MODE, mySecretKey, new IvParameterSpec(nonce));

            serializedCookieData = myCipher.doFinal(encryptedCookieData);
            cookieData = new JsonObject(new String(serializedCookieData));
        } catch (final IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException details) {
            // This code should never be reached, since only we're doing the encryption
            return Future.failedFuture(new ServiceException(CONFIGURATION_ERROR, details.getMessage()));
        } catch (final BadPaddingException | ClassCastException | DecodeException | IllegalArgumentException details) {
            // Cookie was tampered with, stolen, or is otherwise invalid
            return Future.failedFuture(new ServiceException(INVALID_COOKIE_ERROR, details.getMessage()));
        }

        expectedClientIpAddress = cookieData.getString(CookieJsonKeys.CLIENT_IP_ADDRESS);

        if (!aClientIpAddress.equals(expectedClientIpAddress)) {
            // Cookie was stolen
            LOGGER.error(MessageCodes.AUTH_013, aClientIpAddress, expectedClientIpAddress);
            return Future
                    .failedFuture(new ServiceException(INVALID_COOKIE_ERROR, LOGGER.getMessage(MessageCodes.AUTH_011)));
        }
        return Future.succeededFuture(cookieData);
    }

    @Override
    public Future<Void> validateSinaiCookie(final String aAuthCookieValue, final String aIvCookieValue) {
        try {
            final byte[] encryptedCookieData = Hex.decodeHex(aAuthCookieValue);
            final byte[] nonce = Hex.decodeHex(aIvCookieValue);
            final byte[] rawCookieData;
            final String[] cookieData;
            final String prefix;
            final LocalDate lastValidDate;

            myCipher.init(Cipher.DECRYPT_MODE, mySecretKeySinai, new IvParameterSpec(nonce));

            rawCookieData = myCipher.doFinal(encryptedCookieData);
            cookieData = new String(rawCookieData).split(SPACE, 2);

            // The prefix must match exactly, and the date must not be more than three days ago
            prefix = cookieData[0];
            // TODO: the Sinai cookie should contain a more specific timestamp so that any given cookie is considered
            // valid for exactly 72 hours after creation
            lastValidDate = LocalDate.parse(cookieData[1], mySinaiCookieDateFormatter).plusDays(3);

            if (prefix.equals(mySinaiCookieValidPrefix) && !LocalDate.now().isAfter(lastValidDate)) {
                return Future.succeededFuture();
            }
            return Future.failedFuture(new ServiceException(INVALID_COOKIE_ERROR,
                    LOGGER.getMessage(MessageCodes.AUTH_012, lastValidDate.plusDays(1).toString())));
        } catch (final IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException details) {
            // This code should never be reached, assuming we've configured the application properly
            return Future.failedFuture(new ServiceException(CONFIGURATION_ERROR, details.getMessage()));
        } catch (final BadPaddingException | ClassCastException | DateTimeException | DecoderException |
                IllegalArgumentException | IndexOutOfBoundsException details) {
            // Cookie was tampered with or is otherwise invalid
            return Future.failedFuture(new ServiceException(INVALID_COOKIE_ERROR, details.getMessage()));
        }
    }
}
