package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.OwnerRequest;
import br.com.fiap.ClyvoCareAPI.auth.OwnerAccessService;
import br.com.fiap.ClyvoCareAPI.entity.City;
import br.com.fiap.ClyvoCareAPI.entity.Owner;
import br.com.fiap.ClyvoCareAPI.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class OwnerService {
    private final OwnerRepository ownerRepository;
    private final CityService cityService;
    private final PasswordEncoder passwordEncoder;
    private final OwnerAccessService ownerAccess;

    public Page<Owner> findAllOwners(Pageable pageable) {
        return ownerRepository.findAll(pageable);
    }

    public Page<Owner> searchOwners(String name, String cpf, String email, Pageable pageable) {
        return ownerRepository.search(name, cpf, email, pageable);
    }

    public Owner findOwnerById(Long id) {
        ownerAccess.restrictOwnerId(id);
        return ownerRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Owner with ID %d not found", id))
        );
    }

    public Owner findOwnerByEmail(String email) {
        return ownerRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Owner with email %s not found", email))
        );
    }

    @Transactional
    public Owner createOwner(OwnerRequest request) {
        if (ownerRepository.existsByCpf(request.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("CPF %s is already registered", request.cpf()));
        }
        if (ownerRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Email %s is already registered", request.email()));
        }
        City city = cityService.findCityById(request.cityId());
        String hashedPassword = passwordEncoder.encode(request.password());
        Owner owner = request.toEntity(city, hashedPassword);
        owner.setRoleName("OWNER");
        owner.setEnabled(true);
        return saveOwner(owner);
    }

    @Transactional
    public Owner updateOwner(Long id, OwnerRequest request) {
        Owner existing = findOwnerById(id);
        if (ownerRepository.existsByCpfAndIdNot(request.cpf(), id)
                || ownerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF or email is already registered");
        }
        City city = cityService.findCityById(request.cityId());
        existing.setName(request.name());
        existing.setCpf(request.cpf());
        existing.setEmail(request.email());
        existing.setPhone(request.phone());
        existing.setCity(city);
        existing.setPasswordHash(passwordEncoder.encode(request.password()));
        return saveOwner(existing);
    }

    @Transactional
    public void deleteOwner(Long id) {
        Owner existing = findOwnerById(id);
        existing.setEnabled(false);
        ownerRepository.save(existing);
    }

    private Owner saveOwner(Owner owner) {
        try {
            return ownerRepository.saveAndFlush(owner);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner data conflicts with an existing record");
        }
    }
}
