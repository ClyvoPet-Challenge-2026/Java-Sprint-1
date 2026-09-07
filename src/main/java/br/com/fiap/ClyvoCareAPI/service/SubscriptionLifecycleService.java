package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class SubscriptionLifecycleService {

    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> TRANSITIONS = Map.of(
            SubscriptionStatus.ACTIVE, Set.of(SubscriptionStatus.PENDING, SubscriptionStatus.INACTIVE),
            SubscriptionStatus.PENDING, Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.INACTIVE),
            SubscriptionStatus.INACTIVE, Set.of()
    );

    public void validateTransition(SubscriptionStatus currentStatus, SubscriptionStatus nextStatus) {
        if (!TRANSITIONS.get(currentStatus).contains(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Cannot change subscription status from %s to %s", currentStatus, nextStatus)
            );
        }
    }

    public void validatePlanChange(SubscriptionStatus status) {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only ACTIVE or PENDING subscriptions can change plan or payment method"
            );
        }
    }
}
