package nz.ac.auckland.se281;

import java.util.Arrays;
import java.util.List;

/** This class is the main entry point. */
public class MapEngine {

  public MapEngine() {
    // add other code here if you wan
    loadMap(); // keep this mehtod invocation
  }

  /** invoked one time only when constracting the MapEngine class. */
  private void loadMap() {

    List<String> countries = Utils.readCountries();
    List<String> adjacencies = Utils.readAdjacencies();
    for (String country : countries) {
      String[] countryDetails = country.split(",");
      Country tempCountry =
          new Country(countryDetails[0], countryDetails[1], Integer.valueOf(countryDetails[2]));
      for (String individualAdj : adjacencies) {
        if (Arrays.asList(individualAdj.split(",")).contains(tempCountry.getName())) {}
      }
    }
  }

  /** this method is invoked when the user run the command info-country. */
  public void showInfoCountry() {}

  /** this method is invoked when the user run the command route. */
  public void showRoute() {}
}
