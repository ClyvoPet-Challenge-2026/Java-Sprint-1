package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.CityRequest;
import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.State;
import br.com.fiap.ClyvoCareAPI.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {
    private final CityRepository cityRepository;
    private final StateService stateService;

    @Cacheable("cities")
    public List<City> findAll() {
        return cityRepository.findAll();
    }

    @Cacheable("cities")
    public City findById(Long id) {
        return cityRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("City with ID %d not found", id))
        );
    }

    @CacheEvict(value = "cities", allEntries = true)
    public City create(CityRequest request) {
        State state = stateService.getStateById(request.stateId());
        return cityRepository.save(request.toEntity(state));
    }

    @CacheEvict(value = "cities", allEntries = true)
    public City update(Long id, CityRequest request) {
        City existing = findById(id);
        State state = stateService.getStateById(request.stateId());
        existing.setName(request.name());
        existing.setState(state);
        return cityRepository.save(existing);
    }

    @CacheEvict(value = "cities", allEntries = true)
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("City with ID %d not found", id));
        }
        cityRepository.deleteById(id);
    }
}
