package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.SubStatusRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubStatusResponse;
import br.com.fiap.ClyvoCareAPI.service.SubStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/status-contratacao")
@RequiredArgsConstructor
public class SubStatusController {
    private final SubStatusService subStatusService;

    @GetMapping
    public List<SubStatusResponse> findAll() {
        return subStatusService.findAllSubStatus()
                .stream()
                .map(SubStatusResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public SubStatusResponse findById(@PathVariable Long id) {
        return SubStatusResponse.fromEntity(subStatusService.findSubStatusById(id));
    }

    @PostMapping
    public ResponseEntity<SubStatusResponse> create(@RequestBody @Valid SubStatusRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SubStatusResponse.fromEntity(subStatusService.createSubStatus(request)));
    }

    @PutMapping("/{id}")
    public SubStatusResponse update(@PathVariable Long id, @RequestBody @Valid SubStatusRequest request) {
        return SubStatusResponse.fromEntity(subStatusService.updateSubStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subStatusService.deleteSubStatus(id);
        return ResponseEntity.noContent().build();
    }
}
