package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final OwnerRepository ownerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var owner = ownerRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("Responsável com email: " + email + " não encontrado.")
        );

        return User
                .withUsername(owner.getEmail())
                .password(owner.getPasswordHash())
                .roles(owner.getRoleName())
                .disabled(!owner.isEnabled())
                .build();
    }
}
