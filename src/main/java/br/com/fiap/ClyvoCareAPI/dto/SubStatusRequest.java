package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.SubStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubStatusRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must have a max of 50 characters")
        String name
) {
    public SubStatus toEntity() {
        return SubStatus.builder()
                .name(name)
                .build();
    }
}
