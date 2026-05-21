package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.BreedRequest;
import br.com.fiap.ClyvoCareAPI.entity.Breed;
import br.com.fiap.ClyvoCareAPI.entity.Species;
import br.com.fiap.ClyvoCareAPI.repository.BreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BreedService {
    private final BreedRepository breedRepository;
    private final SpeciesService speciesService;

    public List<Breed> findAllBreeds() {
        return breedRepository.findAll();
    }

    public Breed findBreedById(Long id) {
        return breedRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Breed with ID %d not found", id))
        );
    }

    public Breed createBreed(BreedRequest request) {
        Species species = speciesService.findSpeciesById(request.speciesId());
        return breedRepository.save(request.toEntity(species));
    }

    public Breed updateBreed(Long id, BreedRequest request) {
        Breed existing = findBreedById(id);
        Species species = speciesService.findSpeciesById(request.speciesId());
        existing.setName(request.name());
        existing.setSpecies(species);
        return breedRepository.save(existing);
    }

    public void deleteBreed(Long id) {
        if (!breedRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Breed with ID %d not found", id));
        }
        breedRepository.deleteById(id);
    }
}
