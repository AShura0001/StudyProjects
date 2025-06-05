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
  public void showInfoCountry() {
    Country chosenCountry = verifiedCountry();
    // "%s => continent: %s, fuel cost: %s, neighbours: %s"
    MessageCli.COUNTRY_INFO.printMessage(
        chosenCountry.getName(),
        chosenCountry.getContinent(),
        chosenCountry.getFuel().toString(),
        riskMap.getNeighbours(chosenCountry).toString());
  }

  /** this method is invoked when the user run the command route. */
  public void showRoute() {
    Country startCountry = verifiedCountry(); // First valid input on the input stream
    Country endCountry = verifiedCountry(); // Next valid input on the input stream

    if (startCountry != endCountry) {
      Integer fuelReq = 0;
      Integer highestFuelContinentCounter = 0;
      String highestFuelContinent =
          startCountry.getContinent(); // initializes with start country's 0 fuel consumption
      HashMap<String, Integer> transitContinents = new HashMap<>();
      Set<String> orderedVisitedContinents = new LinkedHashSet<>();

      List<String> fastestRouteStringList = new ArrayList<>();
      List<Country> fastestRoute = riskMap.getFastestRouteBFS(startCountry, endCountry);

      for (Country onRouteCountry : fastestRoute) {
        fastestRouteStringList.add(onRouteCountry.getName());
        orderedVisitedContinents.add(onRouteCountry.getContinent());
        if (onRouteCountry != startCountry && onRouteCountry != endCountry) {
          fuelReq += onRouteCountry.getFuel();
          // not assigning temp values to be memory efficient.
          transitContinents.put(
              onRouteCountry.getContinent(),
              transitContinents.getOrDefault(onRouteCountry.getContinent(), 0)
                  + onRouteCountry.getFuel());
          if (transitContinents.get(onRouteCountry.getContinent()) > highestFuelContinentCounter) {
            highestFuelContinent = onRouteCountry.getContinent();
            highestFuelContinentCounter = transitContinents.get(onRouteCountry.getContinent());
          }
        } else {
          transitContinents.put(
              onRouteCountry.getContinent(),
              transitContinents.getOrDefault(onRouteCountry.getContinent(), 0));
        }
      }

      List<String> transitContinentsString = new ArrayList<>();
      for (String visitedContinet : orderedVisitedContinents) {
        String continentInfo =
            visitedContinet + " (" + transitContinents.get(visitedContinet) + ")";
        transitContinentsString.add(continentInfo);
        if (visitedContinet.equals(highestFuelContinent)) {
          highestFuelContinent = continentInfo;
        }
      }

      // for (Country onRouteCountry : fastestRoute) {
      //   fastestRouteStringList.add(onRouteCountry.getName());
      // }

      // for (Integer transitIndex = 1; transitIndex < fastestRoute.size() - 1; transitIndex++) {
      //   Country transitCountry = fastestRoute.get(transitIndex);
      //   fuelReq += transitCountry.getFuel();
      //   // not assigning temp values to be memory efficient.
      //   transitContinents.put(
      //       transitCountry.getContinent(),
      //       transitContinents.getOrDefault(transitCountry.getContinent(), 0)
      //           + transitCountry.getFuel());
      //   if (transitContinents.get(transitCountry.getContinent())> highestFuelContinentCounter){
      //     highestFuelContinent = transitCountry.getContinent();
      //     highestFuelContinentCounter = transitContinents.get(transitCountry.getContinent());
      //   }
      // }

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
  private Country verifiedCountry() {
    while (true) {
      try {
        return readInputCountry();
      } catch (InvalidCountryException invalidInput) {
        System.out.println(invalidInput.getMessage());
      }
    }
  }

  private Country readInputCountry() throws InvalidCountryException {
    String userInput = Utils.scanner.nextLine();
    userInput = Utils.capitalizeFirstLetterOfEachWord(userInput);
    if (countriesRefMap.containsKey(userInput)) {
      return countriesRefMap.get(userInput);
    } else {
      throw new InvalidCountryException(userInput);
    }
  }
}
