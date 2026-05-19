package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.StateRequest;
import br.com.fiap.ClyvoCareAPI.entity.State;
import br.com.fiap.ClyvoCareAPI.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
// @Transactional(readOnly = true) //perguntar pro professor se devo usar
public class StateService {
    private final StateRepository stateRepository;

    @Cacheable("states")
    public List<State> findAllStates() {
        return stateRepository.findAll();
    }

    public State findStateById(Long id) {
        return stateRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("State with ID %d not found", id))
        );
    }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public State createState(StateRequest request) {
        return stateRepository.save(request.toEntity());
    }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public State updateState(Long id, StateRequest request) {
        State existing = findStateById(id);
        existing.setName(request.name());
        existing.setUf(request.uf());
        return stateRepository.save(existing);
    }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public void deleteState(Long id) {
        if (!stateRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("State with ID %d not found", id));
        }
        stateRepository.deleteById(id);
    }

}
