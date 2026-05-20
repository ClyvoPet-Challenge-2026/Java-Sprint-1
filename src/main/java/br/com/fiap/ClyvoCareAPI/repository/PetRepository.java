package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Query("SELECT p FROM Pet p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:ownerId IS NULL OR p.owner.id = :ownerId) AND " +
            "(:speciesId IS NULL OR p.species.id = :speciesId) AND " +
            "(:breedId IS NULL OR p.breed.id = :breedId)")
    Page<Pet> search(@Param("name") String name,
                     @Param("ownerId") Long ownerId,
                     @Param("speciesId") Long speciesId,
                     @Param("breedId") Long breedId,
                     Pageable pageable);
}
