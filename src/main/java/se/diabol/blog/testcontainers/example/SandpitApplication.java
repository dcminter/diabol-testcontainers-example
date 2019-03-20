package se.diabol.blog.testcontainers.example;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

// Remember, implicitly does: @Configuration @EnableAutoConfiguration @ComponentScan
@SpringBootApplication
public class SandpitApplication {	
	
	public static void main(String[] args) {
		SpringApplication.run(SandpitApplication.class, args);
	}

	@Bean(destroyMethod = "close")
    @ConfigurationProperties(prefix = "spring.datasource")
	public DataSource mySqlDataSource() throws SQLException {
		// Driven from the application.yml file
		return DataSourceBuilder.create().build();
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate(final DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}