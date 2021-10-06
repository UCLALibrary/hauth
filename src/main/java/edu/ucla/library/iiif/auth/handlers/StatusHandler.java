
package edu.ucla.library.iiif.auth.handlers;

import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.utils.MediaType;

import info.freelibrary.util.HTTP;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that accepts requests to find out the status of the application.
 */
public class StatusHandler implements Handler<RoutingContext> {

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * Creates a handler that returns the status of the application.
     *
     * @param aVertx A Vert.x instance
     */
    public StatusHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final JsonObject status = new JsonObject();

        status.put(ResponseJsonKeys.STATUS, "ok");

        aContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .setStatusCode(HTTP.OK).end(status.encodePrettily());
    }

    /**
     * Gets the Vert.x instance associated with this handler.
     *
     * @return The Vert.x instance associated with this handler
     */
    public Vertx getVertx() {
        return myVertx;
    }
}
