
package edu.ucla.library.iiif.auth.utils;

/**
 * Constants used in testing.
 */
@SuppressWarnings({ "PMD.CommentSize", "checkstyle:lineLengthChecker" })
public final class TestConstants {

    public static final String INADDR_ANY = "0.0.0.0";

    /**
     * A test initialization vector used to encrypt {@link #TEST_SINAI_AUTHENTICATED_3DAY}. This is just the value
     * "0123456789ABCDEF" (see Ruby script below) encoded in hexadecimal.
     */
    public static final String TEST_INITIALIZATION_VECTOR = "30313233343536373839414243444546";

    /**
     * A test cookie generated using the following Ruby code, mocking the relevant part of the Sinai application:
     * <p>
     *
     * <pre>
     * #!/usr/bin/env ruby
     *
     * require "openssl"
     *
     * cipher = OpenSSL::Cipher::AES256.new :CBC
     * cipher.encrypt
     * cipher.key = "ThisPasswordIsReallyHardToGuess!"
     * cipher.iv = "0123456789ABCDEF"
     * puts (cipher.update("Authenticated #{Time.at(0).utc}") + cipher.final).unpack("H*")[0].upcase
     * </pre>
     *
     * @see <a href=
     *      "https://github.com/UCLALibrary/sinaimanuscripts/blob/44cbbd9bf508c32b742f1617205a679edf77603e/app/controllers/application_controller.rb#L98-L103">How
     *      the Sinai application encodes cookies</a>
     */
    public static final String TEST_SINAI_AUTHENTICATED_3DAY =
            "5AFF80488740353F8A11B99C7A493D871807521908500772B92E4F8FC919E305A607ADB714B22EF08D2C22FC08C8A6EC";

    /*
     * Constant classes have private constructors.
     */
    private TestConstants() {
    }

}
