package se.diabol.blog.testcontainers.example.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DefaultSandpitService implements SandpitService {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultSandpitService.class);

	private final JdbcTemplate jdbcTemplate;

	private static final String NAME_FIELD = "NAME";
	private static final String WRITE_NAME_SQL = "INSERT INTO TEST_SAMPLE(NAME) VALUES (?)";
	private static final String READ_NAME_SQL = "SELECT NAME FROM TEST_SAMPLE ORDER BY ID";

	@Autowired
	public DefaultSandpitService(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

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
}