package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PaymentMethodRequest;
import br.com.fiap.ClyvoCareAPI.dto.PaymentMethodResponse;
import br.com.fiap.ClyvoCareAPI.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/metodos")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public List<PaymentMethodResponse> findAll() {
        return paymentMethodService.findAllPaymentMethods()
                .stream()
                .map(PaymentMethodResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public PaymentMethodResponse findById(@PathVariable Long id) {
        return PaymentMethodResponse.fromEntity(paymentMethodService.findPaymentMethodById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@RequestBody @Valid PaymentMethodRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PaymentMethodResponse.fromEntity(paymentMethodService.createPaymentMethod(request)));
    }

    @PutMapping("/{id}")
    public PaymentMethodResponse update(@PathVariable Long id, @RequestBody @Valid PaymentMethodRequest request) {
        return PaymentMethodResponse.fromEntity(paymentMethodService.updatePaymentMethod(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.noContent().build();
    }
}