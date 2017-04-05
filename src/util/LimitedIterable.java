package util;

import java.util.Iterator;

/**
 * Used to place a limit on the number of elements that can be obtained from an {@link Iterator}.
 *
 * @author Stuart Clark
 */
public class LimitedIterable<T> implements Iterable<T>, Iterator<T> {

  private final Iterator<T> iterator;
  private final int limit;
  private int counter;

  /**
   * @param iterator the {@link Iterator} to limit.
   * @param limit the number of times it should be possible to call {@link LimitedIterable#next()}.
   */
  public LimitedIterable(Iterator<T> iterator, int limit) {
    this.iterator = iterator;
    this.limit = limit;
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

  @Override
  public Iterator<T> iterator() {
    return this;
  }

}