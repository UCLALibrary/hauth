
package edu.ucla.library.iiif.auth.verticles;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Op;
import edu.ucla.library.iiif.auth.handlers.StatusHandler;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends AbstractVerticle {

    /**
     * The map of verticle names and deployment IDs.
     */
    static final String VERTICLES_MAP = "verticles.map";

    /**
     * The main verticle's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MessageCodes.BUNDLE);

    /**
     * The location of the OpenAPI specification.
     */
    private static final String DEFAULT_API_SPEC = "hauth.yaml";

    /**
     * The default host at which the application runs.
     */
    private static final String DEFAULT_HOST = "0.0.0.0"; // NOPMD

    /**
     * The default port at which the application runs.
     */
    private static final int DEFAULT_PORT = 8888;

    /**
     * The HTTP server for the Hauth service.
     */
    private HttpServer myServer;

    @Override
    public void start(final Promise<Void> aPromise) {
        ConfigRetriever.create(vertx).getConfig()
                .onSuccess(config -> configureServer(config.mergeIn(config()), aPromise))
                .onFailure(error -> aPromise.fail(error));
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        myServer.close().onSuccess(result -> aPromise.complete()).onFailure(error -> aPromise.fail(error));
    }

    /**
     * Configure the application server.
     *
     * @param aConfig A JSON configuration
     * @param aPromise A startup promise
     */
    private void configureServer(final JsonObject aConfig, final Promise<Void> aPromise) {
        final String apiSpec = aConfig.getString(Config.API_SPEC, DEFAULT_API_SPEC);
        final String host = aConfig.getString(Config.HTTP_HOST, DEFAULT_HOST);
        final int port = aConfig.getInteger(Config.HTTP_PORT, DEFAULT_PORT);

        RouterBuilder.create(vertx, apiSpec).onComplete(routerConfig -> {
            if (routerConfig.succeeded()) {
                final HttpServerOptions serverOptions = new HttpServerOptions().setPort(port).setHost(host);
                final RouterBuilder routerBuilder = routerConfig.result();

                // Associate handlers with operation IDs from the application's OpenAPI specification
                routerBuilder.operation(Op.GET_STATUS).handler(new StatusHandler(getVertx()));

                myServer = getVertx().createHttpServer(serverOptions).requestHandler(routerBuilder.createRouter());
                myServer.listen().onSuccess(result -> {
                    LOGGER.info(MessageCodes.AUTH_001, port);
                    aPromise.complete();
                }).onFailure(error -> aPromise.fail(error));
            } else {
                aPromise.fail(routerConfig.cause());
            }
        });
    }
}
