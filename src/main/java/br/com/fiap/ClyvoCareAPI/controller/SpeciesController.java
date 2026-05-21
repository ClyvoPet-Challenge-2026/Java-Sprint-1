package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SpeciesRequest;
import br.com.fiap.ClyvoCareAPI.dto.SpeciesResponse;
import br.com.fiap.ClyvoCareAPI.service.SpeciesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especies")
@RequiredArgsConstructor
@Tag(name = "Espécies", description = "CRUD das espécies de pets cadastradas (Cão, Gato, etc)")
public class SpeciesController {
    private final SpeciesService speciesService;

    @GetMapping
    @Operation(summary = "Lista todas as espécies")
    public List<SpeciesResponse> findAll() {
        return speciesService.findAllSpecies()
                .stream()
                .map(SpeciesResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca espécie por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Espécie encontrada"),
            @ApiResponse(responseCode = "404", description = "Espécie não encontrada")
    })
    public SpeciesResponse findById(@PathVariable Long id) {
        return SpeciesResponse.fromEntity(speciesService.findSpeciesById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova espécie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Espécie criada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação")
    })
    public ResponseEntity<SpeciesResponse> create(@RequestBody @Valid SpeciesRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SpeciesResponse.fromEntity(speciesService.createSpecies(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma espécie existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Espécie atualizada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Espécie não encontrada")
    })
    public SpeciesResponse update(@PathVariable Long id, @RequestBody @Valid SpeciesRequest request) {
        return SpeciesResponse.fromEntity(speciesService.updateSpecies(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma espécie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Espécie removida"),
            @ApiResponse(responseCode = "404", description = "Espécie não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        speciesService.deleteSpecies(id);
        return ResponseEntity.noContent().build();
    }
}
