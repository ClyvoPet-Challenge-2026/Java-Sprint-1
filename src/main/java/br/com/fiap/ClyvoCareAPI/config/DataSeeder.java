package br.com.fiap.ClyvoCareAPI.config;

import br.com.fiap.ClyvoCareAPI.entity.*;
import br.com.fiap.ClyvoCareAPI.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final PlanRepository planRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubStatusRepository subStatusRepository;

    @Override
    public void run(String... args) {
        seedStatesAndCities();
        seedSpeciesAndBreeds();
        seedPlans();
        seedPaymentMethods();
        seedSubStatus();
    }

    private void seedStatesAndCities() {
        if (stateRepository.count() == 0) {
            log.info("Populando Estados e Cidades...");
            State sp = stateRepository.save(State.builder().uf("SP").name("São Paulo").build());
            State rj = stateRepository.save(State.builder().uf("RJ").name("Rio de Janeiro").build());
            State mg = stateRepository.save(State.builder().uf("MG").name("Minas Gerais").build());
            State pr = stateRepository.save(State.builder().uf("PR").name("Paraná").build());
            State rs = stateRepository.save(State.builder().uf("RS").name("Rio Grande do Sul").build());
            State ba = stateRepository.save(State.builder().uf("BA").name("Bahia").build());
            State sc = stateRepository.save(State.builder().uf("SC").name("Santa Catarina").build());
            State ce = stateRepository.save(State.builder().uf("CE").name("Ceará").build());

            cityRepository.saveAll(List.of(
                    City.builder().state(sp).name("São Paulo").build(),
                    City.builder().state(sp).name("São José dos Campos").build(),
                    City.builder().state(sp).name("Campinas").build(),
                    City.builder().state(sp).name("Santos").build(),
                    City.builder().state(rj).name("Rio de Janeiro").build(),
                    City.builder().state(rj).name("Niterói").build(),
                    City.builder().state(mg).name("Belo Horizonte").build(),
                    City.builder().state(pr).name("Curitiba").build(),
                    City.builder().state(rs).name("Porto Alegre").build(),
                    City.builder().state(ba).name("Salvador").build(),
                    City.builder().state(sc).name("Florianópolis").build(),
                    City.builder().state(ce).name("Fortaleza").build()
            ));
            log.info("Estados e Cidades populados com sucesso!");
        }
    }

    private void seedSpeciesAndBreeds() {
        if (speciesRepository.count() == 0) {
            log.info("Populando Espécies e Raças...");
            Species dog = speciesRepository.save(Species.builder().name("Canino").build());
            Species cat = speciesRepository.save(Species.builder().name("Felino").build());
            Species bird = speciesRepository.save(Species.builder().name("Ave").build());
            Species rodent = speciesRepository.save(Species.builder().name("Roedor").build());

            breedRepository.saveAll(List.of(
                    Breed.builder().species(dog).name("Golden Retriever").build(),
                    Breed.builder().species(dog).name("Labrador").build(),
                    Breed.builder().species(dog).name("Bulldog").build(),
                    Breed.builder().species(dog).name("Poodle").build(),
                    Breed.builder().species(dog).name("SRD (Vira-lata)").build(),
                    Breed.builder().species(cat).name("Siamês").build(),
                    Breed.builder().species(cat).name("Persa").build(),
                    Breed.builder().species(cat).name("Maine Coon").build(),
                    Breed.builder().species(cat).name("SRD (Vira-lata)").build(),
                    Breed.builder().species(bird).name("Calopsita").build(),
                    Breed.builder().species(bird).name("Periquito").build(),
                    Breed.builder().species(rodent).name("Hamster").build()
            ));
            log.info("Espécies e Raças populadas com sucesso!");
        }
    }

    private void seedPlans() {
        if (planRepository.count() == 0) {
            log.info("Populando Planos...");
            planRepository.saveAll(List.of(
                    Plan.builder().name("Clyvo Essencial").monthlyValue(BigDecimal.valueOf(49.90)).build(),
                    Plan.builder().name("Clyvo Conforto").monthlyValue(BigDecimal.valueOf(89.90)).build(),
                    Plan.builder().name("Clyvo Plus").monthlyValue(BigDecimal.valueOf(139.90)).build(),
                    Plan.builder().name("Clyvo Premium").monthlyValue(BigDecimal.valueOf(199.90)).build(),
                    Plan.builder().name("Clyvo Sênior").monthlyValue(BigDecimal.valueOf(239.90)).build()
            ));
            log.info("Planos populados com sucesso!");
        }
    }

    private void seedPaymentMethods() {
        if (paymentMethodRepository.count() == 0) {
            log.info("Populando Formas de Pagamento...");
            paymentMethodRepository.saveAll(List.of(
                    PaymentMethod.builder().name("Pix").build(),
                    PaymentMethod.builder().name("Cartão de Crédito").build(),
                    PaymentMethod.builder().name("Boleto Bancário").build(),
                    PaymentMethod.builder().name("Débito Automático").build()
            ));
            log.info("Formas de Pagamento populadas!");
        }
    }

    private void seedSubStatus() {
        if (subStatusRepository.count() == 0) {
            log.info("Populando Status de Contratação...");
            subStatusRepository.saveAll(List.of(
                    SubStatus.builder().name("ATIVO").build(),
                    SubStatus.builder().name("CANCELADO").build(),
                    SubStatus.builder().name("SUSPENSO").build(),
                    SubStatus.builder().name("PENDENTE").build()
            ));
            log.info("Status de Contratação populados!");
        }
    }
}
