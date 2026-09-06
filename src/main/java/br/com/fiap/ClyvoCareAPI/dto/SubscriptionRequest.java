package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Plan;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionRequest(
        @NotNull(message = "petId is mandatory")
        @Positive(message = "petId must be a positive value")
        Long petId,

        @NotNull(message = "planId is mandatory")
        @Positive(message = "planId must be a positive value")
        Long planId,

        @NotNull(message = "status is mandatory")
        SubscriptionStatus status,

        @NotNull(message = "paymentMethod is mandatory")
        PaymentMethod paymentMethod
) {
    public Subscription toEntity(Pet pet, Plan plan) {
        return Subscription.builder()
                .pet(pet)
                .plan(plan)
                .status(status)
                .paymentMethod(paymentMethod)
                .contractedValue(plan.getMonthlyValue())
                .build();
    }
}
