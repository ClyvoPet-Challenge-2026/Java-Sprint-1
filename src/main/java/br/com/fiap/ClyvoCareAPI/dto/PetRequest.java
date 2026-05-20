package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Breed;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Sex;
import br.com.fiap.ClyvoCareAPI.entity.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 100, message = "Name must have a max of 100 characters")
        String name,

        @NotNull(message = "birthDate is mandatory")
        @PastOrPresent(message = "birthDate cannot be in the future")
        LocalDate birthDate,

        @NotNull(message = "sex is mandatory")
        Sex sex,

        @NotNull(message = "ownerId is mandatory")
        @Positive(message = "ownerId must be a positive value")
        Long ownerId,

        @NotNull(message = "speciesId is mandatory")
        @Positive(message = "speciesId must be a positive value")
        Long speciesId,

        @Positive(message = "breedId must be a positive value")
        Long breedId
) {
    public Pet toEntity(Owner owner, Species species, Breed breed) {
        return Pet.builder()
                .name(name)
                .birthDate(birthDate)
                .sex(sex)
                .owner(owner)
                .species(species)
                .breed(breed)
                .build();
    }
}
