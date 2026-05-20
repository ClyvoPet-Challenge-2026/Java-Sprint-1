package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SubscriptionRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionResponse;
import br.com.fiap.ClyvoCareAPI.service.SubscriptionService;
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
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping
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
    public SubscriptionResponse findById(@PathVariable Long id) {
        return SubscriptionResponse.fromEntity(subscriptionService.findSubscriptionById(id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@RequestBody @Valid SubscriptionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SubscriptionResponse.fromEntity(subscriptionService.createSubscription(request)));
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Long id, @RequestBody @Valid SubscriptionRequest request) {
        return SubscriptionResponse.fromEntity(subscriptionService.updateSubscription(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}
