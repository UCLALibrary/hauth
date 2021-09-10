
package edu.ucla.library.iiif.auth.verticles;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.NoSuchPaddingException;

import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Op;
import edu.ucla.library.iiif.auth.handlers.AccessCookieHandler;
import edu.ucla.library.iiif.auth.handlers.StatusHandler;
import edu.ucla.library.iiif.auth.services.AccessCookieCryptoService;
import edu.ucla.library.iiif.auth.services.DatabaseService;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;

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

    /**
     * The database service.
     */
    private MessageConsumer<JsonObject> myDatabaseService;

    /**
     * The access cookie crypto service.
     */
    private MessageConsumer<JsonObject> myAccessCookieCryptoService;

    @Override
    public void start(final Promise<Void> aPromise) {
        ConfigRetriever.create(vertx).getConfig().onSuccess(config -> {
            try {
                configureServer(config.mergeIn(config()), aPromise);
            } catch (final InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException
                    | NoSuchPaddingException details) {
                aPromise.fail(details);
            }
        }).onFailure(aPromise::fail);
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        final Future<Void> stopAll = CompositeFuture
                .all(myDatabaseService.unregister(), myAccessCookieCryptoService.unregister())
                .compose(result -> myServer.close());

        stopAll.onSuccess(aPromise::complete).onFailure(aPromise::fail);
    }

    /**
     * Configure the application server.
     *
     * @param aConfig A JSON configuration
     * @param aPromise A startup promise
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    private void configureServer(final JsonObject aConfig, final Promise<Void> aPromise)
            throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException {
        final String apiSpec = aConfig.getString(Config.API_SPEC, DEFAULT_API_SPEC);
        final String host = aConfig.getString(Config.HTTP_HOST, DEFAULT_HOST);
        final int port = aConfig.getInteger(Config.HTTP_PORT, DEFAULT_PORT);
        final ServiceBinder serviceBinder = new ServiceBinder(getVertx());

        // Register the services on the event bus, and keep a reference to them so they can be unregistered later
        myDatabaseService = serviceBinder.setAddress(DatabaseService.ADDRESS)
                .register(DatabaseService.class, DatabaseService.create(getVertx(), aConfig));
        myAccessCookieCryptoService = serviceBinder.setAddress(AccessCookieCryptoService.ADDRESS)
                .register(AccessCookieCryptoService.class, AccessCookieCryptoService.create(aConfig));

        RouterBuilder.create(vertx, apiSpec).onComplete(routerConfig -> {
            if (routerConfig.succeeded()) {
                final HttpServerOptions serverOptions = new HttpServerOptions().setPort(port).setHost(host);
                final RouterBuilder routerBuilder = routerConfig.result();

                // Associate handlers with operation IDs from the application's OpenAPI specification
                routerBuilder.operation(Op.GET_STATUS).handler(new StatusHandler(getVertx()));
                routerBuilder.operation(Op.GET_COOKIE).handler(new AccessCookieHandler(getVertx(), aConfig));

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
