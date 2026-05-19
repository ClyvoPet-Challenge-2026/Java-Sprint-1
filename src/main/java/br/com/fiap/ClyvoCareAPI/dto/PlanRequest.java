package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must have a max of 50 characters")
        String name,

        @NotNull(message = "monthlyValue is mandatory")
        @Positive(message = "monthlyValue must be a positive number")
        BigDecimal monthlyValue
) {
    public Plan toEntity() {
        return Plan.builder()
                .name(name)
                .monthlyValue(monthlyValue)
                .build();
    }
}

