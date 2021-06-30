
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;

import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that handles item access level requests.
 */
public class AccessLevelHandler implements Handler<RoutingContext> {

    /**
     * The shared Vert.x instance.
     */
    @SuppressWarnings({ "PMD.UnusedPrivateField", "unused", "PMD.SingularField" })
    private final Vertx myVertx;

    /**
     * Creates a handler that checks the access level of an ID.
     *
     * @param aVertx The Vert.x instance
     */
    public AccessLevelHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final HttpServerRequest request = aContext.request();
        final JsonObject info = new JsonObject();
        // final String id = request.getParam(Param.ID);

        info.put("status", "ok");

        response.setStatusCode(HTTP.OK);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).end(info.encodePrettily());
    }

}
