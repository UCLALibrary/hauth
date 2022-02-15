
package edu.ucla.library.iiif.auth.verticles;

import java.security.GeneralSecurityException;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.AdminAuthenticationProvider;
import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.Error;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Op;
import edu.ucla.library.iiif.auth.ResponseJsonKeys;
import edu.ucla.library.iiif.auth.handlers.AccessCookieHandler;
import edu.ucla.library.iiif.auth.handlers.AccessModeHandler;
import edu.ucla.library.iiif.auth.handlers.AccessTokenHandler;
import edu.ucla.library.iiif.auth.handlers.ItemsHandler;
import edu.ucla.library.iiif.auth.handlers.SinaiAccessTokenHandler;
import edu.ucla.library.iiif.auth.handlers.StatusHandler;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
import edu.ucla.library.iiif.auth.services.DatabaseService;
import edu.ucla.library.iiif.auth.utils.MediaType;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * Main verticle that starts the application.
 */
@SuppressWarnings({ "PMD.ExcessiveImports" })
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
     * The access cookie service.
     */
    private MessageConsumer<JsonObject> myAccessCookieService;

    @Override
    public void start(final Promise<Void> aPromise) {
        ConfigRetriever.create(vertx).getConfig().compose(config -> {
            final String apiSpec = config.getString(Config.API_SPEC, DEFAULT_API_SPEC);
            final String host = config.getString(Config.HTTP_HOST, DEFAULT_HOST);
            final int port = config.getInteger(Config.HTTP_PORT, DEFAULT_PORT);
            final ServiceBinder serviceBinder = new ServiceBinder(getVertx());

            // Register the services on the event bus, and keep references to them so they can be unregistered later
            try {
                myAccessCookieService = serviceBinder.setAddress(AccessCookieService.ADDRESS)
                        .register(AccessCookieService.class, AccessCookieService.create(config));
            } catch (final GeneralSecurityException details) {
                return Future.failedFuture(details);
            }
            myDatabaseService = serviceBinder.setAddress(DatabaseService.ADDRESS).register(DatabaseService.class,
                    DatabaseService.create(getVertx(), config));

            // Load the OpenAPI specification
            return RouterBuilder.create(vertx, apiSpec).compose(routerBuilder -> {
                final Router router;

                // Associate handlers with operation IDs from the OpenAPI spec
                routerBuilder.operation(Op.GET_STATUS).handler(new StatusHandler(getVertx()));
                routerBuilder.operation(Op.GET_ACCESS_MODE).handler(new AccessModeHandler(getVertx()))
                        .failureHandler(AccessModeHandler::handleFailure);
                routerBuilder.operation(Op.GET_COOKIE).handler(new AccessCookieHandler(getVertx(), config));
                routerBuilder.operation(Op.GET_TOKEN).handler(new AccessTokenHandler(getVertx(), config))
                        .failureHandler(AccessTokenHandler::handleFailure);
                routerBuilder.operation(Op.GET_TOKEN_SINAI).handler(new SinaiAccessTokenHandler(getVertx(), config))
                        .failureHandler(SinaiAccessTokenHandler::handleFailure);
                routerBuilder.operation(Op.POST_ITEMS).handler(new ItemsHandler(getVertx()))
                        .failureHandler(ItemsHandler::handleFailure);

                // Add API key authentication for routes that should use the "Admin" security scheme
                routerBuilder.securityHandler("Admin")
                        .bindBlocking(unused -> APIKeyHandler.create(new AdminAuthenticationProvider(config)));

                router = routerBuilder.createRouter();

                // Handle authentication errors on admin routes
                router.route().failureHandler(aContext -> {
                    final Throwable throwable = aContext.failure();

                    if (throwable instanceof HttpException) {
                        final HttpException error = (HttpException) throwable;
                        final int statusCode = error.getStatusCode();

                        final HttpServerResponse response = aContext.response().putHeader(HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_JSON.toString());
                        final JsonObject responseBody = new JsonObject();

                        response.setStatusCode(statusCode);

                        if (statusCode == HTTP.UNAUTHORIZED) {
                            responseBody.put(ResponseJsonKeys.ERROR, Error.INVALID_ADMIN_CREDENTIALS)
                                    .put(ResponseJsonKeys.MESSAGE, LOGGER.getMessage(MessageCodes.AUTH_016));
                        } else {
                            responseBody.put(ResponseJsonKeys.ERROR, StringUtils.format("HTTP {}", statusCode))
                                    .put(ResponseJsonKeys.MESSAGE, error.getMessage());
                        }

                        response.end(responseBody.encodePrettily());
                    } else {
                        aContext.next();
                    }
                });

                return Future.succeededFuture(router);
            }).compose(router -> {
                // Finally, spin up the HTTP server
                final HttpServerOptions serverOptions = new HttpServerOptions().setPort(port).setHost(host);
                final HttpServer server = getVertx().createHttpServer(serverOptions);

                return server.requestHandler(router).listen();
            });
        }).onSuccess(server -> {
            LOGGER.info(MessageCodes.AUTH_001, server.actualPort());
            aPromise.complete();
        }).onFailure(aPromise::fail);
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        final Future<Void> stopAll =
                CompositeFuture.all(myDatabaseService.unregister(), myAccessCookieService.unregister())
                        .compose(result -> myServer.close());

        stopAll.onSuccess(aPromise::complete).onFailure(aPromise::fail);
    }
}
