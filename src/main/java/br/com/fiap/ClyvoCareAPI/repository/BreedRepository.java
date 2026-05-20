package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Breed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedRepository extends JpaRepository<Breed, Long> {
}
