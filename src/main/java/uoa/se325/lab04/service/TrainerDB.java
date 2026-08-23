package uoa.se325.lab04.service;

import uoa.se325.lab04.model.pokemon.Pokemon;
import uoa.se325.lab04.model.pokemon.Trainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TrainerDB {

    private final List<Trainer> trainers = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public List<Trainer> getTrainers() {
        return Collections.unmodifiableList(trainers);
    }

    public synchronized Trainer addTrainer(Trainer trainer) {
        trainers.add(trainer);
        return trainer;
    }

    public synchronized Trainer addTrainer(String name, int trainerId) {
        return addTrainer(new Trainer(name, trainerId));
    }

    public synchronized Trainer addTrainer(String name, int trainerId, Pokemon... pokemon) {
        return addTrainer(new Trainer(name, trainerId, pokemon));
    }

    public synchronized Optional<Trainer> findTrainer(long id) {
        return trainers.stream().filter(t -> t.getTrainerId() == id).findFirst();
    }

    public synchronized void clear() {
        idCounter.set(0);
        trainers.clear();
    }
}
