package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Owner;

import java.time.LocalDateTime;

public record OwnerResponse(
        Long id,
        String name,
        String cpf,
        String email,
        String phone,
        CityResponse city,
        LocalDateTime createdAt
) {
    public static OwnerResponse fromEntity(Owner owner) {
        return new OwnerResponse(
                owner.getId(),
                owner.getName(),
                owner.getCpf(),
                owner.getEmail(),
                owner.getPhone(),
                CityResponse.fromEntity(owner.getCity()),
                owner.getCreatedAt()
        );
    }
}
