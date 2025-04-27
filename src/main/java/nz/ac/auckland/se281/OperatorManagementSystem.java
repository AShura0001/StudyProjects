package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;
import nz.ac.auckland.se281.Types.ActivityType;
import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  private HashMap<String, HashMap<String, String>> locationsMap = new HashMap<>();
  private HashMap<String, HashMap<String, ArrayList<String>>> activityMap = new HashMap<>();
  private HashMap<String, ArrayList<Review>> reviewMap = new HashMap<>();
  private HashMap<String, ArrayList<String>> rankingMap = new HashMap<>();

  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  private void pushReview(Review review, String activityId) {
    Boolean validActivityId = false;
    String activityName = null;

    ArrayList<String> activityDetails = activityDetailFetcher(activityId);
    // Check if the activity ID is valid and fetch the activity related details.
    if (activityDetails.get(0).equals("Y")) {
      validActivityId = true;
      activityName = activityDetails.get(1);
    }

    if (validActivityId) {
      // Insert the review into the review map
      // Pushes out appropriate messagee about the specific review being added.
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
    // Method to fetch the activity details from the activity map if
    // the activity ID is valid.
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
          String[] activityIdExploded = activityId.split("-");
          tempList.add(activityIdExploded[1]);
          // saved in the list as follows:
          // [validActivityFlag, name, type, location abb]
        }
      }
    }
    return tempList;
  }

  private String reviewIdCreator(String activityId, HashMap<String, ArrayList<Review>> reviewMap) {
    // Method to create the review ID for the review being added.
    Integer reviewIndex = reviewMap.get(activityId).size() + 1;
    String reviewId = activityId + "-" + "R" + reviewIndex.toString();
    return reviewId;
  }

  private String opNameFetcher(String operatorId) {
    // Method to fetch the operator name from the operator ID.
    // Splits the operator ID into its components and fetches
    // the name from the locations map using the location abb at index 1 in the list.
    String[] operatorComponents = operatorId.split("-");
    HashMap<String, String> localLocationMap = locationsMap.get(operatorComponents[1]);
    String operatorName = localLocationMap.get(operatorId);
    return operatorName;
  }

  private String activityIdBuilder(Integer activityIndex, String operatorId) {
    // Method to create the activity ID for the activity being added based
    // on the given parameters.
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
    // Method to create the operator ID for the respective operator being added.
    // Mutable list for operator initials.
    ArrayList<Character> operatorInitialsArraylist = new ArrayList<>();
    String[] words = operatorName.split(" ");
    // Looping through the operator name and adding the first character of each word to the list.
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

    // creating the number for the operator id based on the operator index.
    // The number is padded with 0s to make it 3 digits long.
    // The number is then converted to string and appended to the operator initials and location
    // abb.
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
    // Checks if operator ID is valid by checking if it exists in the locations map as a value.
    Boolean validOpIdFlag = false;
    for (HashMap<String, String> localLocations : locationsMap.values()) {
      if (localLocations.containsKey(operatorId)) {
        validOpIdFlag = true;
      }
    }
    return validOpIdFlag;
  }

  public void searchOperators(String keyword) {
    // Method to search for operators based on the given keyword.

    // removes any leading or trailing spaces from the keyword.
    keyword = keyword.strip();
    if (keyword.equals("*")) {
      // If the keyword is "*", it means all operators are needed to be displayed.
      Integer operatorCount = 0;
      for (HashMap<String, String> locations : locationsMap.values()) {
        operatorCount += locations.size();
      }

      Integer locationsMapSize = locationsMap.size();
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
      // If the keyword is not "*", it means the user wants to search for specific operators.
      // The keyword can be a part of the operator name, location abbreviation, or location full
      // name.

      ArrayList<ArrayList<String>> matchingSets = new ArrayList<>();
      Integer matchingCount = 0;

      for (String localLocationsKey : locationsMap.keySet()) {
        HashMap<String, String> localLocation = locationsMap.get(localLocationsKey);
        Location reqLocation = Location.fromString(localLocationsKey);
        String reqLocAbb = reqLocation.getLocationAbbreviation();
        String reqLocFullName = reqLocation.getFullName();
        // Everything is converted to upper case for case insensitive search as
        // contains method is case sensitive and we want to do look for substring
        // in a case insensitive manner.
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
    // Method to create the operator based on the given parameters.
    // The operator name is trimmed to remove any leading or trailing spaces.
    operatorName = operatorName.strip();
    if (operatorName.length() >= 3) {
      Location reqLocation = Location.fromString(location);
      if (reqLocation != null) {
        String reqLocationFullName = reqLocation.getFullName();
        String reqLocationAbb = reqLocation.getLocationAbbreviation();

        Boolean locationMapFlag = locationsMap.containsKey(reqLocationAbb);
        if (locationMapFlag == true) {
          // If the location map already exists, check if the operator name already
          // exists in the location map.
          HashMap<String, String> localLocationsMap = locationsMap.get(reqLocationAbb);
          Boolean localLocationFlag = localLocationsMap.containsValue(operatorName);

          if (localLocationFlag == true) {
            // If the operator name already exists in the location map, do not create a new
            // operator.
            MessageCli.OPERATOR_NOT_CREATED_ALREADY_EXISTS_SAME_LOCATION.printMessage(
                operatorName, reqLocationFullName);
          } else if (localLocationFlag == false) {
            // If the operator name does not exist in the location map, create a new operator.
            Integer operatorIndex = localLocationsMap.size() + 1;
            String operatorId = operatorInitialBuilder(operatorName, operatorIndex, reqLocationAbb);
            localLocationsMap.put(operatorId, operatorName);
            MessageCli.OPERATOR_CREATED.printMessage(operatorName, operatorId, reqLocationFullName);
          }
        } else {
          // This is the first operator being created in this location.
          // Create a new location map and add the operator to it.
          // The location doesn not exist in the locations map prior to this operator being
          // created.
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
    // Method to view the activities for the given operator ID if it is valid.
    Boolean validOpIdFlag = opIdConfirmer(operatorId);
    if (validOpIdFlag) {
      // If the operator ID is valid, check if the operator ID exists in the activity map.
      if (activityMap.containsKey(operatorId)) {
        // Since it exists in the activity map, it means that the operator has activities.
        // Fetch the activities for the operator ID.
        String operatorName = opNameFetcher(operatorId);
        HashMap<String, ArrayList<String>> operatorIdMap = activityMap.get(operatorId);
        Integer numActivities = operatorIdMap.size();
        if (numActivities == 1) {
          // if the number of activities is 1, print accordingly.
          MessageCli.ACTIVITIES_FOUND.printMessage("is", "1", "y", ":");
          for (String activityId : operatorIdMap.keySet()) {
            // loops through the activities for the operator ID and prints them based on the
            // activity ID, which is the key for activity details in the operator ID map.

            // acitvityID = acitivityDetails[name, type]
            ArrayList<String> activityDetails = operatorIdMap.get(activityId);
            // ACTIVITY_ENTRY("  * %s: [%s/%s] offered by %s"),
            MessageCli.ACTIVITY_ENTRY.printMessage(
                activityDetails.get(0), activityId, activityDetails.get(1), operatorName);
          }
        } else {
          // if the number of activities is greater than 1, print accordingly.
          MessageCli.ACTIVITIES_FOUND.printMessage("are", numActivities.toString(), "ies", ":");
          for (String activityId : operatorIdMap.keySet()) {
            // loops through the activities for the operator ID and prints them based on the
            // activity ID, which is the key for activity details in the operator ID map.

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
    // Method to create the activity based on the given parameters.
    activityName = activityName.trim();
    if (activityName.length() >= 3) {
      Boolean validOpIdFlag = opIdConfirmer(operatorId);
      if (validOpIdFlag) {
        // If the operator ID is valid, check if the operator ID exists in the activity map.
        String operatorName = opNameFetcher(operatorId);
        ActivityType reqActivityType = ActivityType.fromString(activityType);
        String nameActivityType = reqActivityType.getName();
        // HashMap<String, ArrayList<String>> operatorIdMap = new HashMap<>();
        if (activityMap.containsKey(operatorId)) {
          // If the operator ID exists in the activity map, it means that the operator has
          // activities already and the new activity is being added to the existing
          // activities map.
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
          // If the operator ID does not exist in the activity map, it means that the operator
          // does not have any activities yet and the new activity is added with a new map
          // for the operator ID within the activity map.
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
    // Method to search for activities based on the given keyword.
    // Similar to the search operators method, this method searches for activities
    // based on the given keyword.
    keyword = keyword.strip();
    if (keyword.equals("*")) {
      // If the keyword is "*", it means all activities are needed to be displayed.
      Integer acitivityCount = 0;
      for (HashMap<String, ArrayList<String>> opIdMap : activityMap.values()) {
        acitivityCount += opIdMap.size();
      }

      // Prints out messages and the activities based on the number of activities found.
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
      // Searches for keyword as a substring in the activity name, activity type,
      // location abbreviation, and location full name.
      // Everything is converted to upper case for case insensitive search as
      // contains method is case sensitive and we want to look for substring
      // in a case insensitive manner.
      String tempkeyword = keyword.toUpperCase();
      // An arraylist of arraylists to store the matching sets of activities.
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
            // a temp arraylist of activity details matched.
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
    // Adds a public review
    PublicReview review = new PublicReview(activityId, options);
    pushReview(review, activityId);
  }

  public void addPrivateReview(String activityId, String[] options) {
    // Adds a private review
    PrivateReview review = new PrivateReview(activityId, options);
    pushReview(review, activityId);
  }

  public void addExpertReview(String activityId, String[] options) {
    // Adds an expert review
    ExpertReview review = new ExpertReview(activityId, options);
    pushReview(review, activityId);
  }

  public void displayReviews(String activityId) {
    // Method to display the reviews for the given activity ID.
    // Similar to search activities method and search operators method.
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
          // access the review and use the display method defined
          // within the review class to print the review details.
          review.displayReviewEntry();
        }
      } else if (numReviews > 1) {
        MessageCli.REVIEWS_FOUND.printMessage("are", numReviews.toString(), "s", activityId);
        for (Review review : reviewMap.get(activityId)) {
          // access the review and use the display method defined
          // within the review class to print the review details.
          review.displayReviewEntry();
        }
      }
    } else {
      MessageCli.ACTIVITY_NOT_FOUND.printMessage(activityId);
    }
  }

  public void endorseReview(String reviewId) {
    // Method to endorse 'Public' reviews based on the given review ID.
    Boolean reviewExistFlag = false;
    Boolean publicReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      // Loop through the review map and check if the review ID exists.
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof PublicReview) {
            // if the review id is matched and the review is a public review,
            // set the public review flag to true and endorse the review.
            // endorse method is defined in the PublicReview class.
            publicReviewFlag = true;
            PublicReview reqReview = (PublicReview) review;
            // Note: The endorse method is defined for the PublicReview class only.
            reqReview.endorse();
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (publicReviewFlag) {
        MessageCli.REVIEW_ENDORSED.printMessage(reviewId);
      } else {
        // If the review ID is found but it is not a public review, print the message.
        MessageCli.REVIEW_NOT_ENDORSED.printMessage(reviewId);
      }
    } else {
      // If the review ID is not found, print the message.
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void resolveReview(String reviewId, String response) {
    // Method to resolve 'Private' reviews based on the given review ID.
    Boolean reviewExistFlag = false;
    Boolean privateReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      // Loop through the review map and check if the review ID exists.
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof PrivateReview) {
            // if the review id is matched and the review is a private review,
            // set the private review flag to true and respond to the review.
            privateReviewFlag = true;
            PrivateReview reqReview = (PrivateReview) review;
            // Note: respond method is defined for the PrivateReview class only.
            reqReview.respond(response);
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (!privateReviewFlag) {
        // If the review ID is found but it is not a private review, print the message.
        MessageCli.REVIEW_NOT_RESOLVED.printMessage(reviewId);
      }
    } else {
      // If the review ID is not found, print the message.
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void uploadReviewImage(String reviewId, String imageName) {
    // Method to upload an image for 'Expert' reviews based on the given review ID.
    imageName = imageName.trim();
    Boolean reviewExistFlag = false;
    Boolean expertReviewFlag = false;
    for (ArrayList<Review> reviewList : reviewMap.values()) {
      // Loop through the review map and check if the review ID exists.
      for (Review review : reviewList) {
        if (review.getReviewId().equals(reviewId)) {
          reviewExistFlag = true;
          if (review instanceof ExpertReview) {
            // if the review id is matched and the review is an expert review,
            // set the expert review flag to true and upload the image.
            expertReviewFlag = true;
            ExpertReview reqReview = (ExpertReview) review;
            // Note: uploadImg method is defined in the ExpertReview class only.
            reqReview.uploadImg(imageName);
          }
        }
      }
    }
    if (reviewExistFlag) {
      if (!expertReviewFlag) {
        // If the review ID is found but it is not an expert review, print the message.
        MessageCli.REVIEW_IMAGE_NOT_ADDED_NOT_EXPERT.printMessage(reviewId);
      }
    } else {
      // If the review ID is not found, print the message.
      MessageCli.REVIEW_NOT_FOUND.printMessage(reviewId);
    }
  }

  public void displayTopActivities() {
    // Method to display the top activities based on the average rating.
    // The top activity is the one with the highest average rating in each location.
    // The location abbreviation is used as the key for the ranking map.
    // Only locations with at least one review are included in the ranking map.
    for (String activityId : reviewMap.keySet()) {
      Integer ratingsSum = 0;
      Integer avgRating = 0;
      ArrayList<String> tempList = new ArrayList<>();
      for (Review review : reviewMap.get(activityId)) {
        ratingsSum += review.getRating();
      }

      // Casting a long to int for an Integer variable (avgRating) only
      // since the long value will fit inside int and the variable being Integer makes it convenient
      // too.
      avgRating = (int) Math.round((double) (ratingsSum / (reviewMap.get(activityId).size())));
      ArrayList<String> activityDetails = activityDetailFetcher(activityId);
      String activityName = activityDetails.get(1);
      String locationAbb = activityDetails.get(3);
      tempList.add(activityName);
      tempList.add(avgRating.toString());
      if (rankingMap.containsKey(locationAbb)) {
        // If the location abbreviation already exists in the ranking map,
        // check if the average rating is greater than or equal to the current top rating.
        // If it is, update the ranking map with the new activity details.
        Integer currentTopRating = Integer.valueOf(rankingMap.get(locationAbb).get(1));
        if (avgRating >= currentTopRating) {
          rankingMap.put(locationAbb, tempList);
        }
      } else {
        rankingMap.put(locationAbb, tempList);
      }
    }
    for (Location location : Location.values()) {
      // Loop through all the locations and check if the location abbreviation exists in the ranking
      // map.
      // If it does, print the top activity details.
      // If it does not, print the message that there are no reviewed activities in that location.
      if (rankingMap.containsKey(location.getLocationAbbreviation())) {
        ArrayList<String> topActivityDetails = rankingMap.get(location.getLocationAbbreviation());
        MessageCli.TOP_ACTIVITY.printMessage(
            location.getFullName(), topActivityDetails.get(0), topActivityDetails.get(1));
      } else {
        MessageCli.NO_REVIEWED_ACTIVITIES.printMessage(location.getFullName());
      }
    }
  }
}
