package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Plan;

import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String name,
        BigDecimal monthlyValue
) {
    public static PlanResponse fromEntity(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getMonthlyValue()
        );
    }
}