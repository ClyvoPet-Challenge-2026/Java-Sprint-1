package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PetRequest;
import br.com.fiap.ClyvoCareAPI.dto.PetResponse;
import br.com.fiap.ClyvoCareAPI.service.PetService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "CRUD dos pets com paginação e filtros por nome, dono, espécie e raça")
public class PetController {
    private final PetService petService;

    @GetMapping
    @Operation(summary = "Lista pets com paginação e filtros opcionais")
    public Page<PetResponse> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long breedId,
            Pageable pageable
    ) {
        return petService.searchPets(name, ownerId, speciesId, breedId, pageable)
                .map(PetResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca pet por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pet encontrado"),
            @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    public PetResponse findById(@PathVariable Long id) {
        return PetResponse.fromEntity(petService.findPetById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Cadastra um novo pet (raça é opcional para vira-latas)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pet criado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação (data futura, sexo inválido, campos obrigatórios)"),
            @ApiResponse(responseCode = "404", description = "Owner, espécie ou raça informada não existe")
    })
    public ResponseEntity<PetResponse> create(@RequestBody @Valid PetRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PetResponse.fromEntity(petService.createPet(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Atualiza um pet existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pet atualizado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Pet, owner, espécie ou raça não encontrado")
    })
    public PetResponse update(@PathVariable Long id, @RequestBody @Valid PetRequest request) {
        return PetResponse.fromEntity(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Remove um pet")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pet removido"),
            @ApiResponse(responseCode = "404", description = "Pet não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
