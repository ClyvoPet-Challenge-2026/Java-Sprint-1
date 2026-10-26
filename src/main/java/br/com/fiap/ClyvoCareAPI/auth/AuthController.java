package br.com.fiap.ClyvoCareAPI.auth;

import br.com.fiap.ClyvoCareAPI.dto.OwnerResponse;
import br.com.fiap.ClyvoCareAPI.service.OwnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final OwnerService ownerService;
    private final OwnerAccessService ownerAccessService;

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record LoginResponse(String token) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var jwt = tokenService.generateToken(auth.getName());
        return new LoginResponse(jwt);
    }

    @GetMapping("/me")
    public OwnerResponse me() {
        return OwnerResponse.fromEntity(ownerService.findOwnerById(ownerAccessService.currentOwnerId()));
    }
}
