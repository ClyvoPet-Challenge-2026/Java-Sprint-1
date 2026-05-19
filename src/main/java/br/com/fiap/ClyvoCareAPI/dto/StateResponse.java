package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.State;

public record StateResponse(
        Long id,
        String name,
        String uf
) {
    public static StateResponse fromEntity(State state) {
        return new StateResponse(
                state.getId(),
                state.getName(),
                state.getUf()
        );
    }
}
