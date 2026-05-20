package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Breed;
import br.com.fiap.ClyvoCareAPI.entity.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BreedRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 100, message = "Name must have a max of 100 characters")
        String name,

        @NotNull(message = "speciesId is mandatory")
        @Positive(message = "speciesId must be a positive value")
        Long speciesId
) {
    public Breed toEntity(Species species) {
        return Breed.builder()
                .name(name)
                .species(species)
                .build();
    }
}
