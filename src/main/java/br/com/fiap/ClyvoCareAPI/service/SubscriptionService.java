package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.SubscriptionRequest;
import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Plan;
import br.com.fiap.ClyvoCareAPI.entity.SubStatus;
import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final PetService petService;
    private final PlanService planService;
    private final SubStatusService subStatusService;
    private final PaymentMethodService paymentMethodService;

    public Page<Subscription> searchSubscriptions(Long petId, Long planId, Long statusId, Long paymentMethodId, Pageable pageable) {
        return subscriptionRepository.search(petId, planId, statusId, paymentMethodId, pageable);
    }

    public Subscription findSubscriptionById(Long id) {
        return subscriptionRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Subscription with ID %d not found", id))
        );
    }

    public Subscription createSubscription(SubscriptionRequest request) {
        Pet pet = petService.findPetById(request.petId());
        Plan plan = planService.findPlanById(request.planId());
        SubStatus status = subStatusService.findSubStatusById(request.statusId());
        PaymentMethod paymentMethod = paymentMethodService.findPaymentMethodById(request.paymentMethodId());
        return subscriptionRepository.save(request.toEntity(pet, plan, status, paymentMethod));
    }

    public Subscription updateSubscription(Long id, SubscriptionRequest request) {
        Subscription existing = findSubscriptionById(id);
        Pet pet = petService.findPetById(request.petId());
        Plan plan = planService.findPlanById(request.planId());
        SubStatus status = subStatusService.findSubStatusById(request.statusId());
        PaymentMethod paymentMethod = paymentMethodService.findPaymentMethodById(request.paymentMethodId());
        existing.setPet(pet);
        existing.setPlan(plan);
        existing.setStatus(status);
        existing.setPaymentMethod(paymentMethod);
        existing.setContractedValue(plan.getMonthlyValue());
        return subscriptionRepository.save(existing);
    }

    public void deleteSubscription(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subscription with ID %d not found", id));
        }
        subscriptionRepository.deleteById(id);
    }
}
