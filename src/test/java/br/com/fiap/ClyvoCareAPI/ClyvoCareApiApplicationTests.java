package br.com.fiap.ClyvoCareAPI;

import br.com.fiap.ClyvoCareAPI.support.TestRsaKeys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seed.enabled=false"
})
class ClyvoCareApiApplicationTests extends TestRsaKeys {

	@Test
	void contextLoads() {
	}

}
