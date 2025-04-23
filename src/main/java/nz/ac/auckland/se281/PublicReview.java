package nz.ac.auckland.se281;

public class PublicReview extends Review {
  private Boolean endorsedFlag = false;

  PublicReview(String acitvityId, String[] options) {
    super(acitvityId, options);
    this.type = "Public";
    if (options[1].equalsIgnoreCase("Y")) {
      this.name = "Anonymous";
    }
    rating = Integer.valueOf(options[2]);
    comment = options[3];
  }

  public Boolean getEndorsedFlag() {
    return this.endorsedFlag;
  }

  public void endorse() {
    endorsedFlag = true;
  }

  @Override
  public void displayReviewEntry() {
    super.displayReviewEntry();
    if (endorsedFlag) {
      MessageCli.REVIEW_ENTRY_ENDORSED.printMessage();
    }
  }
}
