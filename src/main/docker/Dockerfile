
ARG ALPINE_TAG
ARG BUILDER_TAG

# We use a builder image as a path to a final image
FROM ksclarke/maven-graalvm-musl:${BUILDER_TAG} AS builder

ARG APP_VERSION

# Run the Maven build
WORKDIR /opt/hauth
COPY /maven/ .

RUN mkdir -p /opt/hauth/target \
    ; cp target/hauth-${APP_VERSION}.jar /opt/hauth/target/hauth-${APP_VERSION}.jar \
    ; cp src/main/docker/scripts/hauth /opt/hauth/target/hauth

# Set up the image to which to copy our build artifacts
FROM alpine:${ALPINE_TAG}

# Really only needed for a native compile, but it doesn't hurt
ENV javax.net.ssl.trustStore /etc/default/cacerts
ENV javax.net.ssl.trustAnchors /etc/default/cacerts

ARG NATIVE_COMPILE
ARG APP_VERSION

# This is either a placeholder or the uber-jar
COPY --from=builder /opt/hauth/target/hauth-${APP_VERSION}.jar /opt/hauth/hauth.jar
# This is either the native binary or a startup script for the uber-jar
COPY --from=builder /opt/hauth/target/hauth /usr/bin/hauth
# Copy in the SSL certs from the builder image
COPY --from=builder /etc/default/cacerts /etc/default/cacerts

RUN if [ "${NATIVE_COMPILE}" = true ] ; then \
      rm /opt/hauth/hauth.jar ; \
    else \
      apk add openjdk11 --quiet --no-cache ; \
    fi \
    && addgroup -S hauth \
    && adduser -S hauth -G hauth \
    && chown hauth:hauth /usr/bin/hauth \
    && chmod 750 /usr/bin/hauth

# Required so that the current date is always the same from the perspective of both the container and Maven
# TODO: the Sinai cookie should contain a more specific timestamp so that setting the system time is not strictly necessary
RUN apk add alpine-conf \
    && setup-timezone -z America/Los_Angeles \
    && apk del alpine-conf

# The user that runs the hauth application
USER hauth

# The port we expose the Hauth service on
EXPOSE 8888

CMD [ "sh", "-c", "exec /usr/bin/hauth" ]
