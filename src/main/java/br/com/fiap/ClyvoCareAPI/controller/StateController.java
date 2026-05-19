package br.com.fiap.ClyvoCareAPI.controller;
import br.com.fiap.ClyvoCareAPI.dto.StateRequest;
import br.com.fiap.ClyvoCareAPI.dto.StateResponse;
import br.com.fiap.ClyvoCareAPI.service.StateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estados")
@RequiredArgsConstructor
public class StateController {
    private final StateService stateService;

    @GetMapping
    public List<StateResponse> findAll() {
        return stateService.findAllStates()
                .stream()
                .map(StateResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public StateResponse findById(@PathVariable Long id) {
        return StateResponse.fromEntity(stateService.findStateById(id));
    }

    @PostMapping
    public ResponseEntity<StateResponse> create(@RequestBody @Valid StateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(StateResponse.fromEntity(stateService.createState(request)));
    }

    @PutMapping("/{id}")
    public StateResponse update(@PathVariable Long id, @RequestBody @Valid StateRequest request) {
        return StateResponse.fromEntity(stateService.updateState(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stateService.deleteState(id);
        return ResponseEntity.noContent().build();
    }
}

