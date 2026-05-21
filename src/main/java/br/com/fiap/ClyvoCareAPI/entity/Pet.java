package br.com.fiap.ClyvoCareAPI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@Table(name = "TB_CAD_PET")
@AllArgsConstructor
@NoArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Owner owner;
    @ManyToOne
    private Species species;
    @ManyToOne
    private Breed breed;
    private String name;
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING)
    private Sex sex;
}
