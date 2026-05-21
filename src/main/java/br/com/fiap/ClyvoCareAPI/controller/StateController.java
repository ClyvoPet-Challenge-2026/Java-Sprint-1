package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.StateRequest;
import br.com.fiap.ClyvoCareAPI.dto.StateResponse;
import br.com.fiap.ClyvoCareAPI.service.StateService;
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
@RequestMapping("/estados")
@RequiredArgsConstructor
@Tag(name = "Estados", description = "CRUD de estados brasileiros (lookup, leitura cacheada)")
public class StateController {
    private final StateService stateService;

    @GetMapping
    @Operation(summary = "Lista todos os estados")
    public List<StateResponse> findAll() {
        return stateService.findAllStates()
                .stream()
                .map(StateResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca estado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado encontrado"),
            @ApiResponse(responseCode = "404", description = "Estado não encontrado")
    })
    public StateResponse findById(@PathVariable Long id) {
        return StateResponse.fromEntity(stateService.findStateById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um novo estado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estado criado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação")
    })
    public ResponseEntity<StateResponse> create(@RequestBody @Valid StateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StateResponse.fromEntity(stateService.createState(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um estado existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado atualizado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Estado não encontrado")
    })
    public StateResponse update(@PathVariable Long id, @RequestBody @Valid StateRequest request) {
        return StateResponse.fromEntity(stateService.updateState(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um estado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estado removido"),
            @ApiResponse(responseCode = "404", description = "Estado não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stateService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}
