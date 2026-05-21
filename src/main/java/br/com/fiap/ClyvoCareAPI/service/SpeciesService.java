package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.SpeciesRequest;
import br.com.fiap.ClyvoCareAPI.entity.Species;
import br.com.fiap.ClyvoCareAPI.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeciesService {
    private final SpeciesRepository speciesRepository;

    public List<Species> findAllSpecies() {
        return speciesRepository.findAll();
    }

    public Species findSpeciesById(Long id) {
        return speciesRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Species with ID %d not found", id))
        );
    }

    public Species createSpecies(SpeciesRequest request) {
        return speciesRepository.save(request.toEntity());
    }

    public Species updateSpecies(Long id, SpeciesRequest request) {
        Species existing = findSpeciesById(id);
        existing.setName(request.name());
        return speciesRepository.save(existing);
    }

    public void deleteSpecies(Long id) {
        if (!speciesRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Species with ID %d not found", id));
        }
        speciesRepository.deleteById(id);
    }
}
