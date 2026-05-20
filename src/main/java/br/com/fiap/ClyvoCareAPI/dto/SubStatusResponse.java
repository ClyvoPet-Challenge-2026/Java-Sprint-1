package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.SubStatus;

public record SubStatusResponse(
        Long id,
        String name
) {
    public static SubStatusResponse fromEntity(SubStatus subscriptionStatus) {
        return new SubStatusResponse(
                subscriptionStatus.getId(),
                subscriptionStatus.getName()
        );
    }
}
