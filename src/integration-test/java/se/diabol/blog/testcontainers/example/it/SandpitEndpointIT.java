package se.diabol.blog.testcontainers.example.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import se.diabol.blog.testcontainers.example.SandpitApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(
	// Explicit class is necessary because we're in a peer package of example
	// Explicit configuration is necessary if we want to override the url used in
	// the repository
	classes = { 
			SandpitApplication.class,
			SandpitEndpointIT.SandpitEndpointITConfig.class 
	}, 
	webEnvironment = WebEnvironment.RANDOM_PORT
)
public class SandpitEndpointIT {
	private static final Logger LOG = LoggerFactory.getLogger(SandpitEndpointIT.class);
	private static final String POST_URL_TEMPLATE = "http://localhost:%d/sandpit/%s";
	private static final String GET_URL_TEMPLATE = "http://localhost:%d/sandpit/";

	private static final String DOCKER_FILENAME = "docker-compose.yml";
	private static final String MYSQL_SERVICE_NAME = "mysql-db";
	private static final int MYSQL_PROTOCOL_PORT = 3306;

	private static final String MYSQL_USERNAME = "it-user";
	private static final String MYSQL_PASSWORD = "it-password";	

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private static final File dockerFile = new File(ClassLoader.getSystemResource(DOCKER_FILENAME).getFile());

	@ClassRule
	public static DockerComposeContainer<?> environment = new DockerComposeContainer<>(dockerFile)
				.withExposedService(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT)
				.waitingFor(MYSQL_SERVICE_NAME, Wait.forListeningPort());
	
	@TestConfiguration
	public static class SandpitEndpointITConfig {
		@Bean(destroyMethod = "close")
		@Primary // @Primary ensures this bean gets priority in wiring over the existing one
		public DataSource testSqlDataSource() throws SQLException {
			LOG.info("Loading up IntegrationTest DataSource");

			// Acquire the host & port with which we can connect to the MySQL instance
			final var mySqlHostname = environment.getServiceHost(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);
			final var hostMySqlProtocolPort = environment.getServicePort(MYSQL_SERVICE_NAME, MYSQL_PROTOCOL_PORT);
			final var url = String.format("jdbc:mysql://%s:%s/integration", mySqlHostname, hostMySqlProtocolPort);

			return DataSourceBuilder.create()
					.url(url)
					.username(MYSQL_USERNAME)
					.password(MYSQL_PASSWORD)
					.build();
		}
	}

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
}