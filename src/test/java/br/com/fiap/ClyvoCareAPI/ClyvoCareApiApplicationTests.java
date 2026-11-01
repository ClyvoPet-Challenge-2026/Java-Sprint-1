package br.com.fiap.ClyvoCareAPI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class ClyvoCareApiApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JwtDecoder decoder;

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

    @Test
    void contextLoadsWithIsolatedDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

    @Test
    void signedTokenCanBeVerifiedWithExternalPublicKey() {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("clyvocare-api")
                .subject("test-owner@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("role", "OWNER")
                .build();

        var encoded = encoder.encode(JwtEncoderParameters.from(claims));
        var decoded = decoder.decode(encoded.getTokenValue());

        assertEquals("RS256", decoded.getHeaders().get("alg"));
        assertEquals("test-owner@example.com", decoded.getSubject());
        assertEquals("OWNER", decoded.getClaimAsString("role"));
    }

}
