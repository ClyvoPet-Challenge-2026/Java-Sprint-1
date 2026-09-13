package br.com.fiap.ClyvoCareAPI.config;

import br.com.fiap.ClyvoCareAPI.entity.*;
import br.com.fiap.ClyvoCareAPI.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("!${spring.flyway.enabled:true}")
public class DataInitializer implements CommandLineRunner {

    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final PlanRepository planRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            initStatesAndCities();
            initSpeciesAndBreeds();
            initPlans();
            initDefaultUsers();
            log.info("ClyvoCare API - Carga inicial de dados concluída com sucesso.");
        } catch (Exception e) {
            log.warn("ClyvoCare API - Carga inicial já executada ou ignorada: {}", e.getMessage());
        }
    }

    private void initStatesAndCities() {
        if (stateRepository.count() == 0) {
            var sp = stateRepository.save(State.builder().uf("SP").name("Sao Paulo").build());
            var rj = stateRepository.save(State.builder().uf("RJ").name("Rio de Janeiro").build());
            var mg = stateRepository.save(State.builder().uf("MG").name("Minas Gerais").build());
            var pr = stateRepository.save(State.builder().uf("PR").name("Parana").build());
            var ce = stateRepository.save(State.builder().uf("CE").name("Ceara").build());
            var rs = stateRepository.save(State.builder().uf("RS").name("Rio Grande do Sul").build());
            var ba = stateRepository.save(State.builder().uf("BA").name("Bahia").build());
            var sc = stateRepository.save(State.builder().uf("SC").name("Santa Catarina").build());

            cityRepository.saveAll(List.of(
                    City.builder().state(sp).name("Sao Paulo").build(),
                    City.builder().state(sp).name("Campinas").build(),
                    City.builder().state(sp).name("Santos").build(),
                    City.builder().state(rj).name("Rio de Janeiro").build(),
                    City.builder().state(rj).name("Niteroi").build(),
                    City.builder().state(mg).name("Belo Horizonte").build(),
                    City.builder().state(pr).name("Curitiba").build(),
                    City.builder().state(ce).name("Fortaleza").build(),
                    City.builder().state(rs).name("Porto Alegre").build(),
                    City.builder().state(ba).name("Salvador").build(),
                    City.builder().state(sc).name("Florianopolis").build()
            ));
        }
    }

    private void initSpeciesAndBreeds() {
        if (speciesRepository.count() == 0) {
            var dog = speciesRepository.save(Species.builder().name("Dog").build());
            var cat = speciesRepository.save(Species.builder().name("Cat").build());
            var bird = speciesRepository.save(Species.builder().name("Bird").build());
            var rabbit = speciesRepository.save(Species.builder().name("Rabbit").build());
            speciesRepository.save(Species.builder().name("Reptile").build());
            speciesRepository.save(Species.builder().name("Hamster").build());

            breedRepository.saveAll(List.of(
                    Breed.builder().species(dog).name("Labrador").build(),
                    Breed.builder().species(dog).name("Golden Retriever").build(),
                    Breed.builder().species(dog).name("Bulldog").build(),
                    Breed.builder().species(dog).name("Poodle").build(),
                    Breed.builder().species(dog).name("Mixed").build(),
                    Breed.builder().species(cat).name("Siamese").build(),
                    Breed.builder().species(cat).name("Persian").build(),
                    Breed.builder().species(cat).name("Maine Coon").build(),
                    Breed.builder().species(bird).name("Cockatiel").build(),
                    Breed.builder().species(rabbit).name("Mini Lop").build()
            ));
        }
    }

    private void initPlans() {
        if (planRepository.count() == 0) {
            planRepository.saveAll(List.of(
                    Plan.builder().name("Essential").monthlyValue(new BigDecimal("49.90")).build(),
                    Plan.builder().name("Basic").monthlyValue(new BigDecimal("69.90")).build(),
                    Plan.builder().name("Premium").monthlyValue(new BigDecimal("99.90")).build(),
                    Plan.builder().name("Master").monthlyValue(new BigDecimal("139.90")).build(),
                    Plan.builder().name("Total").monthlyValue(new BigDecimal("179.90")).build(),
                    Plan.builder().name("Corporate").monthlyValue(new BigDecimal("249.90")).build()
            ));
        }
    }

    private void initDefaultUsers() {
        var defaultCity = cityRepository.findAll().stream().findFirst().orElse(null);
        if (defaultCity == null) {
            return;
        }
        createOwnerIfMissing("ana@email.com", "Ana Paula Souza", "111.111.111-11",
                "(11) 91111-1111", "ADMIN", defaultCity);
        createOwnerIfMissing("carlos@email.com", "Carlos Henrique Lima", "222.222.222-22",
                "(11) 92222-2222", "OWNER", defaultCity);
    }

    private void createOwnerIfMissing(String email, String name, String cpf, String phone,
                                     String roleName, City city) {
        if (ownerRepository.existsByEmail(email)) {
            return;
        }
        ownerRepository.save(Owner.builder()
                .name(name)
                .cpf(cpf)
                .email(email)
                .passwordHash(passwordEncoder.encode("senha123"))
                .roleName(roleName)
                .enabled(true)
                .phone(phone)
                .city(city)
                .build()
        );
    }
}
