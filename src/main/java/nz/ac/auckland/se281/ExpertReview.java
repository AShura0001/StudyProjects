package nz.ac.auckland.se281;

public class ExpertReview extends Review {
  private Boolean recommendFlag = false;

  ExpertReview(String acitvityId, String[] options) {
    super(acitvityId, options);
    if (options[3].equalsIgnoreCase("Y")) {
      recommendFlag = true;
    }
    rating = options[1];
    comment = options[2];
  }

  public Boolean getRecommendFlag() {
    return this.recommendFlag;
  }
}
