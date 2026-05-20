package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Sex;

import java.time.LocalDate;

public record PetResponse(
        Long id,
        String name,
        LocalDate birthDate,
        Sex sex,
        OwnerResponse owner,
        SpeciesResponse species,
        BreedResponse breed
) {
    public static PetResponse fromEntity(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getBirthDate(),
                pet.getSex(),
                OwnerResponse.fromEntity(pet.getOwner()),
                SpeciesResponse.fromEntity(pet.getSpecies()),
                pet.getBreed() != null ? BreedResponse.fromEntity(pet.getBreed()) : null
        );
    }
}
