package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.ChangePlanRequest;
import br.com.fiap.ClyvoCareAPI.dto.ChangeStatusRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionRequest;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionSimulationRequest;
import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.entity.Pet;
import br.com.fiap.ClyvoCareAPI.entity.Plan;
import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final PetService petService;
    private final PlanService planService;
    private final ContractPricingService contractPricingService;
    private final SubscriptionLifecycleService subscriptionLifecycleService;

    @Transactional(readOnly = true)
    public Page<Subscription> findActiveSubscriptionsByOwnerEmail(String email, Pageable pageable) {
        return subscriptionRepository.findByOwnerEmailAndStatus(email, SubscriptionStatus.ACTIVE, pageable);
    }

    public Page<Subscription> searchSubscriptions(
            Long petId,
            Long planId,
            SubscriptionStatus status,
            PaymentMethod paymentMethod,
            Pageable pageable
    ) {
        return subscriptionRepository.search(
                petId, planId, status, paymentMethod, pageable
        );
    }

    public Subscription findSubscriptionById(Long id) {
        return subscriptionRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Subscription with ID %d not found", id)
                )
        );
    }

    @Transactional(readOnly = true)
    public ContractPricingService.PriceCalculation simulateSubscription(
            SubscriptionSimulationRequest request
    ) {
        Plan plan = planService.findPlanById(request.planId());

        return contractPricingService.calculate(
                plan.getMonthlyValue(),
                request.paymentMethod()
        );
    }

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request) {
        Plan plan = planService.findPlanById(request.planId());
        Pet pet = petService.findPetByIdForUpdate(request.petId());

        validateActiveSubscription(pet.getId(), null);

        var calculation = contractPricingService.calculate(
                plan.getMonthlyValue(),
                request.paymentMethod()
        );

        Subscription subscription = request.toEntity(
                pet,
                plan,
                SubscriptionStatus.ACTIVE,
                calculation.finalValue()
        );

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription updateSubscription(Long id, SubscriptionRequest request) {
        Subscription existing = findSubscriptionByIdForUpdate(id);
        subscriptionLifecycleService.validatePlanChange(existing.getStatus());

        Plan plan = planService.findPlanById(request.planId());
        Pet pet = petService.findPetByIdForUpdate(request.petId());

        if (existing.getStatus() == SubscriptionStatus.ACTIVE) {
            validateActiveSubscription(pet.getId(), existing.getId());
        }

        existing.setPet(pet);
        applyPlanAndPayment(existing, plan, request.paymentMethod());

        return subscriptionRepository.save(existing);
    }

    @Transactional
    public Subscription changeStatus(Long id, ChangeStatusRequest request) {
        Subscription existing = findSubscriptionByIdForUpdate(id);
        subscriptionLifecycleService.validateTransition(existing.getStatus(), request.status());

        if (request.status() == SubscriptionStatus.ACTIVE) {
            Pet pet = petService.findPetByIdForUpdate(existing.getPet().getId());
            validateActiveSubscription(pet.getId(), existing.getId());
        }

        existing.setStatus(request.status());
        return subscriptionRepository.save(existing);
    }

    @Transactional
    public Subscription changePlan(Long id, ChangePlanRequest request) {
        Subscription existing = findSubscriptionByIdForUpdate(id);
        subscriptionLifecycleService.validatePlanChange(existing.getStatus());

        Plan plan = planService.findPlanById(request.planId());
        PaymentMethod paymentMethod = request.paymentMethod() != null
                ? request.paymentMethod()
                : existing.getPaymentMethod();

        applyPlanAndPayment(existing, plan, paymentMethod);
        return subscriptionRepository.save(existing);
    }

    private Subscription findSubscriptionByIdForUpdate(Long id) {
        return subscriptionRepository.findByIdForUpdate(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Subscription with ID %d not found", id)
                )
        );
    }

    private void applyPlanAndPayment(Subscription subscription, Plan plan, PaymentMethod paymentMethod) {
        var calculation = contractPricingService.calculate(plan.getMonthlyValue(), paymentMethod);
        subscription.setPlan(plan);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setContractedValue(calculation.finalValue());
    }

    private void validateActiveSubscription(Long petId, Long excludedId) {
        boolean alreadyActive = excludedId == null
                ? subscriptionRepository.existsByPet_IdAndStatus(
                        petId, SubscriptionStatus.ACTIVE
                )
                : subscriptionRepository.existsByPet_IdAndStatusAndIdNot(
                        petId, SubscriptionStatus.ACTIVE, excludedId
                );

        if (alreadyActive) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Pet already has an active subscription"
            );
        }
    }

    public void deleteSubscription(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Subscription with ID %d not found", id)
            );
        }

        subscriptionRepository.deleteById(id);
    }
}
