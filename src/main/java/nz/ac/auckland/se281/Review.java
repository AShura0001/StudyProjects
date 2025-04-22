package nz.ac.auckland.se281;

abstract class Review {
  private String name;
  private String rating;
  private String comment;
  private String reviewId;
  protected String[] options;
  protected String activityId;

  protected Review(String acitvityId, String[] options) {
    this.activityId = acitvityId;
    this.options = options;
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

  public void addReviewId(String reviewId) {
    this.reviewId = reviewId;
  }
}
