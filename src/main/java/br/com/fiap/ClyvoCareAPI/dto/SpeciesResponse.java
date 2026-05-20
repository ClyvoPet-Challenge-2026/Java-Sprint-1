package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Species;

import java.time.LocalDateTime;

public record SpeciesResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {
    public static SpeciesResponse fromEntity(Species species) {
        return new SpeciesResponse(
                species.getId(),
                species.getName(),
                species.getCreatedAt()
        );
    }
}
