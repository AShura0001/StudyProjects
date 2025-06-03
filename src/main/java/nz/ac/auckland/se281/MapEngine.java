package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** This class is the main entry point. */
public class MapEngine {
  Graph riskMap = new Graph();

  public MapEngine() {
    // add other code here if you wan
    loadMap(); // keep this mehtod invocation
  }

  /** invoked one time only when constracting the MapEngine class. */
  private void loadMap() {

    List<String> countries = Utils.readCountries();
    // for (String country : countries) {
    //   String[] countryDetails = country.split(",");
    //   Country tempRootCountry =
    //       new Country(countryDetails[0], countryDetails[1], Integer.valueOf(countryDetails[2]));
    //   riskMap.addNodes(tempRootCountry, null);
    // }
    List<String> adjacencies = Utils.readAdjacencies();
    for (String country : countries) {
      String[] countryDetails = country.split(",");
      Country tempRootCountry =
          new Country(countryDetails[0], countryDetails[1], Integer.valueOf(countryDetails[2]));
      for (String individualAdj : adjacencies) {
        List<String> individualAdjList = Arrays.asList(individualAdj.split(","));
        if (individualAdjList.get(0).equals(tempRootCountry.getName())) {
          List<Country> adjCountries = new ArrayList<>();
          for (int countryIndex = 1; countryIndex < individualAdjList.size(); countryIndex++) {
            for (String adjcountry : countries) {}
          }
        }
      }
    }
  }

  /** this method is invoked when the user run the command info-country. */
  public void showInfoCountry() {}

  /** this method is invoked when the user run the command route. */
  public void showRoute() {}
}
