package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
}
