package br.com.fiap.ClyvoCareAPI.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clyvocare-tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seed.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
public abstract class ApiIntegrationTest {
    @DynamicPropertySource
    static void signingKeys(DynamicPropertyRegistry registry) throws Exception {
        Path directory = Files.createTempDirectory("clyvocare-test-keys-");
        directory.toFile().deleteOnExit();
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        Path privateKey = directory.resolve("private_key.pem");
        Path publicKey = directory.resolve("public_key.pem");
        writePem(privateKey, "PRIVATE KEY", pair.getPrivate().getEncoded());
        writePem(publicKey, "PUBLIC KEY", pair.getPublic().getEncoded());
        registry.add("rsa.private-key", () -> privateKey.toUri().toString());
        registry.add("rsa.public-key", () -> publicKey.toUri().toString());
    }

    private static void writePem(Path path, String label, byte[] bytes) throws Exception {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes);
        Files.writeString(path, "-----BEGIN " + label + "-----\n" + encoded
                + "\n-----END " + label + "-----\n");
        path.toFile().deleteOnExit();
    }
}
