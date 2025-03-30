package nz.ac.auckland.se281;

import java.util.ArrayList;
import java.util.HashMap;

import nz.ac.auckland.se281.Types.Location;

public class OperatorManagementSystem {

  private HashMap<String, HashMap<String, String>> locations_map = new HashMap<>();


  // Do not change the parameters of the constructor
  public OperatorManagementSystem() {}

  public void searchOperators(String keyword) {
    MessageCli.OPERATORS_FOUND.printMessage("are", "no", "s", ".");
  }

  public void createOperator(String operatorName, String location) {
    Location req_location = Location.fromString(location);
    String req_location_full_name = req_location.getFullName();
    String req_location_abb = req_location.getLocationAbbreviation();
    
    Boolean location_map_flag = locations_map.containsKey(req_location_abb); 
    if (location_map_flag == true){
      HashMap<String, String> local_locations_map = locations_map.get(req_location_abb);
      Boolean local_location_flag = local_locations_map.containsKey(operatorName);
      
      if (local_location_flag == true){
        MessageCli.OPERATOR_NOT_CREATED_ALREADY_EXISTS_SAME_LOCATION.printMessage(operatorName, req_location_full_name);
      }

      else if (local_location_flag == false){
        // Mutable list for operator initials.
        ArrayList<Character> operator_initials_arraylist = new ArrayList<>();
        String[] words = operatorName.split(" ");
        for (String word : words) {
            operator_initials_arraylist.add(word.charAt(0));
        }
        // Mutable string converted from list to stringbuilder class.
        StringBuilder operator_initials_sb = new StringBuilder();
          for (Character ch : operator_initials_arraylist){
            operator_initials_sb.append(ch);
          } 
        // String Builder Class turned to String class.
        String operator_initials = operator_initials_sb.toString();

        // creating the number for the operator id
        StringBuilder operator_ID_numBuilder = new StringBuilder();
        Integer operator_index = local_locations_map.size() + 1;
        String operator_indexString = Integer.toString(operator_index);
        if (operator_index<10){
          operator_ID_numBuilder.append("00");
          operator_ID_numBuilder.append(operator_indexString);
        }
        else if (operator_index<100){
          operator_ID_numBuilder.append("0");
          operator_ID_numBuilder.append(operator_indexString);
        }
        
        else if (operator_index<1000){
          operator_ID_numBuilder.append(operator_indexString);
        }
        String operator_ID_number = operator_ID_numBuilder.toString();
        

        // String[] operator_ID_array = new String[3];
        String operator_ID = String.join("-",operator_initials,req_location_abb,operator_ID_number);

        local_locations_map.put(operatorName, operator_ID);
        MessageCli.OPERATOR_CREATED.printMessage(operatorName, operator_ID, req_location_full_name);
      }
    }

    else {
      HashMap<String, String> local_locations_map = new HashMap<>();
      ArrayList<Character> operator_initials_arraylist = new ArrayList<>();
      String[] words = operatorName.split(" ");
      for (String word : words) {
          operator_initials_arraylist.add(word.charAt(0));
      }
      // Mutable string converted from list to stringbuilder class.
      StringBuilder operator_initials_sb = new StringBuilder();
        for (Character ch : operator_initials_arraylist){
          operator_initials_sb.append(ch);
        } 
      // String Builder Class turned to String class.
      String operator_initials = operator_initials_sb.toString();

      // creating the number for the operator id
      StringBuilder operator_ID_numBuilder = new StringBuilder();
      Integer operator_index = local_locations_map.size() + 1;
      String operator_indexString = Integer.toString(operator_index);
      if (operator_index<10){
        operator_ID_numBuilder.append("00");
        operator_ID_numBuilder.append(operator_indexString);
      }
      else if (operator_index<100){
        operator_ID_numBuilder.append("0");
        operator_ID_numBuilder.append(operator_indexString);
      }
      
      else if (operator_index<1000){
        operator_ID_numBuilder.append(operator_indexString);
      }
      String operator_ID_number = operator_ID_numBuilder.toString();
      

      String operator_ID = String.join("-",operator_initials,req_location_abb,operator_ID_number);

      local_locations_map.put(operatorName, operator_ID);
      MessageCli.OPERATOR_CREATED.printMessage(operatorName, operator_ID, req_location_full_name);
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
