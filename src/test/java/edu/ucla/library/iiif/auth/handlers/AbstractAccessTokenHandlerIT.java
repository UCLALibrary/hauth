
package edu.ucla.library.iiif.auth.handlers;

import java.util.Random;

import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.Param;

/**
 * A base class for access token handler integration tests.
 */
public abstract class AbstractAccessTokenHandlerIT extends AbstractHandlerIT {

    /**
     * Obtains a random unsigned integer by zeroing the sign bit of a random signed integer.
     */
    protected final String myMessageID = String.valueOf(new Random().nextInt() & 0x7FFFFFFF);

    /**
     * The query string to use for token requests by browser clients.
     */
    protected final String myGetTokenRequestQuery =
            StringUtils.format("{}={}&{}={}", Param.MESSAGE_ID, myMessageID, Param.ORIGIN, TEST_ORIGIN);

    /**
     * The Handlebars template used by the handler for rendering responses to requests by browser clients.
     */
    protected final String myTokenResponseTemplate = "src/main/resources/templates/token.hbs";
}
