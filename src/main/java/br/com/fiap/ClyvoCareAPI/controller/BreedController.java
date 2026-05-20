package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.BreedRequest;
import br.com.fiap.ClyvoCareAPI.dto.BreedResponse;
import br.com.fiap.ClyvoCareAPI.service.BreedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/racas")
@RequiredArgsConstructor
public class BreedController {
    private final BreedService breedService;

    @GetMapping
    public List<BreedResponse> findAll() {
        return breedService.findAllBreeds()
                .stream()
                .map(BreedResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public BreedResponse findById(@PathVariable Long id) {
        return BreedResponse.fromEntity(breedService.findBreedById(id));
    }

    @PostMapping
    public ResponseEntity<BreedResponse> create(@RequestBody @Valid BreedRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BreedResponse.fromEntity(breedService.createBreed(request)));
    }

    @PutMapping("/{id}")
    public BreedResponse update(@PathVariable Long id, @RequestBody @Valid BreedRequest request) {
        return BreedResponse.fromEntity(breedService.updateBreed(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        breedService.deleteBreed(id);
        return ResponseEntity.noContent().build();
    }
}
