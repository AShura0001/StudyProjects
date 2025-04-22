package nz.ac.auckland.se281;

abstract class Review {
  protected String name;
  protected Integer rating;
  protected String comment;
  protected String reviewId;
  protected String[] options;
  protected String activityId;

  protected Review(String acitvityId, String[] options) {
    this.activityId = acitvityId;
    this.options = options;
    this.name = options[0];
    rating = 0;

    if (rating < 1) {
      rating = 1;
    } else if (rating > 5) {
      rating = 5;
    }
  }

  public String getName() {
    return name;
  }

  public Integer getRating() {
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
