package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.BreedRequest;
import br.com.fiap.ClyvoCareAPI.dto.BreedResponse;
import br.com.fiap.ClyvoCareAPI.service.BreedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/racas")
@RequiredArgsConstructor
@Tag(name = "Raças", description = "CRUD das raças vinculadas a uma espécie (Labrador, Persa, etc)")
public class BreedController {
    private final BreedService breedService;

    @GetMapping
    @Operation(summary = "Lista todas as raças")
    public List<BreedResponse> findAll() {
        return breedService.findAllBreeds()
                .stream()
                .map(BreedResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca raça por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raça encontrada"),
            @ApiResponse(responseCode = "404", description = "Raça não encontrada")
    })
    public BreedResponse findById(@PathVariable Long id) {
        return BreedResponse.fromEntity(breedService.findBreedById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria uma nova raça vinculada a uma espécie existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Raça criada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Espécie informada não existe")
    })
    public ResponseEntity<BreedResponse> create(@RequestBody @Valid BreedRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BreedResponse.fromEntity(breedService.createBreed(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza uma raça existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raça atualizada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Raça ou espécie não encontrada")
    })
    public BreedResponse update(@PathVariable Long id, @RequestBody @Valid BreedRequest request) {
        return BreedResponse.fromEntity(breedService.updateBreed(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove uma raça")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Raça removida"),
            @ApiResponse(responseCode = "404", description = "Raça não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        breedService.deleteBreed(id);
        return ResponseEntity.noContent().build();
    }
}
