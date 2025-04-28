package nz.ac.auckland.se281;

import java.util.ArrayList;

public class ExpertReview extends Review {
  private Boolean recommendFlag = false;
  private ArrayList<String> images = new ArrayList<>();

  ExpertReview(String acitvityId, String[] options) {
    // Constructor for expert review
    super(acitvityId, options);
    this.type = "Expert";
    if (options[3].equalsIgnoreCase("Y")) {
      recommendFlag = true;
    }
    setRating(Integer.valueOf(options[1]));
    comment = options[2];
  }

  public Boolean getRecommendFlag() {
    return this.recommendFlag;
  }

  public ArrayList<String> getImg() {
    return images;
  }

  public void uploadImg(String imgName) {
    // Uploads the image name to the review instance.
    // The image name is added to the images array list.
    images.add(imgName);
    MessageCli.REVIEW_IMAGE_ADDED.printMessage(imgName, reviewId);
  }

  @Override
  public void displayReviewEntry() {
    super.displayReviewEntry();
    if (recommendFlag) {
      MessageCli.REVIEW_ENTRY_RECOMMENDED.printMessage();
    }

    Integer imgListSize = images.size();
    if (imgListSize > 0) {
      // prints the image names uploaded for the review.
      StringBuilder imagesArray = new StringBuilder();
      for (int index = 0; index < imgListSize - 1; index++) {
        imagesArray.append(images.get(index));
        imagesArray.append(",");
      }
      // the last image name is not printed with a comma at the end.
      imagesArray.append(images.get(imgListSize - 1));
      MessageCli.REVIEW_ENTRY_IMAGES.printMessage(imagesArray.toString());
    }
  }
}
