package nz.ac.auckland.se281;

public class PublicReview extends Review {
  private Boolean endorsedFlag = false;

  PublicReview(String acitvityId, String[] options) {
    super(acitvityId, options);
    if (options[1].equalsIgnoreCase("Y")) {
      this.name = "Anonymous";
    }
    rating = options[2];
    comment = options[3];
  }

  public Boolean getEndorsedFlag() {
    return this.endorsedFlag;
  }
}
