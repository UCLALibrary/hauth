
package edu.ucla.library.iiif.auth.services;

import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.MessageCodes;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * A base class for service tests.
 */
@ExtendWith(VertxExtension.class)
public abstract class AbstractServiceTest {

    /**
     * The logger used by these tests.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractServiceTest.class, MessageCodes.BUNDLE);

    /**
     * Completes the context if the actual result and the expected result are equal, otherwise fails the context.
     *
     * @param <T> The type of result
     * @param aResult The actual result
     * @param aExpected The expected result
     * @param aContext A test context
     */
    public static <T> void completeIfExpectedElseFail(final T aResult, final T aExpected,
            final VertxTestContext aContext) {
        if (aResult.equals(aExpected)) {
            aContext.completeNow();
        } else {
            aContext.failNow(LOGGER.getMessage(MessageCodes.AUTH_007, aResult, aExpected));
        }
    }
}
