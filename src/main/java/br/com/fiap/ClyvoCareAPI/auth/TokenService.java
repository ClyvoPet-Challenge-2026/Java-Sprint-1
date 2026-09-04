package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder encoder;
    private final OwnerRepository ownerRepository;

    public String generateToken(String email) {
        var owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Responsável com email: " + email + " não encontrado."));

        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("clyvocare-api")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .subject(owner.getEmail())
                .claim("role", owner.getRoleName())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
