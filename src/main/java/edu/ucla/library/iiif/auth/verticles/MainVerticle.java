
package edu.ucla.library.iiif.auth.verticles;

import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.AdminAuthenticationProvider;
import edu.ucla.library.iiif.auth.Config;
import edu.ucla.library.iiif.auth.MessageCodes;
import edu.ucla.library.iiif.auth.Op;
import edu.ucla.library.iiif.auth.handlers.AccessCookieHandler;
import edu.ucla.library.iiif.auth.handlers.AccessModeHandler;
import edu.ucla.library.iiif.auth.handlers.MissingAccessCookieErrorHandler;
import edu.ucla.library.iiif.auth.handlers.AccessTokenHandler;
import edu.ucla.library.iiif.auth.handlers.AdminAuthenticationErrorHandler;
import edu.ucla.library.iiif.auth.handlers.HtmlRenderingErrorHandler;
import edu.ucla.library.iiif.auth.handlers.ItemsHandler;
import edu.ucla.library.iiif.auth.handlers.SinaiAccessTokenHandler;
import edu.ucla.library.iiif.auth.handlers.StatusHandler;
import edu.ucla.library.iiif.auth.services.AccessCookieService;
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
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * Main verticle that starts the application.
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class MainVerticle extends AbstractVerticle {

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
     * The event bus services.
     */
    private Set<MessageConsumer<?>> myEventBusServices;

    /**
     * The HTTP server for the Hauth service.
     */
    private HttpServer myServer;

    @Override
    public void start(final Promise<Void> aPromise) {
        ConfigRetriever.create(vertx).getConfig().compose(config -> {
            return createEventBusServices(config).compose(services -> {
                // Save a reference to the services so we can unregister them later
                myEventBusServices = services;

                return createRouter(config);
            }).compose(router -> startHttpServer(config, router));
        }).onSuccess(server -> {
            // Save a reference to the HTTP server so we can close it later
            myServer = server;

            LOGGER.info(MessageCodes.AUTH_001, server.actualPort());
            aPromise.complete();
        }).onFailure(aPromise::fail);
    }

    /**
     * Creates event bus services.
     *
     * @param aConfig A configuration
     * @return A Future that resolves to the list of event bus services
     */
    public Future<Set<MessageConsumer<?>>> createEventBusServices(final JsonObject aConfig) {
        final MessageConsumer<JsonObject> accessCookieService;
        final MessageConsumer<JsonObject> databaseService;
        final ServiceBinder serviceBinder = new ServiceBinder(vertx);

        try {
            accessCookieService = serviceBinder.setAddress(AccessCookieService.ADDRESS)
                    .register(AccessCookieService.class, AccessCookieService.create(aConfig));
        } catch (final GeneralSecurityException details) {
            return Future.failedFuture(details);
        }

        databaseService = serviceBinder.setAddress(DatabaseService.ADDRESS) //
                .register(DatabaseService.class, DatabaseService.create(vertx, aConfig));

        return Future.succeededFuture(Set.of(accessCookieService, databaseService));
    }

    /**
     * Creates the HTTP request router.
     *
     * @param aConfig A configuration
     * @return A Future that resolves to the HTTP request router
     */
    public Future<Router> createRouter(final JsonObject aConfig) {
        final String apiSpec = aConfig.getString(Config.API_SPEC, DEFAULT_API_SPEC);

        // Load the OpenAPI specification
        return RouterBuilder.create(vertx, apiSpec).compose(builder -> {
            final Router router;

            // In the case of the access token service, in order to construct a response that complies with
            // https://iiif.io/api/auth/1.0/#access-token-error-conditions, we need to take control back from the
            // ValidationHandler that gets invoked when an incoming request violates the OpenAPI contract (e.g., missing
            // access cookie).
            final ErrorHandler missingAccessCookieErrorHandler = new MissingAccessCookieErrorHandler(vertx);

            // Associate handlers with operation IDs from the OpenAPI spec
            builder.operation(Op.GET_STATUS).handler(new StatusHandler(vertx));
            builder.operation(Op.GET_ACCESS_MODE).handler(new AccessModeHandler(vertx));
            builder.operation(Op.GET_COOKIE).handler(new AccessCookieHandler(vertx, aConfig));
            builder.operation(Op.GET_TOKEN).handler(new AccessTokenHandler(vertx, aConfig))
                    .failureHandler(missingAccessCookieErrorHandler);
            builder.operation(Op.GET_TOKEN_SINAI).handler(new SinaiAccessTokenHandler(vertx, aConfig))
                    .failureHandler(missingAccessCookieErrorHandler);
            builder.operation(Op.POST_ITEMS).handler(new ItemsHandler(vertx));

            // Add API key authentication for routes that should use the "Admin" security scheme
            builder.securityHandler("Admin")
                    .bindBlocking(unused -> APIKeyHandler.create(new AdminAuthenticationProvider(aConfig)));

            router = builder.createRouter();

            // Register error handlers that are generic enough to apply to more than one operation.
            //
            // Note that the operation-specific handlers above are responsible for handling any ServiceExceptions
            // that they may encounter, since the proper handling of those particular errors is likely to be
            // context-dependent.
            router.route() //
                    .failureHandler(new AdminAuthenticationErrorHandler()) //
                    .failureHandler(new HtmlRenderingErrorHandler());

            // Enable deployment behind a reverse proxy
            router.allowForward(AllowForwardHeaders.X_FORWARD);

            return Future.succeededFuture(router);
        });
    }

    /**
     * Starts the HTTP server.
     *
     * @param aConfig A configuration
     * @param aRouter An HTTP request router
     * @return A Future that resolves to the started HTTP server
     */
    public Future<HttpServer> startHttpServer(final JsonObject aConfig, final Router aRouter) {
        final String host = aConfig.getString(Config.HTTP_HOST, DEFAULT_HOST);
        final int port = aConfig.getInteger(Config.HTTP_PORT, DEFAULT_PORT);

        return vertx.createHttpServer(new HttpServerOptions().setPort(port).setHost(host)) //
                .requestHandler(aRouter) //
                .listen();
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        final Stream<Future<?>> stopEventBusServices =
                myEventBusServices.parallelStream().map(MessageConsumer::unregister);

        myServer.close().compose(unused -> {
            return CompositeFuture.all(stopEventBusServices.collect(Collectors.toList()));
        }).onSuccess(unused -> aPromise.complete()).onFailure(aPromise::fail);
    }
}
