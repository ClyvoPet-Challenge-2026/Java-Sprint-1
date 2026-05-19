package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CityRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 100, message = "Name must have a max of 100 characters")
        String name,

        @NotNull(message = "stateId is mandatory")
        @Positive(message = "stateId must be a positive value")
        Long stateId
) {
    public City toEntity(State state) {
        return City.builder()
                .name(name)
                .state(state)
                .build();
    }
}
