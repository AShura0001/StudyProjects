package nz.ac.auckland.se281;

public class PrivateReview extends Review {
  private Boolean followUpFlag = false;
  private String email;
  private String response;

  PrivateReview(String acitvityId, String[] options) {
    // Constructor for private review
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

  public String getResponse() {
    return response;
  }

  public void respond(String response) {
    // Stores the response for the private review instance.
    // The followUpFlag is turned off as the review is resolved.
    this.response = response;
    followUpFlag = false;
    // Appropriate message is printed for the resolved review.
    MessageCli.REVIEW_RESOLVED.printMessage(reviewId);
  }

  @Override
  public void displayReviewEntry() {
    super.displayReviewEntry();
    if (followUpFlag) {
      // if the followUpFlag is true, the review is not resolved and the
      // need to follow up message is printed.
      MessageCli.REVIEW_ENTRY_FOLLOW_UP.printMessage(email);
    } else {
      // if the followUpFlag is false, the review is resolved and the
      // resolved message is printed.
      // if response was never required the same is printed.
      MessageCli.REVIEW_ENTRY_RESOLVED.printMessage("-");
    }
  }
}
