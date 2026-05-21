package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.PetRequest;
import br.com.fiap.ClyvoCareAPI.entity.Breed;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Species;
import br.com.fiap.ClyvoCareAPI.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PetService {
    private final PetRepository petRepository;
    private final OwnerService ownerService;
    private final SpeciesService speciesService;
    private final BreedService breedService;

    public Page<Pet> searchPets(String name, Long ownerId, Long speciesId, Long breedId, Pageable pageable) {
        return petRepository.search(name, ownerId, speciesId, breedId, pageable);
    }

    public Pet findPetById(Long id) {
        return petRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Pet with ID %d not found", id))
        );
    }

    public Pet createPet(PetRequest request) {
        Owner owner = ownerService.findOwnerById(request.ownerId());
        Species species = speciesService.findSpeciesById(request.speciesId());
        Breed breed = request.breedId() != null
                ? breedService.findBreedById(request.breedId())
                : null;
        return petRepository.save(request.toEntity(owner, species, breed));
    }

    public Pet updatePet(Long id, PetRequest request) {
        Pet existing = findPetById(id);
        Owner owner = ownerService.findOwnerById(request.ownerId());
        Species species = speciesService.findSpeciesById(request.speciesId());
        Breed breed = request.breedId() != null
                ? breedService.findBreedById(request.breedId())
                : null;
        existing.setName(request.name());
        existing.setBirthDate(request.birthDate());
        existing.setSex(request.sex());
        existing.setOwner(owner);
        existing.setSpecies(species);
        existing.setBreed(breed);
        return petRepository.save(existing);
    }

    public void deletePet(Long id) {
        if (!petRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Pet with ID %d not found", id));
        }
        petRepository.deleteById(id);
    }
}
