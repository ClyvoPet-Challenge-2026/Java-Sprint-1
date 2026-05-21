package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PaymentMethodRequest;
import br.com.fiap.ClyvoCareAPI.dto.PaymentMethodResponse;
import br.com.fiap.ClyvoCareAPI.service.PaymentMethodService;
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
@RequestMapping("/formas-pagamento")
@RequiredArgsConstructor
@Tag(name = "Formas de Pagamento", description = "CRUD das formas de pagamento aceitas (Pix, Cartão, Boleto, etc)")
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @Operation(summary = "Lista todas as formas de pagamento")
    public List<PaymentMethodResponse> findAll() {
        return paymentMethodService.findAllPaymentMethods()
                .stream()
                .map(PaymentMethodResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca forma de pagamento por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Forma de pagamento encontrada"),
            @ApiResponse(responseCode = "404", description = "Forma de pagamento não encontrada")
    })
    public PaymentMethodResponse findById(@PathVariable Long id) {
        return PaymentMethodResponse.fromEntity(paymentMethodService.findPaymentMethodById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova forma de pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Forma de pagamento criada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação")
    })
    public ResponseEntity<PaymentMethodResponse> create(@RequestBody @Valid PaymentMethodRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PaymentMethodResponse.fromEntity(paymentMethodService.createPaymentMethod(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma forma de pagamento existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Forma de pagamento atualizada"),
            @ApiResponse(responseCode = "400", description = "Erro de validação"),
            @ApiResponse(responseCode = "404", description = "Forma de pagamento não encontrada")
    })
    public PaymentMethodResponse update(@PathVariable Long id, @RequestBody @Valid PaymentMethodRequest request) {
        return PaymentMethodResponse.fromEntity(paymentMethodService.updatePaymentMethod(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma forma de pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Forma de pagamento removida"),
            @ApiResponse(responseCode = "404", description = "Forma de pagamento não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.noContent().build();
    }
}
