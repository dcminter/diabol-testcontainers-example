# Integration testing against real services with JUnit and Testcontainers

Often applications being integration tested must interact with other
software but doing so can be tricky. A classic scenario is testing an
application’s interactions with its database.

Sharing a database instance will have side effects that complicate or
break tests. Mocked databases are not representative of the production
environment. The solution shown here uses containerisation to provide a
private database instance.

This article demonstrates the use of the [Docker Compose
Module](https://www.testcontainers.org/modules/docker_compose/) in
[Testcontainers](https://www.testcontainers.org/) with
[JUnit](https://junit.org/) to integration test a service backed by a
[MySQL](https://www.mysql.com/) database instance.

## Common approaches

Most developers see the merits of integration testing, but there are
practical considerations when creating services like databases to test
against. This is especially true for components that are not themselves
implemented in Java and therefore cannot be launched directly as Java
libraries.

Some approaches for integration testing against databases, along with
their pros and cons, are:

  * Use a private schema on a shared database
    *   *Pros* No collisions with other users running tests
    *   *Cons* Test data must be set up and torn down for the test (or care must be taken not to modify it). Parallel tests will have data collisions if care is not taken. Tests will not be portable outside network environment. The test requires CREATE privileges if a clean database is to be established for each test.
  * Use a shared schema on a shared database
    *   *Pros* Requires no special privileges
    *   *Cons* Requires careful management of test data to avoid collisions with other users, inevitable side-effects (e.g. on system statistics) may affect other users, requires coordination with other users of any changes to the schema, not portable outside network environment, can leave stale data accumulating in the database

  * Use an in-memory database
    *   *Pros* No collisions with other users, portable between environments, requires no special privileges
    *   *Cons* Unlikely to be what’s used for a production database and will therefore have significantly different behaviour - for example it may have different fundamental data types and SQL syntax

## A better approach

Instead of running a shared or mocked database, run a real instance of
the database within a Docker container.

This can be created automatically before the test suite is run and
removed afterwards. The approach has the following merits:

  * No collisions or side effects from other users running tests
  * Requires no special privileges beyond the ability to run Docker
  * No collisions with other users
  * Portable between environments
  * The application under test can run against the same database implementation that will be encountered in production

The example makes use of the Testcontainers libraries to automatically
start and stop a Docker Compose environment before and after the suite
of tests is run.

For many integration testing scenarios this approach will be ideal!

### Prerequisites

The example presumes that you are running the following versions

  * Java 11 or higher
  * Docker 1.10 or higher

Command line examples are from a Linux environment but should be similar
on OSX or Windows

Testcontainers can be used in your own code with Java 8 or higher and
the example here can be run under Java 10 with very minor changes (see
the section “Java 11” toward the end of this article).

## Writing the docker-compose file

Docker is a tool for containerising applications, and Docker Compose is
a tool for coordinating a suite of containerised applications.

In a complex test one might wish to initialise different software
(e.g. database, message queue, storage API) for a full integration test.
Docker compose is the tool that Testcontainers uses internally and
therefore must be provided with a suitable [docker compose
configuration file](https://docs.docker.com/compose/compose-file/).

Docker compose files are written in [YAML](https://yaml.org/) and this
test only needs a simple file to set up the MySQL database.

Here is the complete file:

```yaml
version: '2'
services:
 mysql-db:
    image: mysql:8.0
 ports:
    - "3306"
 volumes:
    - ./init.sql:/docker-entrypoint-initdb.d/init.sql
 environment:
    MYSQL_ROOT_PASSWORD: it-root-pwd
    MYSQL_DATABASE: integration
    MYSQL_USER: it-user
    MYSQL_PASSWORD: it-password
```

This configuration file:

  * Creates a MySQL 8.0 image
  * Makes port 3306 on the container available to the host on an ephemeral port
  * Initialises the database with a SQL script
  * Defines some user credentials
  * Creates a database schema called ‘integration’
  * Requires version 2 of Testcontainers

Also provided is a script named `init.sql` that is run as the database’s
standard initialisation script within the container.

The script is as follows:

```sql
USE integration;

CREATE TABLE IF NOT EXISTS TEST_SAMPLE(
   ID SERIAL PRIMARY KEY,
   NAME VARCHAR(50) NOT NULL
);
```

This creates a table for the application in the `integration` scheme.

To test this configuration before trying to incorporate it into the
integration test, put the two files alone in a directory and run
docker-compose from that directory thus:

```bash
docker-compose up
```

The output will look something like the following:

```bash
/usr/lib/python3/dist-packages/requests/__init__.py:80:
   RequestsDependencyWarning: urllib3 (1.24.1) or chardet (3.0.4) doesn't match a supported version!
 RequestsDependencyWarning)

Creating network "resources_default" with the default driver
Creating resources_mysql-db_1 ... done
Attaching to resources_mysql-db_1
mysql-db_1 | Initializing database
mysql-db_1 | 2019-03-18T10:45:04.451446Z 0 [Warning] [MY-011070] [Server] 'Disabling symbolic links using --skip-symbolic-links (or equivalent) is the default. Consider not using this option as it' is deprecated and will be removed in a future release.

*... etc ...*

mysql-db_1 | 2019-03-18T10:45:18.711609Z 0 [Warning] [MY-010068] [Server] CA certificate ca.pem is self signed.
mysql-db_1 | 2019-03-18T10:45:18.715066Z 0 [Warning] [MY-011810] [Server] Insecure configuration for --pid-file: Location
'/var/run/mysqld' in the path is accessible to all OS users. Consider choosing a different directory.
mysql-db_1 | 2019-03-18T10:45:18.727401Z 0 [System] [MY-010931] [Server] /usr/sbin/mysqld: ready for connections. Version: '8.0.15' socket: '/var/run/mysqld/mysqld.sock' port: 3306 MySQL Community Server - GPL.
mysql-db_1 | 2019-03-18T10:45:18.829625Z 0 [System] [MY-011323] [Server] X Plugin ready for connections. Socket: '/var/run/mysqld/mysqlx.sock' bind-address: '::' port: 33060
```

Although this output lists the ports that the database has opened, this 
is from the perspective of the container. From the host environment you
can view the status of the running service(s) using the process listing
command from another terminal session:

```bash
docker ps
```

You should see something similar to the following listing the container
that you started:

```
CONTAINER ID IMAGE COMMAND CREATED STATUS PORTS NAMES
3b99911e5e4a mysql:8.0 "docker-entrypoint.s…" 5 minutes ago Up 5 minutes 33060/tcp, 0.0.0.0:32771->3306/tcp resources_mysql-db_1
```

To shutdown the docker container hit Ctrl-C in the terminal session
currently running docker-compose or run docker-compose down from another
terminal session.

## Port forwarding

Inside the docker container the default MySQL protocol port of 3306 is
used.

On the host it’s undesirable to have any fixed dependency on the port
number. Once the integration test is running on a build server, there
may be other tests or other instances of the same test running
simultaneously.. If the port number was predetermined then the tests
would fail if the desired port was already in use.

Instead you can instruct Docker to assign an ephemeral port number (an
unused port above 1024) on the host computer and bridge that to the
MySQL protocol port on the container. The section on the integration
test shows that the test can then obtain the assigned port number at
runtime.

The following snippet from a configuration file shows the preferred
configuration of a dynamic host port number versus the (commented out)
predetermined port number:

```
ports:
 # A fixed mapping of the container's port to the host risks port collisions; do not use:
 # - "13306:3306"
 # Dynamic mapping of the container's port to the host will succeed if there are unused ephemeral port numbers:
 - "3306"
```

In the output from docker ps in the previous section you can see a list
of the ports that the service running in the container has open. Note
the part reading as follows:

```
0.0.0.0:32771->3306/tcp
```

This indicates that port **3306** running on the container has been bridged
to the host on ephemeral port **32771**.

## Importing the library

The Testcontainers library is available from the Maven Central
repository, so taking advantage of its features should just be a matter
of including the dependency in your build configuration.

For example, for Maven with the current version 1.10.6 of the library:

```xml
<dependency>
   <groupId>org.testcontainers</groupId>
   <artifactId>testcontainers</artifactId>
   <version>1.10.6</version>
   <scope>test</scope>
</dependency>
```

## The example implementation under test

In the example code the implementation under test runs within Spring
Boot. It exposes an endpoint by which a user can add a name to a list or
retrieve the list. The list is written to and read from the database
instance via Spring’s `JdbcTemplate`.

```java
@Override
public void addName(final String name) {
   LOG.info("Adding name %s",name);
   jdbcTemplate.update(WRITE_NAME_SQL, name);
}

@Override
public List<String> listNames() {
   final var names = jdbcTemplate.query(READ_NAME_SQL, (rs, row) -> rs.getString(NAME_FIELD));
   LOG.info("Names pulled from database are: {}", names);
   return names;
}
```

## The integration test

The integration test will run the implementation and then run a test of
the following form:

  * Given that the endpoint does not return a name
  * When I post a new name to the endpoint
  * Then I expect to see that the endpoint does now return the name

The test class is `SandpitEndpointIT` in the
`se.diabol.blog.testcontainers.example.it` package and is a normal JUnit
test class. By default test classes ending in `IT` will run in the
integration-test phase of a Maven build. The Maven configuration has
been added (not shown here) to move the integration test source code
into `src/integration-test/java` and its resource files into
`src/integration-test/resources` so that they can more easily be
distinguished from the unit tests and actual implementation code.

The integration test is wired up as a `@SpringBootTest` to run the Spring
Boot application and uses a `ClassRule` from Testcontainers to create the
docker environment that the test runs against.

```java
private static final String DOCKER_FILENAME = "docker-compose.yml";
private static final String MYSQL_SERVICE_NAME = "mysql-db";
private static final int MYSQL_PROTOCOL_PORT = 3306;
private static final File dockerFile = new File(ClassLoader.getSystemResource(DOCKER_FILENAME).getFile());

@ClassRule
public static DockerComposeContainer<?> environment = new DockerComposeContainer<>(dockerFile)
   .withExposedService(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT)
   .waitingFor(MYSQL_SERVICE_NAME, Wait.forListeningPort());
```

The class rule object provides methods that can retrieve the host and
port names needed when configuring the Spring Boot application with its
database connection.

```java
final var mySqlHostname = environment.getServiceHost(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);
final var hostMySqlProtocolPort = environment.getServicePort(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);
```

A configuration object is also shown providing a `DataSource` to connect
to the test database. The `@Primary` annotation on the bean ensures that
this version takes precedence over the implementation’s usual datasource
bean.

This is where you would make use of the calls into the class rule object
and use the resulting information to set the port and host details for
the connection.

```java
@TestConfiguration
public static class SandpitEndpointITConfig {

 @Bean(destroyMethod = "close")
 @Primary // @Primary ensures this bean gets priority in wiring over the existing one
 public DataSource testSqlDataSource() throws SQLException {
    LOG.info("Loading up IntegrationTest DataSource");

    // Acquire the host & port for connecting to the MySQL instance
    final var mySqlHostname = environment.getServiceHost(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);
    final var hostMySqlProtocolPort = environment.getServicePort(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);

    final String url = String.format("jdbc:mysql://%s:%s/integration", mySqlHostname, hostMySqlProtocolPort);
    return DataSourceBuilder.create()
       .url(url)
       .username(MYSQL_USERNAME)
       .password(MYSQL_PASSWORD)
       .build();
 }
}
```

With this wiring in place the happy path test make straight forward use
of a rest template to call into the endpoint and make assertions about
the results for the test.

```java
@Test
public void simpleAliveHappyPath() {
   final String name = "johnsmith";
   final String get = String.format(GET_URL_TEMPLATE, port);
   final String post = String.format(POST_URL_TEMPLATE, port, name);

   // Given that the endpoint does not contain a name
   assertThat(this.restTemplate.getForObject(get, String.class)).doesNotContain(name);

   // When we post a name to the endpoint
   assertThat(this.restTemplate.postForLocation(post, new Object[] {}));

   // Then we expect the endpoint to return the name
   assertThat(this.restTemplate.getForObject(get, String.class)).contains(name);
}
```

### Possible drawbacks

For a lot of integration test scenarios this approach is ideal - but
there are always trade-offs. Here are some for you to consider.

**Software Prerequisite**

The use of Testcontainers requires Docker to be present on machines
running the test suite. On developer workstations this is generally not
an issue, but it can complicate the configuration of integration
servers.

**Using SpringBootTest could have side-effects**

The specific example here wires up a `@SpringBootTest` to start and stop
the main Spring Boot application and to override the datasource
configuration. An ideal test would make no changes beyond configuration
properties and parameterisation. This is still possible with
Testcontainers and Docker Compose tooling but is more complex as it
requires:

  * Generation of the appropriate configuration artifacts (e.g. property files)
  * Launch of the application as a self-container process

Whereas the approach used in our example offers much of the benefit with
more portability and less risk of creating brittle file-format
dependencies.

**Suitable Docker images might not exist**

For most open source software (not just databases) there will be a
pre-built docker image available. If one is not available then you may
have to build it yourself in order to use this strategy for your test.

For commercial software you may find that there is no docker image and
that the license prevents from running your tests in this manner.

For commercial APIs (e.g. Amazon S3) the software may be unavailable
other than by the API so there is nothing to convert into an image.
Occasionally mock versions of APIs may be available (e.g. [s3mock](https://github.com/findify/s3mock)) but
these offer only limited assurance that an implementation will work
against the real API.

## Notes on the implementation

In this last section a few issues are noted that arise from the
implementation choices.

### Hikari properties in Spring Boot 2

Spring Boot 2 made the default `DataSource` connection pool dependency
[HikariCP](http://brettwooldridge.github.io/Hikari)

Hikari’s configuration properties are slightly different from the
default Spring DataSource properties as they require `jdbc-url` instead of
just a url - which may cause warnings in your tooling (e.g. Spring Tool
Suite). A configuration file
`src/main/java/META-INF/additional-spring-configuration-metadata.json` can
be added to document the new property for tooling such as Spring Tool
Suite.

```json
{ 
   "properties": [{
      "name": "spring.datasource.jdbc-url",
      "type": "java.lang.String",
      "description": "HikariCP requires 'spring.datasource.jdbc-url' instead of 'spring.datasource.url'"
   }]
}
```

You can alternatively use the technique [documented in the Spring Boot
reference
manual](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-a-datasource)
to copy the default properties into the Hikari data source.

### Java 11

The example project is compiled with Java 11 - the current long-term
supported release of Java - and this is explicitly declared in the
pom.xml declaration thus:

```xml
<properties>
   <java.version>11</java.version>
   ...
</properties>
```

The code should still run with Java 10 (amend the `java.version` property
to 10 instead of 11) without change. The code makes some use of local
variable inference (declaring variables as type var) which was
introduced in Java 10, so to use earlier releases of Java you will need
to make changes.

### SSL Exceptions under Java 11

Running under Java 11 you will likely see exceptions logged of the
following kind:

```
** BEGIN NESTED EXCEPTION **
javax.net.ssl.SSLException MESSAGE: closing inbound before receiving peer's close_notify
```

These are due to [a known issue with the MySQL JDBC
driver](https://bugs.mysql.com/bug.php?id=93590) and can be safely
ignored - the tests should pass cleanly regardless.

**Support for version 3 of the Docker Compose file format**

At the time of writing Testcontainers does not directly support version
3 of the Docker Compose file format. This is an annoyance if you already
have a version 3 compose file that you would like to re-use or if you
want to take advantage of capabilities such as restarting swarms that
are not supported in the lower versions.

See [this issue on
GitHub](https://github.com/testcontainers/testcontainers-java/issues/531)
for the latest status of support for version 3 files and the available
workarounds if you need to use them.

## Additional resources

Although the example is provided to demonstrate an approach to integration testing, 
you can run it as a standalone application if you wish. Instructions are provided in 
the [standalone/README.md](standalone/README.md) file.

## Summary

In this article I discussed some of the issues with integration testing
an application against dependencies such as databases (for example
MySQL). I then presented a solution to these problems using
Testcontainers and Docker Compose to manage a containerised MySQL
database instance.

I’m here for any questions.

David Minter,
[Diabol AB](https://diabol.se/)
