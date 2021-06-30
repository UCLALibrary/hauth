
ARG ALPINE_TAG
ARG BUILDER_TAG

# We use a builder image as a path to a final image
FROM ksclarke/maven-graalvm-musl:${BUILDER_TAG} AS builder

ARG NATIVE_COMPILE
ARG APP_VERSION
ARG USE_UPX

# Run the Maven build
WORKDIR /opt/bouncer 
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
      #  upx --best -q /opt/bouncer/target/bouncer ; \
      #fi \
      # We need a placeholder jar file to copy (and then later delete)
#      && touch /opt/bouncer/target/bouncer-${APP_VERSION}.jar ; \
    # If we're using an uber-jar copy over the startup script
#    else \
      mkdir -p /opt/bouncer/target ; \
      cp target/bouncer-${APP_VERSION}.jar /opt/bouncer/target/bouncer-${APP_VERSION}.jar; \
      cp src/main/docker/scripts/bouncer /opt/bouncer/target/bouncer ; \
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
COPY --from=builder /opt/bouncer/target/bouncer-${APP_VERSION}.jar /opt/bouncer/bouncer.jar
# This is either the native binary or a startup script for the uber-jar
COPY --from=builder /opt/bouncer/target/bouncer /usr/bin/bouncer
# Copy in the SSL certs from the builder image
COPY --from=builder /etc/default/cacerts /etc/default/cacerts

RUN if [ "${NATIVE_COMPILE}" = true ] ; then \
      rm /opt/bouncer/bouncer.jar ; \
    else \
      apk add openjdk11 --quiet --no-cache ; \
    fi \
    && addgroup -S bouncer \
    && adduser -S bouncer -G bouncer \
    && chown bouncer:bouncer /usr/bin/bouncer \
    && chmod 750 /usr/bin/bouncer

# The user that runs the bouncer application
USER bouncer

# The port we expose the Bouncer service on
EXPOSE 8888

CMD [ "sh", "-c", "exec /usr/bin/bouncer" ]
