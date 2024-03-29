# Hauth

[![Maven Build](https://github.com/uclalibrary/hauth/workflows/Maven%20PR%20Build/badge.svg)](https://github.com/UCLALibrary/hauth/actions) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/f0e9885893cb47499767112f20050cdc)](https://app.codacy.com/gh/UCLALibrary/hauth?utm_source=github.com&utm_medium=referral&utm_content=UCLALibrary/hauth&utm_campaign=Badge_Grade_Settings)

A IIIF Auth implementation for limiting access by IP address or affiliation with the [Sinai Manuscripts Digital Library](https://sinaimanuscripts.library.ucla.edu).

## Prerequisites

To build Hauth, you need the following things installed and configured on your local machine:

* [Java](https://adoptopenjdk.net/) &gt;= JDK 11
* [Maven](https://maven.apache.org/download.cgi) &gt;= 3.8.x
* [Docker](https://www.docker.com/products/container-runtime) &gt;= 20.10.x

## Building

To build the project, run:

    mvn verify

This will run the build and all the unit and integration tests.

To run the integration tests with optional environment variables set, run:

    mvn -Poptional-env-vars verify

You can also, optionally, supply `-Ddocker.showLogs` if you want to see the containers' logs as the integration tests are running.

## Running in Developer Mode

To bring up the server locally for testing or development purposes, run:

    mvn initialize docker:build docker:run

This will spin up Hauth locally, along with the Redis, PostgreSQL, and Cantaloupe Docker containers. The containers will be randomly assigned unused ports, and random UUIDs will be created as dummy values for various secrets. The port numbers can be seen in the Maven output.

## Environment Variables

| Name | Default Value | Required |
| --- | --- | --- |
| ACCESS_COOKIE_DOMAIN | XXX | No
| ACCESS_COOKIE_WINDOW_CLOSE_DELAY | XXX | No
| ACCESS_TOKEN_EXPIRES_IN | XXX | No |
| API_KEY | XXX | Yes |
| API_SPEC | hauth.yaml | No |
| CAMPUS_NETWORK_SUBNETS | XXX | Yes |
| DB_CACHE_HOST | localhost | No |
| DB_CACHE_PORT | 6379 | No |
| DB_CONNECTION_POOL_MAX_SIZE | 5 | No |
| DB_HOST | localhost | No |
| DB_NAME | postgres | No |
| DB_PASSWORD | XXX | Yes |
| DB_PORT | 5432 | No |
| DB_RECONNECT_ATTEMPTS | 2 | No |
| DB_RECONNECT_INTERVAL | 1000 | No |
| DB_USER | postgres | No |
| HAUTH_VERSION | XXX | Yes |
| HTTP_HOST | 0.0.0.0 | No |
| HTTP_PORT | 8888 | No |
| SECRET_KEY_PASSWORD | XXX | Yes |
| SECRET_KEY_SALT | XXX | Yes |
| SINAI_COOKIE_SECRET_KEY_PASSWORD | XXX | Yes |
| SINAI_COOKIE_VALID_PREFIX | XXX | Yes |

## Sinai Cookie Format

The format of the Sinai cookie isn't officially documented anywhere, but its construction can be seen in the front-end
application's [source code](https://github.com/UCLALibrary/sinaimanuscripts/blob/v2.15.7/app/controllers/application_controller.rb#L98-L103").

The cookie contains an encypted value that is constructed using a prefix (`Authenticated ` + date) and a password (or
`CIPHER_KEY` as the front-end application calls it). For implementation details, see the above link, which is a
permalink to the version of the front-end code that was in production at the time of the last Hauth deployment. We'll
try to keep the above link up to date, but if we fall out of step the encrypted value _should_ be able to be found in
the front-end application controller's `create_encrypted_string` method.
