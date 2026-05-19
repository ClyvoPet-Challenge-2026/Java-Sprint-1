package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.PlanRequest;
import br.com.fiap.ClyvoCareAPI.dto.PlanResponse;
import br.com.fiap.ClyvoCareAPI.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planos")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    @GetMapping
    public List<PlanResponse> findAll() {
        return planService.findAllPlans()
                .stream()
                .map(PlanResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public PlanResponse findById(@PathVariable Long id) {
        return PlanResponse.fromEntity(planService.findPlanById(id));
    }

    @PostMapping
    public ResponseEntity<PlanResponse> create(@RequestBody @Valid PlanRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PlanResponse.fromEntity(planService.createPlan(request)));
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable Long id, @RequestBody @Valid PlanRequest request) {
        return PlanResponse.fromEntity(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}

