package nz.ac.auckland.se281;

public class Country {
  private String name;
  private String continent;
  private Integer fuel;

  public Country (String name, String continent, Integer fuel) {
    this.name = name;
    this.continent = continent;
    this.fuel = fuel;
  }

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
