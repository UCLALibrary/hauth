
package edu.ucla.library.iiif.auth.handlers;

import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import info.freelibrary.util.HTTP;

import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests the {@link AccessLevelHandler}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AccessLevelHandlerTest {

    @Mock
    RoutingContext myEvent;

    @Mock
    HttpServerRequest myRequest;

    @Mock
    HttpServerResponse myResponse;

    /**
     * Set up the tests with what we expect to happen via our mocks.
     */
    @BeforeEach
    public void setup() {
        // Configure the routing event with request and response objects
        // doReturn(myRequest).when(myEvent).request();
        doReturn(myResponse).when(myEvent).response();

        // Have our mock request use this ARK for its "id" parameter
        // doReturn("ark:/21198/zz002dvwr6").when(myRequest).getParam("id");

        // Support Vert.x's fluent design by returning the thing that had its headers set
        doReturn(myResponse).when(myResponse).putHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON.toString());
    }

    /**
     * Tests the handler's successful response.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testHandle(final Vertx aVertx, final VertxTestContext aContext) {
        new AccessLevelHandler(aVertx).handle(myEvent);

        Mockito.verify(myResponse).setStatusCode(HTTP.OK);
        aContext.completeNow();
    }

}
