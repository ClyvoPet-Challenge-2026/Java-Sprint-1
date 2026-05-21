package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.OwnerRequest;
import br.com.fiap.ClyvoCareAPI.dto.OwnerResponse;
import br.com.fiap.ClyvoCareAPI.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/responsaveis")
@RequiredArgsConstructor
@Tag(name = "Responsáveis", description = "CRUD dos responsáveis pelos pets (tutores) com paginação, busca por filtros e senha hasheada via BCrypt")
public class OwnerController {
    private final OwnerService ownerService;

    @GetMapping
    @Operation(summary = "Lista responsáveis com paginação e filtros opcionais por nome, CPF e email")
    public Page<OwnerResponse> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String email,
            Pageable pageable
    ) {
        return ownerService.searchOwners(name, cpf, email, pageable)
                .map(OwnerResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca responsável por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsável encontrado"),
            @ApiResponse(responseCode = "404", description = "Responsável não encontrado")
    })
    public OwnerResponse findById(@PathVariable Long id) {
        return OwnerResponse.fromEntity(ownerService.findOwnerById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um novo responsável (senha é hasheada com BCrypt antes de salvar)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Responsável criado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação (CPF/email inválido, campos obrigatórios)"),
            @ApiResponse(responseCode = "404", description = "Cidade informada não existe"),
            @ApiResponse(responseCode = "409", description = "CPF ou email já cadastrado")
    })
    public ResponseEntity<OwnerResponse> create(@RequestBody @Valid OwnerRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OwnerResponse.fromEntity(ownerService.createOwner(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um responsável existente (senha é re-hasheada)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsável atualizado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Responsável ou cidade não encontrado")
    })
    public OwnerResponse update(@PathVariable Long id, @RequestBody @Valid OwnerRequest request) {
        return OwnerResponse.fromEntity(ownerService.updateOwner(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um responsável")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Responsável removido"),
            @ApiResponse(responseCode = "404", description = "Responsável não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ownerService.deleteOwner(id);
        return ResponseEntity.noContent().build();
    }
}
