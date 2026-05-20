package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.SubStatusRequest;
import br.com.fiap.ClyvoCareAPI.entity.SubStatus;
import br.com.fiap.ClyvoCareAPI.repository.SubStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubStatusService {
    private final SubStatusRepository subStatusRepository;


    public List<SubStatus> findAllSubStatus() {
        return subStatusRepository.findAll();
    }

    public SubStatus findSubStatusById(Long id) {
        return subStatusRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("SubscriptionStatus with ID %d not found", id))
        );
    }

    public SubStatus createSubStatus(SubStatusRequest request) {
        return subStatusRepository.save(request.toEntity());
    }

    public SubStatus updateSubStatus(Long id, SubStatusRequest request) {
        SubStatus existing = findSubStatusById(id);
        existing.setName(request.name());
        return subStatusRepository.save(existing);
    }

    public void deleteSubStatus(Long id) {
        if (!subStatusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("SubscriptionStatus with ID %d not found", id));
        }
        subStatusRepository.deleteById(id);
    }

}


