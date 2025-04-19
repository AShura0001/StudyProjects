package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  private HashMap<String, HashMap<String, String>> locationsMap = new HashMap<>();

  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  private String operatorInitialBuilder(
      String operatorName, Integer operatorIndex, String reqLocationAbb) {
    // Mutable list for operator initials.
    ArrayList<Character> operatorInitialsArraylist = new ArrayList<>();
    String[] words = operatorName.split(" ");
    for (String word : words) {
      operatorInitialsArraylist.add(word.charAt(0));
    }
    // Mutable string converted from list to stringbuilder class.
    StringBuilder operatorInitialsSb = new StringBuilder();
    for (Character ch : operatorInitialsArraylist) {
      operatorInitialsSb.append(ch);
    }
    // String Builder Class turned to String class.
    String operatorInitials = operatorInitialsSb.toString();

    // creating the number for the operator id
    StringBuilder operatorIdNumBuilder = new StringBuilder();
    String operatorIndexString = Integer.toString(operatorIndex);
    if (operatorIndex < 10) {
      operatorIdNumBuilder.append("00");
      operatorIdNumBuilder.append(operatorIndexString);
    } else if (operatorIndex < 100) {
      operatorIdNumBuilder.append("0");
      operatorIdNumBuilder.append(operatorIndexString);
    } else if (operatorIndex < 1000) {
      operatorIdNumBuilder.append(operatorIndexString);
    }
    String operatorIdNumber = operatorIdNumBuilder.toString();

    String operatorId = String.join("-", operatorInitials, reqLocationAbb, operatorIdNumber);
    return operatorId;
  }

  public void searchOperators(String keyword) {
    keyword = keyword.strip();
    if (keyword.equals("*")) {
      Integer operatorCount = 0;
      for (HashMap<String, String> locations : locationsMap.values()) {
        operatorCount += locations.size();
      }

      Integer locationsMapSize = locationsMap.size();
      // System.out.println(locationsMapSize);
      if (locationsMapSize == 0) {
        MessageCli.OPERATORS_FOUND.printMessage("are", "no", "s", ".");
      } else if (operatorCount == 1) {
        String operatorCountString = Integer.toString(operatorCount);
        MessageCli.OPERATORS_FOUND.printMessage("is", operatorCountString, "", ":");

        for (String localLocationsKey : locationsMap.keySet()) {
          HashMap<String, String> localLocation = locationsMap.get(localLocationsKey);

          for (String operatorIdkey : localLocation.keySet()) {
            Location reqLocation = Location.fromString(localLocationsKey);
            String reqLocationFullName = reqLocation.getFullName();

            String operatorName = localLocation.get(operatorIdkey);
            MessageCli.OPERATOR_ENTRY.printMessage(
                operatorName, operatorIdkey, reqLocationFullName);
          }
        }
      } else if (operatorCount > 1) {
        String operatorCountString = Integer.toString(operatorCount);
        MessageCli.OPERATORS_FOUND.printMessage("are", operatorCountString, "s", ":");

        for (String localLocationsKey : locationsMap.keySet()) {
          HashMap<String, String> localLocation = locationsMap.get(localLocationsKey);

          for (String operatorIdkey : localLocation.keySet()) {
            Location reqLocation = Location.fromString(localLocationsKey);
            String reqLocationFullName = reqLocation.getFullName();

            String operatorName = localLocation.get(operatorIdkey);
            MessageCli.OPERATOR_ENTRY.printMessage(
                operatorName, operatorIdkey, reqLocationFullName);
          }
        }
      }
    } else {

      ArrayList<ArrayList<String>> matchingSets = new ArrayList<>();
      Integer matchingCount = 0;

      for (String localLocationsKey : locationsMap.keySet()) {
        HashMap<String, String> localLocation = locationsMap.get(localLocationsKey);
        Location reqLocation = Location.fromString(localLocationsKey);
        String reqLocAbb = reqLocation.getLocationAbbreviation();
        String reqLocFullName = reqLocation.getFullName();
        String tempKeyword = keyword.toUpperCase();
        String tempReqLocAbb = reqLocAbb.toUpperCase();
        String tempReqLocFullName = reqLocFullName.toUpperCase();

        for (String operatorIdkey : localLocation.keySet()) {
          // String tempOperatorIdKey = operatorIdkey.toUpperCase();
          String operatorName = localLocation.get(operatorIdkey);
          String tempOperatorName = operatorName.toUpperCase();
          if (tempOperatorName.contains(tempKeyword)
              || tempReqLocFullName.contains(tempKeyword)
              || tempReqLocAbb.contains(tempKeyword)) {
            matchingCount++;

            ArrayList<String> temp_arraylist_matched = new ArrayList<>();
            temp_arraylist_matched.add(operatorName);
            temp_arraylist_matched.add(operatorIdkey);
            temp_arraylist_matched.add(reqLocFullName);

            matchingSets.add(temp_arraylist_matched);
          }
        }
      }

      if (matchingCount == 0) {
        MessageCli.OPERATORS_FOUND.printMessage("are", "no", "s", ".");
      } else if (matchingCount == 1) {
        String matchingCountString = Integer.toString(matchingCount);
        MessageCli.OPERATORS_FOUND.printMessage("is", matchingCountString, "", ":");

        for (int i = 0; i < matchingCount; i++) {
          ArrayList<String> tempPulledSet = matchingSets.get(i);
          MessageCli.OPERATOR_ENTRY.printMessage(
              tempPulledSet.get(0), tempPulledSet.get(1), tempPulledSet.get(2));
        }
      } else if (matchingCount > 1) {
        String matchingCountString = Integer.toString(matchingCount);
        MessageCli.OPERATORS_FOUND.printMessage("are", matchingCountString, "s", ":");

        for (int i = 0; i < matchingCount; i++) {
          ArrayList<String> tempPulledSet = matchingSets.get(i);
          MessageCli.OPERATOR_ENTRY.printMessage(
              tempPulledSet.get(0), tempPulledSet.get(1), tempPulledSet.get(2));
        }
      }
    }
  }

  public void createOperator(String operatorName, String location) {
    operatorName = operatorName.strip();
    if (operatorName.length() > 3) {
      Location reqLocation = Location.fromString(location);
      if (reqLocation != null) {
        String reqLocationFullName = reqLocation.getFullName();
        String reqLocationAbb = reqLocation.getLocationAbbreviation();

        Boolean locationMapFlag = locationsMap.containsKey(reqLocationAbb);
        if (locationMapFlag == true) {
          HashMap<String, String> localLocationsMap = locationsMap.get(reqLocationAbb);
          Boolean localLocationFlag = localLocationsMap.containsValue(operatorName);

          if (localLocationFlag == true) {
            MessageCli.OPERATOR_NOT_CREATED_ALREADY_EXISTS_SAME_LOCATION.printMessage(
                operatorName, reqLocationFullName);
          } else if (localLocationFlag == false) {
            Integer operatorIndex = localLocationsMap.size() + 1;
            String operatorId = operatorInitialBuilder(operatorName, operatorIndex, reqLocationAbb);
            localLocationsMap.put(operatorId, operatorName);
            MessageCli.OPERATOR_CREATED.printMessage(operatorName, operatorId, reqLocationFullName);
          }
        } else {
          HashMap<String, String> localLocationsMap = new HashMap<>();
          Integer operatorIndex = localLocationsMap.size() + 1;
          String operatorId = operatorInitialBuilder(operatorName, operatorIndex, reqLocationAbb);

          localLocationsMap.put(operatorId, operatorName);
          locationsMap.put(reqLocationAbb, localLocationsMap);
          MessageCli.OPERATOR_CREATED.printMessage(operatorName, operatorId, reqLocationFullName);
        }
      } else {
        MessageCli.OPERATOR_NOT_CREATED_INVALID_LOCATION.printMessage(location);
      }
    } else {
      MessageCli.OPERATOR_NOT_CREATED_INVALID_OPERATOR_NAME.printMessage(operatorName);
    }
  }

  public void viewActivities(String operatorId) {
    // TODO implement
  }

  public void createActivity(String activityName, String activityType, String operatorId) {
    // TODO implement
  }

  public void searchActivities(String keyword) {
    // TODO implement
  }

  public void addPublicReview(String activityId, String[] options) {
    // TODO implement
  }

  public void addPrivateReview(String activityId, String[] options) {
    // TODO implement
  }

  public void addExpertReview(String activityId, String[] options) {
    // TODO implement
  }

  public void displayReviews(String activityId) {
    // TODO implement
  }

  public void endorseReview(String reviewId) {
    // TODO implement
  }

  public void resolveReview(String reviewId, String response) {
    // TODO implement
  }

  public void uploadReviewImage(String reviewId, String imageName) {
    // TODO implement
  }

  public void displayTopActivities() {
    // TODO implement
  }
}
