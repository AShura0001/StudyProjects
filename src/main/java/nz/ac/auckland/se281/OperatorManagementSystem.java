package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import nz.ac.auckland.se281.Types.ActivityType;
import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  private HashMap<String, HashMap<String, String>> locationsMap = new HashMap<>();
  private HashMap<String, HashMap<String, ArrayList<String>>> activityMap = new HashMap<>();
  private HashMap<String, ArrayList<Review>> reviewMap = new HashMap<>();

  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  private void pushReview(Review review, String activityId) {
    Boolean validActivityId = false;
    String activityName = null;

    ArrayList<String> activityDetails = activityDetailFetcher(activityId);
    if (activityDetails.get(0).equals("Y")) {
      validActivityId = true;
      activityName = activityDetails.get(1);
    }

    if (validActivityId) {
      if (reviewMap.containsKey(activityId)) {
        review.insertReviewId(reviewIdCreator(activityId, reviewMap));
        reviewMap.get(activityId).add(review);
      } else {
        reviewMap.put(activityId, new ArrayList<Review>());
        review.insertReviewId(reviewIdCreator(activityId, reviewMap));
        reviewMap.get(activityId).add(review);
      }

      // Public review 'WACT-AKL-001-001-R1' added successfully for activity 'Bethells Beach Camel
      // Trek'.
      // REVIEW_ADDED("%s review '%s' added successfully for activity '%s'."),
      if (review instanceof PublicReview) {
        MessageCli.REVIEW_ADDED.printMessage("Public", review.getReviewId(), activityName);
      } else if (review instanceof PrivateReview) {
        MessageCli.REVIEW_ADDED.printMessage("Private", review.getReviewId(), activityName);
      } else if (review instanceof ExpertReview) {
        MessageCli.REVIEW_ADDED.printMessage("Expert", review.getReviewId(), activityName);
      }
    } else {
      MessageCli.REVIEW_NOT_ADDED_INVALID_ACTIVITY_ID.printMessage(activityId);
    }
  }

  private ArrayList<String> activityDetailFetcher(String activityId) {
    ArrayList<String> tempList = new ArrayList<>();
    String validActivityFlag = "N";
    tempList.add(validActivityFlag);
    for (HashMap<String, ArrayList<String>> opIdMap : activityMap.values()) {
      for (String activityIdKey : opIdMap.keySet()) {
        if (activityId.equals(activityIdKey)) {
          validActivityFlag = "Y";
          tempList.set(0, validActivityFlag);
          tempList.add(opIdMap.get(activityIdKey).get(0));
          tempList.add(opIdMap.get(activityIdKey).get(1));
          // [validActivityFlag, name, type]
        }
      }
    }
    return tempList;
  }

  private String reviewIdCreator(String activityId, HashMap<String, ArrayList<Review>> reviewMap) {
    Integer reviewIndex = reviewMap.get(activityId).size() + 1;
    String reviewId = activityId + "-" + "R" + reviewIndex.toString();
    return reviewId;
  }

  private String opNameFetcher(String operatorId) {
    String[] operatorComponents = operatorId.split("-");
    HashMap<String, String> localLocationMap = locationsMap.get(operatorComponents[1]);
    String operatorName = localLocationMap.get(operatorId);
    return operatorName;
  }

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
      if (localLocations.containsKey(operatorId)) {
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
        String operatorName = opNameFetcher(operatorId);
        HashMap<String, ArrayList<String>> operatorIdMap = activityMap.get(operatorId);
        Integer numActivities = operatorIdMap.size();
        if (numActivities == 1) {
          MessageCli.ACTIVITIES_FOUND.printMessage("is", "1", "y", ":");
          for (String activityId : operatorIdMap.keySet()) {
            // acitvityID = acitivityDetails[name, type]
            ArrayList<String> activityDetails = operatorIdMap.get(activityId);
            // ACTIVITY_ENTRY("  * %s: [%s/%s] offered by %s"),
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityId, activityDetails.get(1), operatorName);
          }
        } else {
          MessageCli.ACTIVITIES_FOUND.printMessage("are", numActivities.toString(), "ies", ":");
          for (String activityId : operatorIdMap.keySet()) {
            // acitvityID = acitivityDetails[name, type]
            ArrayList<String> activityDetails = operatorIdMap.get(activityId);
            // ACTIVITY_ENTRY("  * %s: [%s/%s] offered by %s"),
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityId, activityDetails.get(1), operatorName);
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
        String operatorName = opNameFetcher(operatorId);
        ActivityType reqActivityType = ActivityType.fromString(activityType);
        String nameActivityType = reqActivityType.getName();
        // HashMap<String, ArrayList<String>> operatorIdMap = new HashMap<>();
        if (activityMap.containsKey(operatorId)) {
          System.out.println("here");
          HashMap<String, ArrayList<String>> operatorIdMap = activityMap.get(operatorId);
          Integer activityIndex = operatorIdMap.size() + 1;
          String acitivityId = activityIdBuilder(activityIndex, operatorId);
          ArrayList<String> activityDetails = new ArrayList<>();
          activityDetails.add(activityName);
          activityDetails.add(nameActivityType);
          operatorIdMap.put(acitivityId, activityDetails);
          // ACTIVITY_CREATED("Successfully created activity '%s' ('%s': '%s') for '%s'."),
          MessageCli.ACTIVITY_CREATED.printMessage(
              activityName, acitivityId, nameActivityType, operatorName);
        } else {
          System.out.println("no here");
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
          MessageCli.ACTIVITY_CREATED.printMessage(
              activityName, acitivityId, nameActivityType, operatorName);
        }
      } else {
        MessageCli.ACTIVITY_NOT_CREATED_INVALID_OPERATOR_ID.printMessage(operatorId);
      }
    } else {
      MessageCli.ACTIVITY_NOT_CREATED_INVALID_ACTIVITY_NAME.printMessage(activityName);
    }
  }

  public void searchActivities(String keyword) {
    keyword = keyword.strip();
    if (keyword.equals("*")) {
      Integer acitivityCount = 0;
      for (HashMap<String, ArrayList<String>> opIdMap : activityMap.values()) {
        acitivityCount += opIdMap.size();
      }

      if (acitivityCount == 0) {
        MessageCli.ACTIVITIES_FOUND.printMessage("are", "no", "ies", ".");
      } else if (acitivityCount == 1) {
        MessageCli.ACTIVITIES_FOUND.printMessage("is", "1", "y", ":");
        for (String operatorId : activityMap.keySet()) {
          HashMap<String, ArrayList<String>> opIdmap = activityMap.get(operatorId);
          String operatorName = opNameFetcher(operatorId);
          for (String activityId : opIdmap.keySet()) {
            ArrayList<String> activityDetails = opIdmap.get(activityId);
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityId, activityDetails.get(1), operatorName);
          }
        }
      } else if (acitivityCount > 1) {
        MessageCli.ACTIVITIES_FOUND.printMessage("are", acitivityCount.toString(), "ies", ":");
        for (String operatorId : activityMap.keySet()) {
          HashMap<String, ArrayList<String>> opIdmap = activityMap.get(operatorId);
          String operatorName = opNameFetcher(operatorId);
          for (String activityId : opIdmap.keySet()) {
            ArrayList<String> activityDetails = opIdmap.get(activityId);
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityId, activityDetails.get(1), operatorName);
          }
        }
      }
    } else {
      String tempkeyword = keyword.toUpperCase();
      ArrayList<ArrayList<String>> matchingSets = new ArrayList<>();

      for (String operatorId : activityMap.keySet()) {
        String[] operatorDetails = operatorId.split("-");
        // operatorDetails[operator Initials, locations abb, operator index]
        Location location = Location.fromString(operatorDetails[1]);
        String locationAbb = location.getLocationAbbreviation();
        String locationFullName = location.getFullName();
        String tempLocationAbb = locationAbb.toUpperCase();
        String tempLocationFullName = locationFullName.toUpperCase();
        HashMap<String, ArrayList<String>> opIdMap = activityMap.get(operatorId);
        for (String activityId : opIdMap.keySet()) {
          ArrayList<String> activityDetails = opIdMap.get(activityId);
          String tempactivityName = activityDetails.get(0).toUpperCase();
          if (tempactivityName.contains(tempkeyword)
              || activityDetails.get(1).equalsIgnoreCase(tempkeyword)
              || tempLocationAbb.contains(tempkeyword)
              || tempLocationFullName.contains(tempkeyword)) {
            String operatorName = opNameFetcher(operatorId);
            ArrayList<String> tempArrayList = new ArrayList<>();
            tempArrayList.add(activityDetails.get(0)); // activity name
            tempArrayList.add(activityId); // activity id
            tempArrayList.add(activityDetails.get(1)); // activity type
            tempArrayList.add(operatorName); // operator name

            matchingSets.add(tempArrayList);
          }
        }
      }

      if (matchingSets.size() == 0) {
        MessageCli.ACTIVITIES_FOUND.printMessage("are", "no", "ies", ".");
      } else if (matchingSets.size() == 1) {
        MessageCli.ACTIVITIES_FOUND.printMessage("is", "1", "y", ":");
        for (ArrayList<String> tempPulledSet : matchingSets) {
          MessageCli.ACTIVITY_ENTRY.printMessage(
              tempPulledSet.get(0),
              tempPulledSet.get(1),
              tempPulledSet.get(2),
              tempPulledSet.get(3));
        }
      } else if (matchingSets.size() > 1) {
        MessageCli.ACTIVITIES_FOUND.printMessage("are", matchingSets.size() + "", "ies", ":");
        for (ArrayList<String> tempPulledSet : matchingSets) {
          MessageCli.ACTIVITY_ENTRY.printMessage(
              tempPulledSet.get(0),
              tempPulledSet.get(1),
              tempPulledSet.get(2),
              tempPulledSet.get(3));
        }
      }
    }
  }

  public void addPublicReview(String activityId, String[] options) {
    PublicReview review = new PublicReview(activityId, options);
    pushReview(review, activityId);
  }

  public void addPrivateReview(String activityId, String[] options) {
    PrivateReview review = new PrivateReview(activityId, options);
    pushReview(review, activityId);
  }

  public void addExpertReview(String activityId, String[] options) {
    ExpertReview review = new ExpertReview(activityId, options);
    pushReview(review, activityId);
  }

  public void displayReviews(String activityId) {
    Boolean validActivityFlag = false;
    ArrayList<String> activityDetails = activityDetailFetcher(activityId);
    // [flag, name, type]
    if (activityDetails.get(0).equals("Y")) {
      validActivityFlag = true;
    }

    if (validActivityFlag) {
      Integer numReviews = 0;
      if (reviewMap.containsKey(activityId)) {
        numReviews = reviewMap.get(activityId).size();
      }
      if (numReviews == 0) {
        MessageCli.REVIEWS_FOUND.printMessage("are", "no", "s", activityDetails.get(1));
      } else if (numReviews == 1) {
        // REVIEWS_FOUND("There %s %s review%s for activity '%s'."),
        MessageCli.REVIEWS_FOUND.printMessage("is", "1", "", activityDetails.get(1));
        for (Review review : reviewMap.get(activityId)) {
          review.displayReviewEntry();
        }
      } else if (numReviews > 1) {
        MessageCli.REVIEWS_FOUND.printMessage("are", numReviews.toString(), "s", activityId);
        for (Review review : reviewMap.get(activityId)) {
          review.displayReviewEntry();
        }
      }
    } else {
      MessageCli.ACTIVITY_NOT_FOUND.printMessage(activityId);
    }
  }

  public void endorseReview(String reviewId) {
    Boolean reviewExistFlag = false;
    Boolean publicReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof PublicReview) {
            publicReviewFlag = true;
            PublicReview reqReview = (PublicReview) review;
            reqReview.endorse();
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (publicReviewFlag) {
        MessageCli.REVIEW_ENDORSED.printMessage(reviewId);
      } else {
        MessageCli.REVIEW_NOT_ENDORSED.printMessage(reviewId);
      }
    } else {
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void resolveReview(String reviewId, String response) {
    Boolean reviewExistFlag = false;
    Boolean privateReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof PrivateReview) {
            privateReviewFlag = true;
            PrivateReview reqReview = (PrivateReview) review;
            reqReview.respond(response);
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (!privateReviewFlag) {
        MessageCli.REVIEW_NOT_RESOLVED.printMessage(reviewId);
      }
    } else {
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void uploadReviewImage(String reviewId, String imageName) {
    Boolean reviewExistFlag = false;
    Boolean expertReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof ExpertReview) {
            expertReviewFlag = true;
            ExpertReview reqReview = (ExpertReview) review;
            reqReview.uploadImg(imageName);
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (!expertReviewFlag) {
        MessageCli.REVIEW_NOT_RESOLVED.printMessage(reviewId);
      }
    } else {
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void displayTopActivities() {
    // TODO implement
  }
}
