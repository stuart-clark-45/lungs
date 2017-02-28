package util;

import java.util.Iterator;

/**
 * Used to place a limit on the number of elements that can be obtained from an {@link Iterator}.
 *
 * @author Stuart Clark
 */
public class BatchIterator<T> implements Iterator<T> {

  private final Iterator<T> iterator;
  private final int batchSize;
  private int batchStop;
  private int counter;

  /**
   * The number of times that {@link BatchIterator#next()} can be called before it returns
   * {@code null}.
   */
  private Integer limit;

  /**
   * @param iterator the {@link Iterator} to limit.
   * @param batchSize the size of a single batch.
   */
  public BatchIterator(Iterator<T> iterator, int batchSize) {
    this.iterator = iterator;
    this.batchSize = batchSize;
    this.batchStop = batchSize;
    this.counter = 0;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext() && counter < batchStop && (limit == null || counter < limit);
  }

  @Override
  public T next() {
    if (counter++ >= batchStop || (limit != null && counter > limit)) {
      return null;
    }
    return iterator.next();
  }

  public void nextBatch() {
    batchStop += batchSize;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }
}
