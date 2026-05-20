package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;


public record PaymentMethodResponse(
        Long id,
        String name
) {
    public static PaymentMethodResponse fromEntity(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getName()
        );
    }
}
