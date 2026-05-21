package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SubStatusRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubStatusResponse;
import br.com.fiap.ClyvoCareAPI.service.SubStatusService;
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
@RequestMapping("/status-contratacao")
@RequiredArgsConstructor
@Tag(name = "Status de Contratação", description = "CRUD dos status possíveis de uma contratação (ATIVO, CANCELADO, SUSPENSO)")
public class SubStatusController {
    private final SubStatusService subStatusService;

    @GetMapping
    @Operation(summary = "Lista todos os status de contratação")
    public List<SubStatusResponse> findAll() {
        return subStatusService.findAllSubStatus()
                .stream()
                .map(SubStatusResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca status por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status encontrado"),
            @ApiResponse(responseCode = "404", description = "Status não encontrado")
    })
    public SubStatusResponse findById(@PathVariable Long id) {
        return SubStatusResponse.fromEntity(subStatusService.findSubStatusById(id));
    }

    @PostMapping
    @Operation(summary = "Cria um novo status de contratação")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Status criado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação")
    })
    public ResponseEntity<SubStatusResponse> create(@RequestBody @Valid SubStatusRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SubStatusResponse.fromEntity(subStatusService.createSubStatus(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um status existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Status não encontrado")
    })
    public SubStatusResponse update(@PathVariable Long id, @RequestBody @Valid SubStatusRequest request) {
        return SubStatusResponse.fromEntity(subStatusService.updateSubStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um status")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status removido"),
            @ApiResponse(responseCode = "404", description = "Status não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subStatusService.deleteSubStatus(id);
        return ResponseEntity.noContent().build();
    }
}
