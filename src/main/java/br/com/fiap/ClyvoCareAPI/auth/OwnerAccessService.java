package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OwnerAccessService {
    private final OwnerRepository ownerRepository;

    public boolean isAdmin() {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long currentOwnerId() {
        Authentication authentication = authentication();
        if (!(authentication.getPrincipal() instanceof Jwt jwt) || jwt.getClaimAsString("ownerId") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again to refresh your session");
        }
        return ownerRepository.findByEmail(authentication.getName())
                .filter(owner -> owner.isEnabled() && owner.getId().toString().equals(jwt.getClaimAsString("ownerId")))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated owner not available; sign in again"))
                .getId();
    }

    public Long restrictOwnerId(Long ownerId) {
        if (isAdmin()) {
            return ownerId;
        }
        Long currentOwnerId = currentOwnerId();
        if (ownerId != null && !currentOwnerId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource belongs to another owner");
        }
        return currentOwnerId;
    }

    public void validateOwner(Long ownerId) {
        restrictOwnerId(ownerId);
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(authority ->
                authority.getAuthority().equals("ROLE_OWNER") || authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner or administrator required");
        }
        return authentication;
    }
}
