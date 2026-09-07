package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Plan;
import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscriptionRequest(
        @NotNull(message = "petId is mandatory")
        @Positive(message = "petId must be a positive value")
        Long petId,

        @NotNull(message = "planId is mandatory")
        @Positive(message = "planId must be a positive value")
        Long planId,

        @NotNull(message = "paymentMethod is mandatory")
        PaymentMethod paymentMethod
) {
    public Subscription toEntity(
            Pet pet,
            Plan plan,
            SubscriptionStatus initialStatus,
            BigDecimal contractedValue
    ) {
        return Subscription.builder()
                .pet(pet)
                .plan(plan)
                .status(initialStatus)
                .paymentMethod(paymentMethod)
                .contractedValue(contractedValue)
                .build();
    }
}
