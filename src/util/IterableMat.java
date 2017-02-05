package util;

import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.opencv.core.Mat;

/**
 * Used to iterate over pixels in a {@link Mat}.
 *
 * @author Stuart Clark
 */
public class IterableMat implements Iterable<Double[]> {

  private Mat mat;

  public IterableMat(Mat mat) {
    this.mat = mat;
  }

  @Override
  public Iterator iterator() {
    return new MatIterator();
  }

  private class MatIterator implements Iterator<Double[]> {

    private int row;
    private int col;

    @Override
    public boolean hasNext() {
      return row < mat.rows();
    }

    @Override
    public Double[] next() {
      Double[] pixel = ArrayUtils.toObject(mat.get(row, col));

      col++;

      // Go to next column if end of row.
      if (col == mat.cols()) {
        col = 0;
        row++;
      }

      return pixel;
    }

  }

}
