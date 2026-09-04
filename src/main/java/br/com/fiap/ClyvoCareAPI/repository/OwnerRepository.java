package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<Owner> findByEmail(String email);

    @Query("SELECT o FROM Owner o WHERE " +
            "(:name IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:cpf IS NULL OR o.cpf = :cpf) AND " +
            "(:email IS NULL OR LOWER(o.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    Page<Owner> search(@Param("name") String name,
                       @Param("cpf") String cpf,
                       @Param("email") String email,
                       Pageable pageable);
}
