package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PetRequest;
import br.com.fiap.ClyvoCareAPI.dto.PetResponse;
import br.com.fiap.ClyvoCareAPI.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @GetMapping
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
    public PetResponse findById(@PathVariable Long id) {
        return PetResponse.fromEntity(petService.findPetById(id));
    }

    @PostMapping
    public ResponseEntity<PetResponse> create(@RequestBody @Valid PetRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PetResponse.fromEntity(petService.createPet(request)));
    }

    @PutMapping("/{id}")
    public PetResponse update(@PathVariable Long id, @RequestBody @Valid PetRequest request) {
        return PetResponse.fromEntity(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
