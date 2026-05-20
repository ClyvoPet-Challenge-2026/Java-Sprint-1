package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Breed;

public record BreedResponse(
        Long id,
        String name,
        SpeciesResponse species
) {
    public static BreedResponse fromEntity(Breed breed) {
        return new BreedResponse(
                breed.getId(),
                breed.getName(),
                SpeciesResponse.fromEntity(breed.getSpecies())
        );
    }
}
