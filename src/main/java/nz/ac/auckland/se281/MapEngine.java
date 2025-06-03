package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** This class is the main entry point. */
public class MapEngine {
  private Graph riskMap = new Graph();
  private HashMap<String, Country> countriesRefMap = new HashMap<>();

  public MapEngine() {
    // add other code here if you wan
    loadMap(); // keep this mehtod invocation
  }

  /** invoked one time only when constracting the MapEngine class. */
  private void loadMap() {

    List<String> countries = Utils.readCountries();
    for (String country : countries) {
      String[] countryDetails = country.split(",");
      Country tempRootCountry =
          new Country(countryDetails[0], countryDetails[1], Integer.valueOf(countryDetails[2]));
      countriesRefMap.put(countryDetails[0], tempRootCountry);
    }

    List<String> adjacencies = Utils.readAdjacencies();
    for (String individualAdj : adjacencies) {
      List<Country> localAdjCountriesList = new ArrayList<>();
      String[] individualAdjArray = individualAdj.split(",");
      for (String countryRef : individualAdjArray) {
        localAdjCountriesList.add(countriesRefMap.get(countryRef));
      }
      Country tempRootCountry = localAdjCountriesList.get(0);
      localAdjCountriesList.remove(0);
      riskMap.addNodes(tempRootCountry, localAdjCountriesList);
    }
  }

  /** this method is invoked when the user run the command info-country. */
  Boolean validInputflag = false;

  public void showInfoCountry() {
    while (!validInputflag) {
      try {
        Country chosenCountry = readInputCountry();
        // "%s => continent: %s, fuel cost: %s, neighbours: %s"
        MessageCli.COUNTRY_INFO.printMessage(
            chosenCountry.getName(),
            chosenCountry.getContinent(),
            chosenCountry.getFuel().toString(),
            riskMap.getNeighbours(chosenCountry).toString());
        validInputflag = true;
      } catch (InvalidCountryException invalidInput) {
        invalidInput.getMessage();
      }
    }
  }

  /** this method is invoked when the user run the command route. */
  public void showRoute() {}

  public Country readInputCountry() throws InvalidCountryException {
    String userInput = Utils.scanner.nextLine();
    userInput = Utils.capitalizeFirstLetterOfEachWord(userInput);
    if (countriesRefMap.containsKey(userInput)) {
      return countriesRefMap.get(userInput);
    } else {
      throw new InvalidCountryException();
    }
  }
}
