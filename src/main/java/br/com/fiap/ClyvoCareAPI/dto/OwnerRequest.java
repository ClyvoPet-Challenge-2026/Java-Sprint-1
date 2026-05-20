package br.com.fiap.ClyvoCareAPI.dto;

import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

public record OwnerRequest(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 150, message = "Name must have a max of 150 characters")
        String name,

        @NotBlank(message = "CPF is mandatory")
        @CPF(message = "CPF must be valid")
        String cpf,

        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must have a max of 150 characters")
        String email,

        @NotBlank(message = "Password is mandatory")
        @Size(min = 8, max = 100, message = "Password must have between 8 and 100 characters")
        String password,

        @NotBlank(message = "Phone is mandatory")
        @Size(max = 20, message = "Phone must have a max of 20 characters")
        String phone,

        @NotNull(message = "cityId is mandatory")
        @Positive(message = "cityId must be a positive value")
        Long cityId
) {
    public Owner toEntity(City city, String hashedPassword) {
        return Owner.builder()
                .name(name)
                .cpf(cpf)
                .email(email)
                .passwordHash(hashedPassword)
                .phone(phone)
                .city(city)
                .build();
    }
}
