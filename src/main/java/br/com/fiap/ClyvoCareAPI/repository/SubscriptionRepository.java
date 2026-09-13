package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import br.com.fiap.ClyvoCareAPI.entity.Subscription;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.pet.owner.email = :email AND s.status = :status")
    Page<Subscription> findByOwnerEmailAndStatus(
            @Param("email") String email,
            @Param("status") SubscriptionStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPet_IdAndStatus(Long petId, SubscriptionStatus status);

    boolean existsByPet_IdAndStatusAndIdNot(
            Long petId,
            SubscriptionStatus status,
            Long id
    );

    @Query("SELECT s FROM Subscription s WHERE " +
            "(:petId IS NULL OR s.pet.id = :petId) AND " +
            "(:planId IS NULL OR s.plan.id = :planId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)")
    Page<Subscription> search(
            @Param("petId") Long petId,
            @Param("planId") Long planId,
            @Param("status") SubscriptionStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable
    );
}
