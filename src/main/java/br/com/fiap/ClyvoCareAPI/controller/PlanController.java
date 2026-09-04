package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PlanRequest;
import br.com.fiap.ClyvoCareAPI.dto.PlanResponse;
import br.com.fiap.ClyvoCareAPI.service.PlanService;
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
@RequestMapping("/planos")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "CRUD dos planos de saúde pet (Bronze, Prata, Ouro, etc)")
public class PlanController {
    private final PlanService planService;

    @GetMapping
    @Operation(summary = "Lista todos os planos disponíveis")
    public List<PlanResponse> findAll() {
        return planService.findAllPlans()
                .stream()
                .map(PlanResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca plano por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano encontrado"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public PlanResponse findById(@PathVariable Long id) {
        return PlanResponse.fromEntity(planService.findPlanById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um novo plano")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação")
    })
    public ResponseEntity<PlanResponse> create(@RequestBody @Valid PlanRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PlanResponse.fromEntity(planService.createPlan(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza um plano existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano atualizado"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public PlanResponse update(@PathVariable Long id, @RequestBody @Valid PlanRequest request) {
        return PlanResponse.fromEntity(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um plano")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano removido"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
