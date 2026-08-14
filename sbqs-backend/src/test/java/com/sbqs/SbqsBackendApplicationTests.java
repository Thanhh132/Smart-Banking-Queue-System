package com.sbqs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sbqs.repository.UserRepository;
import com.sbqs.config.PreparedServiceCatalogInitializer;

@SpringBootTest(properties = {
		"spring.main.lazy-initialization=true",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
		"camunda.bpm.enabled=false",
		"sbqs.ticket.outbox.scheduling-enabled=false",
		"spring.cache.type=simple",
		"sbqs.kafka.enabled=false"
})
class SbqsBackendApplicationTests {
	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private PreparedServiceCatalogInitializer preparedServiceCatalogInitializer;

	@Test
	void contextLoads() {
	}

}
