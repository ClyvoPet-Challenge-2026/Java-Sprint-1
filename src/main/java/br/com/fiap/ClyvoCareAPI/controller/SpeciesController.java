package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SpeciesRequest;
import br.com.fiap.ClyvoCareAPI.dto.SpeciesResponse;
import br.com.fiap.ClyvoCareAPI.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especies")
@RequiredArgsConstructor
public class SpeciesController {
    private final SpeciesService speciesService;

    @GetMapping
    public List<SpeciesResponse> findAll() {
        return speciesService.findAllSpecies()
                .stream()
                .map(SpeciesResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public SpeciesResponse findById(@PathVariable Long id) {
        return SpeciesResponse.fromEntity(speciesService.findSpeciesById(id));
    }

    @PostMapping
    public ResponseEntity<SpeciesResponse> create(@RequestBody @Valid SpeciesRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SpeciesResponse.fromEntity(speciesService.createSpecies(request)));
    }

    @PutMapping("/{id}")
    public SpeciesResponse update(@PathVariable Long id, @RequestBody @Valid SpeciesRequest request) {
        return SpeciesResponse.fromEntity(speciesService.updateSpecies(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        speciesService.deleteSpecies(id);
        return ResponseEntity.noContent().build();
    }
}
