package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CityRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        Long stateId
) {
    public City toEntity(State state) {
        return City.builder()
                .name(name)
                .state(state)
                .build();
    }
}
