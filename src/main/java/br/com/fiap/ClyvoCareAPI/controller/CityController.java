package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.CityRequest;
import br.com.fiap.ClyvoCareAPI.dto.CityResponse;
import br.com.fiap.ClyvoCareAPI.service.CityService;
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
@RequestMapping("/cidades")
@RequiredArgsConstructor
@Tag(name = "Cidades", description = "CRUD de cidades com referência a estado (lookup, leitura cacheada)")
public class CityController {
    private final CityService cityService;

    @GetMapping
    @Operation(summary = "Lista todas as cidades")
    public List<CityResponse> findAll() {
        return cityService.findAllCities().stream()
                .map(CityResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca cidade por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cidade encontrada"),
            @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    })
    public CityResponse findById(@PathVariable Long id) {
        return CityResponse.fromEntity(cityService.findCityById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova cidade vinculada a um estado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cidade criada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Estado informado não existe")
    })
    public ResponseEntity<CityResponse> create(@RequestBody @Valid CityRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CityResponse.fromEntity(cityService.createCity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma cidade existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cidade atualizada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Cidade ou estado não encontrado")
    })
    public CityResponse update(@PathVariable Long id, @RequestBody @Valid CityRequest request) {
        return CityResponse.fromEntity(cityService.updateCity(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma cidade")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cidade removida"),
            @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
