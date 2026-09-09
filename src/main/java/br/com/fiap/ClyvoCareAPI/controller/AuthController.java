package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.LoginRequest;
import br.com.fiap.ClyvoCareAPI.dto.OwnerResponse;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação para login no sistema")
public class AuthController {

    private final OwnerService ownerService;

    @PostMapping("/login")
    @Operation(summary = "Autentica um responsável por email e senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<OwnerResponse> login(@RequestBody @Valid LoginRequest request) {
        Owner owner = ownerService.authenticate(request.email(), request.password());
        return ResponseEntity.ok(OwnerResponse.fromEntity(owner));
    }
}
