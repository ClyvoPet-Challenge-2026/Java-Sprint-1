package br.com.fiap.ClyvoCareAPI.service;
import br.com.fiap.ClyvoCareAPI.dto.PlanRequest;
import br.com.fiap.ClyvoCareAPI.entity.Plan;
import br.com.fiap.ClyvoCareAPI.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;


    public List<Plan> findAllPlans() {
        return planRepository.findAll();
    }

    public Plan findPlanById(Long id) {
        return planRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Plan with ID %d not found", id))
        );
    }

    public Plan createPlan(PlanRequest request) {
        return planRepository.save(request.toEntity());
    }

    public Plan updatePlan(Long id, PlanRequest request) {
        Plan existing = findPlanById(id);
        existing.setName(request.name());
        existing.setMonthlyValue(request.monthlyValue());
        return planRepository.save(existing);
    }

    public void deletePlan(Long id) {
        if (!planRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Plan with ID %d not found", id));
        }
        planRepository.deleteById(id);
    }

}

