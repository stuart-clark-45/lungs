package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wraps around a {@link Map} to count the number of times that a key is placed into it.
 *
 * N.B. it is essential that {@code hashCode()} is implemented by the objects you want to count.
 *
 * @author Stuart Clark
 */
public class Counter<T> {

  private Map<T, Integer> map;

  public Counter() {
    map = new HashMap<>();
  }

  /**
   * Adds one to the count of {@code t}
   * 
   * @param t
   */
  public synchronized void add(T t) {
    if (map.containsKey(t)) {
      // Increment count stored as value in map
      map.put(t, map.get(t) + 1);
    } else {
      // Add count of 1
      map.put(t, 1);
    }
  }

  public Set<Map.Entry<T, Integer>> entrySet() {
    return map.entrySet();
  }

  public Integer get(T t) {
    return map.get(t);
  }

  public boolean containsKey(T t) {
    return map.containsKey(t);
  }

}
