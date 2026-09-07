package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Pet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pet p WHERE p.id = :id")
    Optional<Pet> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Pet p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:ownerId IS NULL OR p.owner.id = :ownerId) AND " +
            "(:speciesId IS NULL OR p.species.id = :speciesId) AND " +
            "(:breedId IS NULL OR p.breed.id = :breedId)")
    Page<Pet> search(
            @Param("name") String name,
            @Param("ownerId") Long ownerId,
            @Param("speciesId") Long speciesId,
            @Param("breedId") Long breedId,
            Pageable pageable
    );
}
