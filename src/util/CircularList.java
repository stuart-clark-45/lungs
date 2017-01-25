package util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list the loops round to the first element when {@link CircularList#get(int)} is called with an
 * index that is greater than the max index of the list and vice versa.
 *
 * @author Stuart Clark
 */
public class CircularList<E> extends ArrayList<E> {

  public CircularList(Collection<? extends E> c) {
    super(c);
  }

  @Override
  public E get(int index) {
    while (index < 0) {
      index = size() + index;
    }
    return super.get(index % size());
  }

}
