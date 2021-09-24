
package edu.ucla.library.iiif.auth.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.veqryn.collect.Cidr4Trie;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ip4;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests the {@link AccessCookieHandler}.
 */
@ExtendWith(VertxExtension.class)
public class AccessCookieHandlerTest {

    /**
     * Tests the IP address subnet check.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testIsOnNetwork(final Vertx aVertx, final VertxTestContext aContext) {
        final Cidr4Trie<String> network = new Cidr4Trie<>();
        final String[] subnets = { "192.168.0.0/24", "127.0.0.0/16" };
        final String[] onNetwork = { "192.168.0.1", "127.0.0.1", "127.0.1.1" };
        final String[] offNetwork = { "192.168.1.1", "127.1.0.1", "127.1.1.1" };

        for (final String subnet : subnets) {
            network.put(new Cidr4(subnet), subnet);
        }

        try {
            for (final String ip : onNetwork) {
                assertTrue(AccessCookieHandler.isOnNetwork(new Ip4(ip), network));
            }
            for (final String ip : offNetwork) {
                assertFalse(AccessCookieHandler.isOnNetwork(new Ip4(ip), network));
            }
            aContext.completeNow();
        } catch (final Exception details) {
            aContext.failNow(details);
        }
    }

}
