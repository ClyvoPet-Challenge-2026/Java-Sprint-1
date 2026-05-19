package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.City;

public record CityResponse(
        Long id,
        String name,
        StateResponse state
) {
    public static CityResponse fromEntity(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                StateResponse.fromEntity(city.getState())
        );
    }
}
