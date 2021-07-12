
ARG ALPINE_TAG
ARG BUILDER_TAG

# We use a builder image as a path to a final image
FROM ksclarke/maven-graalvm-musl:${BUILDER_TAG} AS builder

ARG NATIVE_COMPILE
ARG APP_VERSION
ARG USE_UPX

# Run the Maven build
WORKDIR /opt/hauth
COPY /maven/ .

# If we're doing a native-build, compile that; else, the uber jar has already been copied
RUN if [ true = true ] ; then \
#      mvn -B -ntp -Pnative-compile org.apache.maven.plugins:maven-jar-plugin:jar \
#      org.graalvm.nativeimage:native-image-maven-plugin:native-image \
      # 2>&1 > /tmp/maven.log \
      # Check output to make sure our native build succeeded
      #&& if [ $(cat /tmp/maven.log |grep -c ERROR) -gt 0 ] ; then \
      #  cat /tmp/maven.log ; \
      #  exit 1 ; \
      #fi \
      # Determine whether we're going to compress the native build artifact
      #if [ "${USE_UPX}" = true ] ; then \
      #  upx --best -q /opt/hauth/target/hauth ; \
      #fi \
      # We need a placeholder jar file to copy (and then later delete)
#      && touch /opt/hauth/target/hauth-${APP_VERSION}.jar ; \
    # If we're using an uber-jar copy over the startup script
#    else \
      mkdir -p /opt/hauth/target ; \
      cp target/hauth-${APP_VERSION}.jar /opt/hauth/target/hauth-${APP_VERSION}.jar; \
      cp src/main/docker/scripts/hauth /opt/hauth/target/hauth ; \
    fi

#ENTRYPOINT ["tail", "-f", "/dev/null"]

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

# The user that runs the hauth application
USER hauth

# The port we expose the Hauth service on
EXPOSE 8888

CMD [ "sh", "-c", "exec /usr/bin/hauth" ]
