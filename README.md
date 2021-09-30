# Hauth

A IIIF Auth implementation for limiting access by IP address.

Still in development.

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

## Running in Developer Mode

To bring up the server locally for testing or development purposes, run:

    mvn -Plive integration-test

This will spin up Hauth locally, along with the Redis, PostgreSQL, and Cantaloupe Docker containers. You can also, optionally, supply `-Ddocker.showLogs` if you want to see the containers' logs.

## Environmental Properties

These are a work in progress. None of these need to be supplied when running in developer mode. Randomized ports and passwords are created on the fly. The port numbers can be seen in the Maven output.

| ENV Property | Default Value | Required |
| --- | --- | --- |
| HTTP_PORT | 8888 | No |
| HTTP_HOST | 0.0.0.0 | No |
| ACCESS_TOKEN_EXPIRES_IN | 3600 | No |
| API_SPEC | hauth.yaml | No |
| DB_HOST | localhost | No |
| DB_PORT | 5432 | No |
| DB_NAME | postgres | No |
| DB_USER | postgres | No |
| DB_PASSWORD | XXX | Yes |
| DB_CONNECTION_POOL_MAX_SIZE | 5 | No |
| DB_RECONNECT_ATTEMPTS | 2 | No |
| DB_RECONNECT_INTERVAL | 1000 | No |
| DB_CACHE_HOST | localhost | No |
| DB_CACHE_PORT | 6379 | No |
| IIIF_SERVER_HOST | XXX | Yes |
| IIIF_SERVER_PORT | 8182 | No |
| HAUTH_VERSION | XXX | Yes |
| SECRET_KEY_GENERATION_PASSWORD | XXX | Yes |
| SECRET_KEY_GENERATION_SALT | XXX | Yes |
| --- | --- | --- |
