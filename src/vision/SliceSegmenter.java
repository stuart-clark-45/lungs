package vision;

import org.opencv.core.Mat;

/**
 * Used segment CT slices.
 *
 * @author Stuart Clark
 */
public class SliceSegmenter {

  /**
   * The value used for the foreground in the segmented images that are returned.
   */
  private int foreground;

  private int sureFG;

  private int sureBG;

  public SliceSegmenter(int foreground, int sureFG, int sureBG) {
    this.foreground = foreground;
    this.sureFG = sureFG;
    this.sureBG = sureBG;
  }

  public Mat segment(Mat original) {

    return original;

  }

}
