package nz.ac.auckland.se281;

public class PrivateReview extends Review {
  private Boolean followUpFlag = false;
  private String email;
  private String response;

  PrivateReview(String acitvityId, String[] options) {
    super(acitvityId, options);
    this.type = "Private";
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

  @Override
  public void displayReviewEntry() {
    super.displayReviewEntry();
    if (followUpFlag) {
      MessageCli.REVIEW_ENTRY_FOLLOW_UP.printMessage(email);
    } else {
      MessageCli.REVIEW_ENTRY_RESOLVED.printMessage("-");
    }
  }

  public void respond(String response) {
    this.response = response;
    followUpFlag = false;
    MessageCli.REVIEW_RESOLVED.printMessage(reviewId);
  }
}
