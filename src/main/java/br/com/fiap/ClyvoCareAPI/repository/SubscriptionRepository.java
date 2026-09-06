package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE " +
            "(:petId IS NULL OR s.pet.id = :petId) AND " +
            "(:planId IS NULL OR s.plan.id = :planId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)")
    Page<Subscription> search(@Param("petId") Long petId,
                              @Param("planId") Long planId,
                              @Param("status") SubscriptionStatus status,
                              @Param("paymentMethod") PaymentMethod paymentMethod,
                              Pageable pageable);
}
