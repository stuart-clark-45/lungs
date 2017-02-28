package util;

import java.util.Iterator;

/**
 * Used iterate through elements in an {@link Iterator} in batches. Also allows for a limit to be
 * placed on the number of elements that can bit iterated thorough.
 *
 * @author Stuart Clark
 */
public class BatchIterator<T> implements Iterator<T> {

  /**
   * The {@link Iterator} that to iterate over in batches.
   */
  private final Iterator<T> iterator;

  /**
   * The maximum number of elements that should be in each batch.
   */
  private final int batchSize;

  /**
   * Counts the number of times that {@link BatchIterator#next()} has been called.
   */
  private int counter;

  /**
   * The maximum value that {@code counter} should reach for this batch
   */
  private int batchStop;

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
