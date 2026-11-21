package uoa.se325.lab04.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uoa.se325.lab04.model.pokemon.Pokemon;
import uoa.se325.lab04.model.pokemon.Trainer;
import uoa.se325.lab04.repository.TrainerRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TrainerControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TrainerRepository trainerRepository;

    @BeforeEach
    public void setUp() {
        trainerRepository.deleteAll();

        trainerRepository.save(new Trainer(
                "Ash Ketchum",
                1,
                new Pokemon("Pikachu", "Sparky", 100),
                new Pokemon("Charizard", "Fireball", 76)
        ));

        trainerRepository.save(new Trainer(
                "Andrew",
                2,
                new Pokemon("Dragonite", "Yoshi", 255)
        ));
    }

    @Test
    public void shouldGetAllTrainers() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/trainers")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ash Ketchum"))
                .andExpect(jsonPath("$[0].trainerId").value(1))
                .andExpect(jsonPath("$[0].pokemonList[0].speciesName").value("Pikachu"))
                .andExpect(jsonPath("$[0].pokemonList[0].nickname").value("Sparky"))
                .andExpect(jsonPath("$[0].pokemonList[0].level").value(100))
                .andExpect(jsonPath("$[1].name").value("Andrew"))
                .andExpect(jsonPath("$[1].trainerId").value(2))
                .andExpect(jsonPath("$[1].pokemonList[0].speciesName").value("Dragonite"))
                .andExpect(jsonPath("$[1].pokemonList[0].nickname").value("Yoshi"))
                .andExpect(jsonPath("$[1].pokemonList[0].level").value(255));
    }

    @Test
    public void shouldGetSingleTrainer() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/trainers/1")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ash Ketchum"))
                .andExpect(jsonPath("$.trainerId").value(1))
                .andExpect(jsonPath("$.pokemonList[0].speciesName").value("Pikachu"))
                .andExpect(jsonPath("$.pokemonList[0].nickname").value("Sparky"))
                .andExpect(jsonPath("$.pokemonList[0].level").value(100))
                .andExpect(jsonPath("$.pokemonList[1].speciesName").value("Charizard"))
                .andExpect(jsonPath("$.pokemonList[1].nickname").value("Fireball"))
                .andExpect(jsonPath("$.pokemonList[1].level").value(76));
    }

    @Test
    public void shouldGetSingleTrainerNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/trainers/42")
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldGetTrainerPokemon() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/trainers/1/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].speciesName").value("Pikachu"))
                .andExpect(jsonPath("$[0].nickname").value("Sparky"))
                .andExpect(jsonPath("$[0].level").value(100))
                .andExpect(jsonPath("$[1].speciesName").value("Charizard"))
                .andExpect(jsonPath("$[1].nickname").value("Fireball"))
                .andExpect(jsonPath("$[1].level").value(76));
    }

    @Test
    public void shouldGetTrainerPokemonNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/trainers/99/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }


    @Test
    public void shouldDeleteTrainerPokemon() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/trainers/1/pokemon/0")
                        .contentType("application/json"))
                .andExpect(status().isNoContent());

        mvc.perform(MockMvcRequestBuilders.get("/trainers/1/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].speciesName").value("Charizard"))
                .andExpect(jsonPath("$[0].nickname").value("Fireball"))
                .andExpect(jsonPath("$[0].level").value(76));
    }

    @Test
    public void shouldDeleteTrainerPokemonOutOfBounds() throws Exception {
        // Attempt to delete a Pokémon with an out-of-bounds index for Andrew
        mvc.perform(MockMvcRequestBuilders.delete("/trainers/2/pokemon/99")
                        .contentType("application/json"))
                .andExpect(status().isNoContent());

        // Verify that Andrew's Pokémon list remains unmodified
        mvc.perform(MockMvcRequestBuilders.get("/trainers/2/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].speciesName").value("Dragonite"))
                .andExpect(jsonPath("$[0].nickname").value("Yoshi"))
                .andExpect(jsonPath("$[0].level").value(255));
    }

    @Test
    public void shouldDeleteTrainerPokemonNotFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/trainers/99/pokemon/0")
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldAddPokemonToTrainer() throws Exception {
        String newPokemonJson = """
                    {
                        "speciesName": "Bulbasaur",
                        "nickname": "Leafy",
                        "level": 50
                    }
                """;

        // Perform POST request to add a new Pokémon to trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.post("/trainers/1/pokemon")
                        .contentType("application/json")
                        .content(newPokemonJson))
                .andExpect(status().isCreated());

        // Verify that the new Pokémon was added
        mvc.perform(MockMvcRequestBuilders.get("/trainers/1/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].speciesName").value("Bulbasaur"))
                .andExpect(jsonPath("$[2].nickname").value("Leafy"))
                .andExpect(jsonPath("$[2].level").value(50));
    }

    @Test
    public void shouldAddPokemonToTrainerNotFound() throws Exception {
        String newPokemonJson = """
                    {
                        "speciesName": "Bulbasaur",
                        "nickname": "Leafy",
                        "level": 50
                    }
                """;

        // Perform POST request to add a new Pokémon to trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.post("/trainers/99/pokemon")
                        .contentType("application/json")
                        .content(newPokemonJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldModifyPokemonNickname() throws Exception {
        String nicknameUpdateJson = """
                    {
                        "nickname": "Thunder"
                    }
                """;

        // Perform PATCH request to update the nickname of the first Pokémon of trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.patch("/trainers/1/pokemon/0/nickname")
                        .contentType("application/json")
                        .content(nicknameUpdateJson))
                .andExpect(status().isNoContent());

        // Verify that the nickname of the Pokémon was updated
        mvc.perform(MockMvcRequestBuilders.get("/trainers/1/pokemon")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].speciesName").value("Pikachu"))
                .andExpect(jsonPath("$[0].nickname").value("Thunder"))
                .andExpect(jsonPath("$[0].level").value(100));
    }

    @Test
    public void shouldModifyPokemonNicknameNoTrainer() throws Exception {
        String nicknameUpdateJson = """
                    {
                        "nickname": "Thunder"
                    }
                """;

        // Perform PATCH request to update the nickname of the first Pokémon of trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.patch("/trainers/99/pokemon/0/nickname")
                        .contentType("application/json")
                        .content(nicknameUpdateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldModifyPokemonNicknameNoPokemon() throws Exception {
        String nicknameUpdateJson = """
                    {
                        "nickname": "Thunder"
                    }
                """;

        // Perform PATCH request to update the nickname of the first Pokémon of trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.patch("/trainers/1/pokemon/99/nickname")
                        .contentType("application/json")
                        .content(nicknameUpdateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldModifyPokemonNicknameInvalidJSON() throws Exception {
        String nicknameUpdateJson = "{}";

        // Perform PATCH request to update the nickname of the first Pokémon of trainer with ID 1
        mvc.perform(MockMvcRequestBuilders.patch("/trainers/1/pokemon/0/nickname")
                        .contentType("application/json")
                        .content(nicknameUpdateJson))
                .andExpect(status().isUnprocessableEntity());
    }

}
