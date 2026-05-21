package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SubscriptionRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionResponse;
import br.com.fiap.ClyvoCareAPI.service.SubscriptionService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contratacoes")
@RequiredArgsConstructor
@Tag(name = "Contratações", description = "CRUD das contratações de plano. Valor mensal é derivado do Plan automaticamente (cliente não envia)")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Lista contratações com paginação e filtros opcionais por pet, plano, status e forma de pagamento")
    public Page<SubscriptionResponse> findAll(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long paymentMethodId,
            Pageable pageable
    ) {
        return subscriptionService.searchSubscriptions(petId, planId, statusId, paymentMethodId, pageable)
                .map(SubscriptionResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca contratação por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contratação encontrada"),
            @ApiResponse(responseCode = "404", description = "Contratação não encontrada")
    })
    public SubscriptionResponse findById(@PathVariable Long id) {
        return SubscriptionResponse.fromEntity(subscriptionService.findSubscriptionById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova contratação. O contractedValue é derivado do Plan.monthlyValue, não pode ser enviado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contratação criada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Pet, plano, status ou forma de pagamento não encontrado")
    })
    public ResponseEntity<SubscriptionResponse> create(@RequestBody @Valid SubscriptionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SubscriptionResponse.fromEntity(subscriptionService.createSubscription(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma contratação. Trocar plano também recalcula contractedValue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contratação atualizada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Contratação ou alguma FK não encontrada")
    })
    public SubscriptionResponse update(@PathVariable Long id, @RequestBody @Valid SubscriptionRequest request) {
        return SubscriptionResponse.fromEntity(subscriptionService.updateSubscription(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma contratação")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contratação removida"),
            @ApiResponse(responseCode = "404", description = "Contratação não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}
