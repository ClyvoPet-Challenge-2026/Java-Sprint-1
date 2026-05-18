package br.com.fiap.ClyvoCareAPI.service;

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
    public List<State> getAllStates() { return stateRepository.findAll(); }

    public State getStateById(Long id) {
        return stateRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("State with ID %d not found", id))
        );
    }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public State addState(State state) { return stateRepository.save(state); }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public State updateState(Long id, State newState) {
        State existing = getStateById(id);
        existing.setName(newState.getName());
        existing.setUf(newState.getUf());
        return stateRepository.save(existing);
    }

    // @Transactional
    @CacheEvict(value = "states", allEntries = true)
    public void deleteState(Long id) {
        if (!stateRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("State with ID %d not found", id));
        }
        stateRepository.deleteById(id);
    }

}
