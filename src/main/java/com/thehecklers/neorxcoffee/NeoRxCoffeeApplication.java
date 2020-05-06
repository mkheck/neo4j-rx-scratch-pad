package com.thehecklers.neorxcoffee;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.*;

import static org.neo4j.springframework.data.core.schema.Relationship.Direction.INCOMING;
import static org.neo4j.springframework.data.core.schema.Relationship.Direction.OUTGOING;

@SpringBootApplication
public class NeoRxCoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeoRxCoffeeApplication.class, args);
    }

}

@Component
@AllArgsConstructor
class DataLoader {
    private final CoffeeDrinkRepo cdRepo;
    private final CoffeeShopRepo csRepo;

//    public DataLoader(CoffeeDrinkRepo cdRepo, CoffeeShopRepo csRepo) {
//        this.cdRepo = cdRepo;
//        this.csRepo = csRepo;
//    }

    @PostConstruct
    private void load() {
        // Coffee drinks
        CoffeeDrink americano = new CoffeeDrink(UUID.randomUUID().toString(), "Americano");
        CoffeeDrink espresso = new CoffeeDrink(UUID.randomUUID().toString(), "Espresso");
        CoffeeDrink flatWhite = new CoffeeDrink(UUID.randomUUID().toString(), "Flat white");
        CoffeeDrink latte = new CoffeeDrink(UUID.randomUUID().toString(), "Latte");

        cdRepo.deleteAll()
                .thenMany(Flux.just(americano, espresso, flatWhite, latte)
                        .flatMap(cdRepo::save))
                .subscribe(System.out::println);

        // Coffee shops
        CoffeeShop maevas = new CoffeeShop(UUID.randomUUID().toString(), "Maeva's Coffee");
        CoffeeShop germania = new CoffeeShop(UUID.randomUUID().toString(), "Germania Brew Haus");
        CoffeeShop stlbreadco = new CoffeeShop(UUID.randomUUID().toString(), "St. Louis Bread Company");

        csRepo.deleteAll()
                .thenMany(Flux.just(maevas, germania, stlbreadco)
                        .flatMap(csRepo::save))
                .subscribe(System.out::println);

        // Associate!
        maevas.setDrinks(List.of(americano, espresso, flatWhite, latte));
        csRepo.save(maevas);

        germania.setDrinks(List.of(americano, espresso, flatWhite, latte));
        csRepo.save(germania);

        stlbreadco.setDrinks(List.of(americano, espresso, latte));
        csRepo.save(stlbreadco);

        cdRepo.findAll()
                .thenMany(csRepo.findAll())
                .subscribe(System.out::println);
    }
}

@RestController
@RequestMapping("/drinks")
@AllArgsConstructor
class CoffeeDrinkController {
    private final CoffeeDrinkRepo drinkRepo;

    @GetMapping
    Flux<CoffeeDrink> getAll() {
        return drinkRepo.findAll();
    }
}

@RestController
@RequestMapping("/shops")
@AllArgsConstructor
class CoffeeShopController {
    private final CoffeeShopRepo shopRepo;

    @GetMapping
    Flux<CoffeeShop> getAll() {
        return shopRepo.findAll();
    }
}

@Configuration
@EnableReactiveNeo4jRepositories
@EnableTransactionManagement
class MyConfiguration extends AbstractReactiveNeo4jConfig {

    @Bean
    public Driver driver() {
        return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "mkheck"));
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return List.of(CoffeeDrink.class.getPackageName(),
                CoffeeShop.class.getPackageName());
        //return Collections.singletonList(CoffeeDrink.class.getPackage().getName());
    }
}

interface CoffeeDrinkRepo extends ReactiveNeo4jRepository<CoffeeDrink, Long> {
}

interface CoffeeShopRepo extends ReactiveNeo4jRepository<CoffeeShop, Long> {
}


@Node
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@ToString(exclude = "destinations")
class CoffeeDrink {
    @Id
    @GeneratedValue
    private Long neoId;
    @NonNull
    private String id;
    @NonNull
    private String description;

    @JsonIgnoreProperties("drinks")
    @Relationship(type = "OFFERS", direction = INCOMING)
    private Iterable<CoffeeShop> destinations = new ArrayList<>();
}

@Node
@Data
@NoArgsConstructor
@RequiredArgsConstructor
//@ToString(exclude = "drinks")
class CoffeeShop {
    @Id
    @GeneratedValue
    private Long neoId;
    @NonNull
    private String id;
    @NonNull
    private String name;

    @JsonIgnoreProperties("destinations")
    @Relationship(type = "OFFERS", direction = OUTGOING)
    private Iterable<CoffeeDrink> drinks = new ArrayList<>();
}