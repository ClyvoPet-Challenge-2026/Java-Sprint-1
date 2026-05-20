package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpeciesRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must have a max of 50 characters")
        String name
) {
    public Species toEntity() {
        return Species.builder()
                .name(name)
                .build();
    }
}
