package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        LocalDate startDate,
        BigDecimal contractedValue,
        PetResponse pet,
        PlanResponse plan,
        SubscriptionStatus status,
        PaymentMethod paymentMethod
) {
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getStartDate(),
                subscription.getContractedValue(),
                PetResponse.fromEntity(subscription.getPet()),
                PlanResponse.fromEntity(subscription.getPlan()),
                subscription.getStatus(),
                subscription.getPaymentMethod()
        );
    }
}
