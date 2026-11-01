package br.com.fiap.ClyvoCareAPI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.fiap.ClyvoCareAPI.support.ApiIntegrationTest;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClyvoCareApiApplicationTests extends ApiIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private OwnerRepository owners;

    @Test
    void contextLoadsWithIsolatedDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

    @Test
    void signedTokenCanBeVerifiedWithExternalPublicKey() {
        var owner = owners.saveAndFlush(Owner.builder().email("test-owner@example.com")
                .roleName("OWNER").enabled(true).build());
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("clyvocare-api")
                .subject("test-owner@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("role", "OWNER")
                .claim("ownerId", owner.getId())
                .build();

        var encoded = encoder.encode(JwtEncoderParameters.from(claims));
        var decoded = decoder.decode(encoded.getTokenValue());

        assertEquals("RS256", decoded.getHeaders().get("alg"));
        assertEquals("test-owner@example.com", decoded.getSubject());
        assertEquals("OWNER", decoded.getClaimAsString("role"));
    }

}
