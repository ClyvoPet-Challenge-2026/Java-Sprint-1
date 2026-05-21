package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StateRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must have a max of 50 characters")
        String name,

        @NotBlank(message = "UF is mandatory")
        @Size(min = 2, max = 2, message = "UF must have exactly 2 characters")
        String uf
) {
    public State toEntity() {
        return State.builder()
                .name(name)
                .uf(uf)
                .build();
    }
}
