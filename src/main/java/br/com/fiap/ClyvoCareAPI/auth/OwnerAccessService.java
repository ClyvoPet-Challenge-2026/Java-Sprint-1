package br.com.fiap.ClyvoCareAPI.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OwnerAccessService {
    public boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long currentOwnerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to access this resource");
        }
        return Long.valueOf(jwt.getClaimAsString("ownerId"));
    }

    public Long restrictOwnerId(Long ownerId) {
        Long currentId = currentOwnerId();
        if (isAdmin()) {
            return ownerId;
        }
        if (ownerId != null && !currentId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource belongs to another owner");
        }
        return currentId;
    }
}
