# Running the application outside the integration tests

Our example application demonstrates the use of Docker to run integration tests. If you'd like to explore the application running standalone without the tests, the following instructions demonstrate how to do this.

## Establishing a database

The `/standalone` directory contains this README file and a docker-compose file that will start up a MySQL database instance, run the same initialisation script as that used by the integration tests, and listen on the hosts port 3306.

You can therefore start up a database instance suitable for use with the application wtih the docker compose command run from this directory:

```bash
docker-compose up
```

Once running you can use the standard MySQL client to connect to the running database:
```bash
mysql --password --host=127.0.0.1 --protocol=TCP --port=3306 --user=it-user integration
```

You will be prompted for a password, so enter `it-password` (the password given to this user's account in the initialisation script). Once you're confident that the database is running ok, exit the MySQL client.

For example:

```console
mysql> show tables;
+-----------------------+
| Tables_in_integration |
+-----------------------+
| TEST_SAMPLE           |
+-----------------------+
1 row in set (0.01 sec)

mysql> desc TEST_SAMPLE;
+-------+---------------------+------+-----+---------+----------------+
| Field | Type                | Null | Key | Default | Extra          |
+-------+---------------------+------+-----+---------+----------------+
| ID    | bigint(20) unsigned | NO   | PRI | NULL    | auto_increment |
| NAME  | varchar(50)         | NO   |     | NULL    |                |
+-------+---------------------+------+-----+---------+----------------+
2 rows in set (0.00 sec)

mysql> exit
```

The application has been given default connection properties in its `src/main/resources/application.yml` file as follows:

```yaml
spring:
  application:
    name: sandpit
  datasource:
    # Note - we're using Hikari (Spring Boot 2's default DataSource) so jdbc-url not url
    jdbc-url: jdbc:mysql://localhost/integration
    username: it-user
    password: it-password
```

These are compatible with the database started via docker-compose. 

## Running and interating with the application

With the database running in this manner you can run the application from another terminal window with the command:

```bash
mvn spring-boot:run
```

To list the recorded names, you issue a `GET` to the `/sandpit` endpoint. The examples are given with curl, but for ad hoc testing you might like to use a GUI driven REST client such as [Postman](https://www.getpostman.com/) or the [REST Client](https://addons.mozilla.org/en-US/firefox/addon/restclient/) plugin for Firefox.

With curl:
```bash
curl -X GET -i http://localhost:8080/sandpit
```

The response initially will be:
```http
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 32
Date: Wed, 27 Mar 2019 12:19:45 GMT

Names are []
```

Note that no names are returned. Issuing a `POST` request to the `/sandpit/<name>` endpoint with a suitable substitute for `<name>` then creates a new entry in the list of names:

```bash
curl -X POST -i 'http://localhost:8080/sandpit/John%20Smith'
```

The response should include a 200 status code:

```http
HTTP/1.1 200 
Content-Length: 0
Date: Wed, 27 Mar 2019 12:25:31 GMT

```

Re-issuing the `GET` request to the `/sandpit` endpoint will now include `John Smith` in the list of names:

```http
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 32
Date: Wed, 27 Mar 2019 12:19:45 GMT

Names are [John Smith]
```

Login to the database again, and you should be able to list the name(s) posted to the endpoint.

```
mysql> select * from TEST_SAMPLE;
+----+--------------+
| ID | NAME         |
+----+--------------+
|  1 | John Smith   |
+----+--------------+
1 row in set (0.00 sec)
```

