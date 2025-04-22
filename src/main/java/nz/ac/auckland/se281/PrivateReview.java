package nz.ac.auckland.se281;

public class PrivateReview extends Review {
  private Boolean followUpFlag = false;
  private String email;

  PrivateReview(String acitvityId, String[] options) {
    super(acitvityId, options);
    if (options[4].equalsIgnoreCase("Y")) {
      followUpFlag = true;
    }
    email = options[1];
    rating = Integer.valueOf(options[2]);
    comment = options[3];
  }

  public Boolean getFollowUpFlag() {
    return this.followUpFlag;
  }

  public String getEmail() {
    return this.email;
  }
}
