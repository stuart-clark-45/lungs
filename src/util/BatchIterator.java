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
  private int limit;
  private int counter;

  /**
   * @param iterator the {@link Iterator} to limit.
   * @param batchSize the size of a single batch.
   */
  public BatchIterator(Iterator<T> iterator, int batchSize) {
    this.iterator = iterator;
    this.batchSize = batchSize;
    this.limit = batchSize;
    this.counter = 0;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext() && counter < limit;
  }

  @Override
  public T next() {
    if (counter++ >= limit) {
      return null;
    }
    return iterator.next();
  }

  public void nextBatch() {
    limit += batchSize;
  }

}
