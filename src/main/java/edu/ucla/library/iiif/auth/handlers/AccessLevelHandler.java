
package edu.ucla.library.iiif.auth.handlers;

import info.freelibrary.util.HTTP;

import edu.ucla.library.iiif.auth.Param;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that handles item access level requests.
 */
public class AccessLevelHandler implements Handler<RoutingContext> {

    /**
     * The service proxy for accessing the database.
     */
    private final DatabaseService myDatabaseServiceProxy;

    /**
     * Creates a handler that checks the access level of an ID.
     *
     * @param aVertx The Vert.x instance
     */
    public AccessLevelHandler(final Vertx aVertx) {
        myDatabaseServiceProxy = DatabaseService.createProxy(aVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final String id = aContext.request().getParam(Param.ID);

        myDatabaseServiceProxy.getAccessLevel(id).onSuccess(accessLevel -> {
            final boolean isRestricted = accessLevel == 0 ? false : true;
            final JsonObject data = new JsonObject().put(ResponseJsonKeys.ID, id).put(ResponseJsonKeys.RESTRICTED,
                    isRestricted);

            aContext.response().setStatusCode(HTTP.OK)
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .end(data.encodePrettily());
        }).onFailure(aContext::fail);
    }
}
