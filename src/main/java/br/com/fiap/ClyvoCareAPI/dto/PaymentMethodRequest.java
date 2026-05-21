package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record PaymentMethodRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must have a max of 50 characters")
        String name
) {
    public PaymentMethod toEntity() {
        return PaymentMethod.builder()
                .name(name)
                .build();
    }
}
