package nz.ac.auckland.se281;

abstract class Review {
  protected String name;
  protected String rating;
  protected String comment;
  protected String reviewId;
  protected String[] options;
  protected String activityId;

  protected Review(String acitvityId, String[] options) {
    this.activityId = acitvityId;
    this.options = options;
    this.name = options[0];
  }

  public String getName() {
    return name;
  }

  public String getRating() {
    return rating;
  }

  public String getComment() {
    return comment;
  }

  public String getReviewId() {
    return reviewId;
  }

  public void insertReviewId(String reviewId) {
    this.reviewId = reviewId;
  }
}
