package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
}
