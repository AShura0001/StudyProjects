package uoa.se325.lab04.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uoa.se325.lab04.dto.pokemon.NicknameRequest;
import uoa.se325.lab04.model.pokemon.Pokemon;
import uoa.se325.lab04.model.pokemon.Trainer;
import uoa.se325.lab04.service.TrainerDB;

import java.util.List;

@RestController
@RequestMapping("/trainers")
public class TrainerController {

    private final TrainerDB trainerDB;

    @Autowired
    public TrainerController(TrainerDB trainerDB) {
        this.trainerDB = trainerDB;
    }

    // 1. Retrieve all trainers
    @GetMapping
    public List<Trainer> getAllTrainers() {
        return trainerDB.getTrainers();
    }

    // 2. Retrieve a trainer by ID
    @GetMapping("/{id}")
    public Trainer getTrainerById(@PathVariable Long id) {
        return trainerDB.findTrainer(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));
    }

    // 3. Retrieve a trainer's Pokémon
    @GetMapping("/{id}/pokemon")
    public List<Pokemon> getTrainerPokemon(@PathVariable Long id) {
        Trainer trainer = getTrainerById(id); // Reuse existing method for validation
        return trainer.getPokemonList();
    }

    // 4. Add a Pokémon to a trainer
    @PostMapping("/{id}/pokemon")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPokemonToTrainer(@PathVariable Long id, @RequestBody Pokemon pokemon) {
        Trainer trainer = getTrainerById(id);
        trainer.getPokemonList().add(pokemon);
    }

    // 5. Delete a Pokémon from a trainer
    @DeleteMapping("/{id}/pokemon/{pokemonIndex}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainerPokemon(@PathVariable Long id, @PathVariable int pokemonIndex) {
        Trainer trainer = getTrainerById(id);

        if (pokemonIndex < 0 || pokemonIndex >= trainer.getPokemonList().size()) {
            return;
        }

        trainer.getPokemonList().remove(pokemonIndex);
    }

    // 6. Modify a Pokémon's nickname
    @PatchMapping("/{id}/pokemon/{pokemonIndex}/nickname")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void modifyPokemonNickname(
            @PathVariable Long id,
            @PathVariable int pokemonIndex,
            @RequestBody NicknameRequest newNicknameRequest) {
        Trainer trainer = getTrainerById(id);

        if (pokemonIndex < 0 || pokemonIndex >= trainer.getPokemonList().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Pokémon index");
        }

        if (newNicknameRequest == null || newNicknameRequest.getNickname() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nickname is required");
        }

        trainer.getPokemonList().get(pokemonIndex).setNickname(newNicknameRequest.getNickname());
    }
}