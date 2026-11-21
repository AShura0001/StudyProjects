package nz.ac.auckland.se281;

public class Country {
  // A class to store country information as a single object.
  private String name;
  private String continent;
  private Integer fuel;

  public Country(String name, String continent, Integer fuel) {
    // Constructor to initialize the country object with its name, continent, and fuel cost.
    this.name = name;
    this.continent = continent;
    this.fuel = fuel;
  }

  // Getters for the country's informations.
  public String getName() {
    return name;
  }

  public String getContinent() {
    return continent;
  }

  public Integer getFuel() {
    return fuel;
  }
}
