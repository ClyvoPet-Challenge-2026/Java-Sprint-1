package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.CityRequest;
import br.com.fiap.ClyvoCareAPI.dto.CityResponse;
import br.com.fiap.ClyvoCareAPI.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cidades")
@RequiredArgsConstructor
public class CityController {
    private final CityService cityService;

    @GetMapping
    public List<CityResponse> findAll() {
        return cityService.findAllCities().stream()
                .map(CityResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public CityResponse findById(@PathVariable Long id) {
        return CityResponse.fromEntity(cityService.findCityById(id));
    }

    @PostMapping
    public ResponseEntity<CityResponse> create(@RequestBody @Valid CityRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CityResponse.fromEntity(cityService.createCity(request)));
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable Long id, @RequestBody @Valid CityRequest request) {
        return CityResponse.fromEntity(cityService.updateCity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
