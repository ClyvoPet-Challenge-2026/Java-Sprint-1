package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.service.ContractPricingService;

import java.math.BigDecimal;

public record SubscriptionSimulationResponse(
        BigDecimal baseValue,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        BigDecimal finalValue
) {
    public static SubscriptionSimulationResponse fromCalculation(ContractPricingService.PriceCalculation calculation) {
        return new SubscriptionSimulationResponse(
                calculation.baseValue(),
                calculation.discountRate(),
                calculation.discountAmount(),
                calculation.finalValue()
        );
    }
}
