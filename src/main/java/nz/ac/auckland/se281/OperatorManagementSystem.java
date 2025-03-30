package nz.ac.auckland.se281;

import java.util.ArrayList;

import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  public void searchOperators(String keyword) {
    MessageCli.OPERATORS_FOUND.printMessage("are", "no", "s", ".");
  }

  public void createOperator(String operatorName, String location) {
    Location req_location = Location.fromString(location);
    String req_location_full_name = req_location.getFullName();
    ArrayList<Character> operator_initials_arraylist = new ArrayList<>();
    
    String[] words = operatorName.split(" ");

    for (String word : words) {
        operator_initials_arraylist.add(word.charAt(0));
    }
    StringBuilder operator_initials_sb = new StringBuilder();
      for (Character ch : operator_initials_arraylist){
        operator_initials_sb.append(ch);
      } 
    String operator_initials = operator_initials_sb.toString();
    
    MessageCli.OPERATOR_CREATED.printMessage(operatorName, operator_initials, req_location_full_name);
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
