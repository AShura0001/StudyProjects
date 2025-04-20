package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import nz.ac.auckland.se281.Types.ActivityType;
import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  private HashMap<String, HashMap<String, String>> locationsMap = new HashMap<>();
  private HashMap<String, HashMap<String, ArrayList<String>>> activityMap = new HashMap<>();

  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  private String activityIdBuilder(Integer activityIndex, String operatorId) {
    StringBuilder activityId = new StringBuilder();
    activityId.append(operatorId);
    activityId.append("-");
    if (activityIndex < 10) {
      activityId.append("00");
      activityId.append(activityIndex);
    } else if (activityIndex < 100) {
      activityId.append("0");
      activityId.append(activityIndex);
    } else if (activityIndex < 1000) {
      activityId.append(activityIndex);
    }
    return activityId.toString();
  }

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

  private Boolean opIdConfirmer(String operatorId) {
    Boolean validOpIdFlag = false;
    for (HashMap<String, String> localLocations : locationsMap.values()) {
      if (localLocations.containsValue(operatorId)) {
        validOpIdFlag = true;
      }
    }
    return validOpIdFlag;
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
    if (operatorName.length() >= 3) {
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
    Boolean validOpIdFlag = opIdConfirmer(operatorId);
    if (validOpIdFlag) {
      if (activityMap.containsKey(operatorId)) {
        // [operator name initials, locaion abb, operator index]
        String[] operatorComponents = operatorId.split("-");
        HashMap<String, String> localLocationMap = locationsMap.get(operatorComponents[1]);
        String operatorName = localLocationMap.get(operatorId);
        HashMap<String, ArrayList<String>> operatorIdMap = activityMap.get(operatorId);
        Integer numActivities = operatorIdMap.size();
        if (numActivities == 1) {
          MessageCli.ACTIVITIES_FOUND.printMessage("is", "1", "y", ":");
          for (String activityID : operatorIdMap.keySet()) {
            // acitvityID = acitivityDetails[name, type]
            ArrayList<String> activityDetails = operatorIdMap.get(activityID);
            // ACTIVITY_ENTRY("  * %s: [%s/%s] offered by %s"),
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityID, activityDetails.get(1), operatorName);
          }
        } else {
          MessageCli.ACTIVITIES_FOUND.printMessage("are", numActivities.toString(), "ies", ":");
          for (String activityID : operatorIdMap.keySet()) {
            // acitvityID = acitivityDetails[name, type]
            ArrayList<String> activityDetails = operatorIdMap.get(activityID);
            // ACTIVITY_ENTRY("  * %s: [%s/%s] offered by %s"),
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityID, activityDetails.get(1), operatorName);
          }
        }
      } else {
        MessageCli.ACTIVITIES_FOUND.printMessage("are", "no", "ies", ".");
      }

    } else {
      MessageCli.OPERATOR_NOT_FOUND.printMessage(operatorId);
    }
  }

  public void createActivity(String activityName, String activityType, String operatorId) {

    activityName = activityName.trim();
    if (activityName.length() >= 3) {
      Boolean validOpIdFlag = opIdConfirmer(operatorId);
      if (validOpIdFlag) {
        ActivityType reqActivityType = ActivityType.fromString(activityType);
        String nameActivityType = reqActivityType.getName();
        // HashMap<String, ArrayList<String>> operatorIdMap = new HashMap<>();
        if (activityMap.containsKey(operatorId)) {
          HashMap<String, ArrayList<String>> operatorIdMap = activityMap.get(operatorId);
          Integer activityIndex = operatorIdMap.size() + 1;
          String acitivityId = activityIdBuilder(activityIndex, operatorId);
          ArrayList<String> activityDetails = new ArrayList<>();
          activityDetails.add(activityName);
          activityDetails.add(nameActivityType);
          operatorIdMap.put(acitivityId, activityDetails);
        } else {
          HashMap<String, ArrayList<String>> operatorIdMap = new HashMap<>();
          Integer activityIndex = operatorIdMap.size() + 1;
          String acitivityId = activityIdBuilder(activityIndex, operatorId);
          ArrayList<String> activityDetails = new ArrayList<>();
          // [name, type]
          activityDetails.add(activityName);
          activityDetails.add(nameActivityType);
          operatorIdMap.put(acitivityId, activityDetails);
          // <activity id: [name, type]>
          activityMap.put(operatorId, operatorIdMap);
          // activityMap<opID: <activity id: [name, type]>>
        }
      } else {
        MessageCli.ACTIVITY_NOT_CREATED_INVALID_OPERATOR_ID.printMessage(operatorId);
      }
    } else {
      MessageCli.ACTIVITY_NOT_CREATED_INVALID_ACTIVITY_NAME.printMessage(activityName);
    }
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
