package uoa.se325.lab04.model.pokemon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trainer {

    private String name;
    private int trainerId;
    private List<Pokemon> pokemonList = new ArrayList<>();

    public Trainer() {
    }

    public Trainer(String name, int trainerId) {
        this.name = name;
        this.trainerId = trainerId;
    }


    public Trainer(String name, int trainerId, Pokemon... pokemon) {
        this.name = name;
        this.trainerId = trainerId;
        Collections.addAll(this.pokemonList, pokemon);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(int trainerId) {
        this.trainerId = trainerId;
    }

    public List<Pokemon> getPokemonList() {
        return pokemonList;
    }

    public void setPokemonList(List<Pokemon> pokemonList) {
        this.pokemonList = pokemonList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(trainerId).append(")").append("\n");
        for (Pokemon pokemon : pokemonList) {
            sb.append("- ").append(pokemon).append("\n");
        }
        return sb.toString();
    }
}
