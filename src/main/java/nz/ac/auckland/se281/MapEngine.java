package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    // Reads the countries.csv which is first read into a list
    // and then each country object is initialized and added to the
    // countriesRefMap HashMap.
    List<String> countries = Utils.readCountries();
    for (String country : countries) {
      // "name,continent,fuel"
      String[] countryDetails = country.split(",");
      Country tempRootCountry =
          new Country(countryDetails[0], countryDetails[1], Integer.valueOf(countryDetails[2]));
      countriesRefMap.put(countryDetails[0], tempRootCountry);
    }

    // Reads the adjacencies.csv into a list
    List<String> adjacencies = Utils.readAdjacencies();
    for (String individualAdj : adjacencies) {
      // holds the country objects for adjacent countries where the first country is the root
      // country and the rest are the adjacent countries.
      List<Country> localAdjCountriesList = new ArrayList<>();
      // "countryRef,adjacentCountryRef1,adjacentCountryRef2,..."
      String[] individualAdjArray = individualAdj.split(",");
      for (String countryRef : individualAdjArray) {
        // assuming the country refrences are from adjacencies.csv
        // are valid and exist in the countriesRefMap.
        localAdjCountriesList.add(countriesRefMap.get(countryRef));
      }
      // the first country in the list is the root country
      Country tempRootCountry = localAdjCountriesList.get(0);
      localAdjCountriesList.remove(0);
      // add the root country and its adjacent countries to the riskMap graph.
      riskMap.addNodes(tempRootCountry, localAdjCountriesList);
    }
  }

  /** this method is invoked when the user run the command info-country. */
  public void showInfoCountry() {
    // initiate the chosen country object
    Country chosenCountry = countryInputLoop();
    // Prints country info based on the project brief
    // "%s => continent: %s, fuel cost: %s, neighbours: %s"
    MessageCli.COUNTRY_INFO.printMessage(
        chosenCountry.getName(),
        chosenCountry.getContinent(),
        chosenCountry.getFuel().toString(),
        riskMap.getNeighbours(chosenCountry).toString());
  }

  /** this method is invoked when the user run the command route. */
  public void showRoute() {
    // Start and destination countries are defines based on the user input.
    Country startCountry = countryInputLoop(); // First valid input on the input stream
    Country endCountry = countryInputLoop(); // Next valid input on the input stream

    if (startCountry != endCountry) {
      Integer fuelReq = 0;
      Integer highestFuelContinentCounter = 0;
      // this initialization helps avoid problems when there is no transit continent
      // and the first come first picked continent is the start country's continent.
      String highestFuelContinent =
          startCountry.getContinent(); // initializes with start country's 0 fuel consumption
      HashMap<String, Integer> transitContinents = new HashMap<>();
      Set<String> orderedVisitedContinents = new LinkedHashSet<>();

      List<String> fastestRouteStringList = new ArrayList<>();
      // BFS algorithm to find the fastest route between start and end country.
      List<Country> fastestRoute = riskMap.getFastestRouteBFS(startCountry, endCountry);

      for (Country onRouteCountry : fastestRoute) {
        // adds to the string list/set to be used for printing later.
        fastestRouteStringList.add(onRouteCountry.getName());
        orderedVisitedContinents.add(onRouteCountry.getContinent());
        // for only transit countries
        if (onRouteCountry != startCountry && onRouteCountry != endCountry) {
          // increments fuel requirement by the fuel cost of the transit country
          fuelReq += onRouteCountry.getFuel();
          // puts the continent and its fuel consumption in the map
          // if the continent is already in the map, it adds the fuel consumption to the existing
          // value.
          transitContinents.put(
              // not assigning temp values to be memory efficient.
              onRouteCountry.getContinent(),
              transitContinents.getOrDefault(onRouteCountry.getContinent(), 0)
                  + onRouteCountry.getFuel());
          if (transitContinents.get(onRouteCountry.getContinent()) > highestFuelContinentCounter) {
            highestFuelContinent = onRouteCountry.getContinent();
            highestFuelContinentCounter = transitContinents.get(onRouteCountry.getContinent());
          }
        } else {
          // for start and end countries, we only add the continent with 0 fuel consumption
          transitContinents.put(
              onRouteCountry.getContinent(),
              transitContinents.getOrDefault(onRouteCountry.getContinent(), 0));
        }
      }

      // for visited continents, we create a string list as per the project brief
      List<String> transitContinentsString = new ArrayList<>();
      for (String visitedContinet : orderedVisitedContinents) {
        // "ContinentName (fuel consumption)"
        String continentInfo =
            visitedContinet + " (" + transitContinents.get(visitedContinet) + ")";
        transitContinentsString.add(continentInfo);

        // if the visited continent is the one with the highest fuel consumption,
        // we assign it to the highestFuelContinent to fit the format as per brief.
        // "HighestfuelContinentName (fuel consumption)"
        if (visitedContinet.equals(highestFuelContinent)) {
          highestFuelContinent = continentInfo;
        }
      }

      // Prints the route information based on the project brief
      // "The fastest route is: %s"
      MessageCli.ROUTE_INFO.printMessage(fastestRouteStringList.toString());
      // "You will spend this amount of fuel for your journey: %s units"
      MessageCli.FUEL_INFO.printMessage(fuelReq.toString());
      // "You will visit the following continents: %s"
      MessageCli.CONTINENT_INFO.printMessage(transitContinentsString.toString());
      // "The continent where you will spend the most fuel is: %s"
      MessageCli.FUEL_CONTINENT_INFO.printMessage(highestFuelContinent);
    } else {
      MessageCli.NO_CROSSBORDER_TRAVEL.printMessage();
    }
  }

  // New methods;
  private Country countryInputLoop() {
    // loop until a valid country is entered and return the respective Country object.
    while (true) {
      try {
        return readInputCountry(); // either returns a valid country or throws
        // InvalidCountryException
      } catch (InvalidCountryException invalidInput) {
        // prints the error message from the exception
        System.out.println(invalidInput.getMessage());
      }
    }
  }

  private Country readInputCountry() throws InvalidCountryException {
    // scans the user input and checks if it is a valid country.
    String userInput = Utils.scanner.nextLine();
    userInput = Utils.capitalizeFirstLetterOfEachWord(userInput);
    if (countriesRefMap.containsKey(userInput)) {
      return countriesRefMap.get(userInput); // returns the Country object for input country
    } else {
      throw new InvalidCountryException(userInput); // throws an exception if the input is invalid
    }
  }
}
