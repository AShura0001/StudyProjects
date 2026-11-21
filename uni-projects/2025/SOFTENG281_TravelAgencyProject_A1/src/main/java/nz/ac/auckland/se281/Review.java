package nz.ac.auckland.se281;

abstract class Review {
  // Abstract class so that it serves as template for child review class types
  // while also making sure it doesn't get accessed by other than child classes
  // for constructing actual instances.
  protected String name;
  protected Integer rating;
  protected String comment;
  protected String reviewId;
  protected String[] options;
  protected String activityId;
  protected String type;

  protected Review(String acitvityId, String[] options) {
    // Generic constructor for all reviews
    this.activityId = acitvityId;
    this.options = options;
    this.name = options[0];
  }

  protected void setRating(Integer rating) {
    this.rating = rating;
    if (rating < 1) {
      this.rating = 1;
    } else if (rating > 5) {
      this.rating = 5;
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

  public void displayReviewEntry() {
    // Generic display method for all reviews.
    // REVIEW_ENTRY_HEADER("  * [%s/%s] %s review (%s) by '%s'"),
    // * [3/5] Public review (SSB-TRG-002-001-R1) by 'Alice'
    MessageCli.REVIEW_ENTRY_HEADER.printMessage(
        this.rating.toString(), "5", this.type, this.reviewId, this.name);
    System.out.println(String.format("    \"%s\"", comment));
  }

  public void insertReviewId(String reviewId) {
    // Instert reviewId provided using the reviewIdCreator method in the OperatorManagement class.
    this.reviewId = reviewId;
  }
}
