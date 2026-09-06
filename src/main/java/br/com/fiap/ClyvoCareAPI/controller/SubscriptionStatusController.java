package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/status-contratacao")
@Tag(name = "Status de Contratação", description = "Valores fixos permitidos para uma contratação")
public class SubscriptionStatusController {

    @GetMapping
    @Operation(summary = "Lista os status de contratação, sem consulta ao banco")
    public List<SubscriptionStatus> findAll() {
        return List.of(SubscriptionStatus.values());
    }
}
