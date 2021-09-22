package edu.ucla.library.iiif.auth.services;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.CookieJsonKeys;

import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

/**
 * The implementation of AccessCookieCryptoService.
 * <p>
 * Algorithm names are defined
 * <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">here</a>.
 */
public class AccessCookieCryptoServiceImpl implements AccessCookieCryptoService {

    /**
     * The name of the algorithm to use for key derivation.
     */
    private static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA256";

    /**
     * The name of the secret key algorithm.
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * The name of the cipher transformation.
     */
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /**
     * A reference to the configuration.
     */
    private final JsonObject myConfig;

    /**
     * The cryptographic function to use.
     */
    private final Cipher myCipher;

    /**
     * The secret key used for encryption and decryption.
     */
    private final Key mySecretKey;

    /**
     * The RNG used for generating initialization vectors for encryption.
     */
    private final SecureRandom myInitializationVectorRng;

    /**
     * Creates an instance of the service.
     *
     * For reference material on the key derivation crypto, see RFC 8018:
     * <ul>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-4.1">Salt</a>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-4.2">Iteration count</a>
     * <li><a href="https://datatracker.ietf.org/doc/html/rfc8018#section-5.2">PBKDF2</a>
     * </ul>
     *
     * @param aConfig A configuration
     * @throws InvalidKeySpecException if the {@link KeySpec} was not instantiated correctly
     * @throws NoSuchAlgorithmException if either {@link KEY_DERIVATION_FUNCTION} or {@link CIPHER_TRANSFORMATION} are
     * not valid algorithms
     * @throws NoSuchPaddingException if {@link CIPHER_TRANSFORMATION} contains a padding scheme that is not available
     */
    AccessCookieCryptoServiceImpl(final JsonObject aConfig)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException {
        final String password = aConfig.getString(Config.SECRET_KEY_GENERATION_PASSWORD);
        final String salt = aConfig.getString(Config.SECRET_KEY_GENERATION_SALT);
        final int iterationCount = 65_536;
        final int keyLength = 256;
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION);
        final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterationCount, keyLength);
        final SecretKey derivedKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_ALGORITHM);

        myConfig = aConfig;
        mySecretKey = derivedKey;
        myCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        myInitializationVectorRng = new SecureRandom();
    }

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }

    @Override
    public Future<String> generateCookie(final String aClientIpAddress, final boolean aIsOnCampusNetwork,
            final boolean aIsDegradedAllowed) {
        final JsonObject cookieData = new JsonObject().put(CookieJsonKeys.CLIENT_IP_ADDRESS, aClientIpAddress)
                .put(CookieJsonKeys.CAMPUS_NETWORK, aIsOnCampusNetwork)
                .put(CookieJsonKeys.DEGRADED_ALLOWED, aIsDegradedAllowed);
        final byte[] encryptedCookieData;
        final JsonObject unencodedCookie;
        final String cookie;

        try {
            myCipher.init(Cipher.ENCRYPT_MODE, mySecretKey, myInitializationVectorRng);
            encryptedCookieData = myCipher.doFinal(cookieData.encode().getBytes());
        } catch (final BadPaddingException | IllegalBlockSizeException | InvalidKeyException details) {
            return Future.failedFuture(new ServiceException(CONFIGURATION_ERROR, details.getMessage()));
        }

        unencodedCookie = new JsonObject().put(CookieJsonKeys.VERSION, myConfig.getString(Config.HAUTH_VERSION))
                .put(CookieJsonKeys.SECRET, encryptedCookieData).put(CookieJsonKeys.NONCE, myCipher.getIV());
        // Vert.x JsonObject automatically base64-encodes binary data
        cookie = Base64.getEncoder().encodeToString(unencodedCookie.encode().getBytes());

        return Future.succeededFuture(cookie);
    }

    @Override
    public Future<JsonObject> decryptCookie(final String aCookieValue) {
        final JsonObject decodedCookie;
        final byte[] encryptedCookieData;
        final byte[] nonce;
        final byte[] serializedCookieData;
        final JsonObject cookieData;

        try {
            decodedCookie = new JsonObject(new String(Base64.getDecoder().decode(aCookieValue)));
            encryptedCookieData = decodedCookie.getBinary(CookieJsonKeys.SECRET);
            nonce = decodedCookie.getBinary(CookieJsonKeys.NONCE);
        } catch (final ClassCastException | DecodeException | IllegalArgumentException details) {
            return Future.failedFuture(new ServiceException(TAMPERED_COOKIE_ERROR, details.getMessage()));
        }

        try {
            myCipher.init(Cipher.DECRYPT_MODE, mySecretKey, new IvParameterSpec(nonce));
            serializedCookieData = myCipher.doFinal(encryptedCookieData);
            cookieData = new JsonObject(new String(serializedCookieData));
        } catch (final IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException details) {
            // This code should never be reached, since only we're doing the encryption
            return Future.failedFuture(new ServiceException(CONFIGURATION_ERROR, details.getMessage()));
        } catch (final BadPaddingException | DecodeException details) {
            // Cookie was tampered with
            return Future.failedFuture(new ServiceException(TAMPERED_COOKIE_ERROR, details.getMessage()));
        }

        return Future.succeededFuture(cookieData);
    }
}
