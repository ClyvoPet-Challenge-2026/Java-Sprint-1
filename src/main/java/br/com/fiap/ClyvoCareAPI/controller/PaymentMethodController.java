package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/formas-pagamento")
@Tag(name = "Formas de Pagamento", description = "Formas de pagamento aceitas nas contratações")
public class PaymentMethodController {
    @GetMapping
    @Operation(summary = "Lista as formas de pagamento aceitas")
    public List<PaymentMethod> findAll() {
        return List.of(PaymentMethod.values());
    }
}
