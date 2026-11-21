package uoa.se325.lab04.model.pokemon;

import jakarta.persistence.Embeddable;

@Embeddable
public class Pokemon {
    
    private String speciesName;
    private String nickname;
    private int level;

    // Default Constructor
    public Pokemon() {
    }

    // Constructor with Parameters
    public Pokemon(String speciesName, String nickname, int level) {
        this.speciesName = speciesName;
        this.nickname = nickname;
        this.level = level;
    }

    // Getter for speciesName
    public String getSpeciesName() {
        return speciesName;
    }

    // Setter for speciesName
    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    // Getter for nickname
    public String getNickname() {
        return nickname;
    }

    // Setter for nickname
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    // Getter for level
    public int getLevel() {
        return level;
    }

    // Setter for level
    public void setLevel(int level) {
        this.level = level;
    }


    @Override
    public String toString() {
        return String.format("%s, the Lv%d %s", nickname, level, speciesName);
    }
}
