package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionSimulationRequest(
        @NotNull(message = "planId is mandatory")
        @Positive(message = "planId must be a positive value")
        Long planId,

        @NotNull(message = "paymentMethod is mandatory")
        PaymentMethod paymentMethod
) {
}
