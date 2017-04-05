package util;

import java.util.Iterator;

import org.mongodb.morphia.query.Query;

import model.ROI;

/**
 * Used to synthesise new nodule {@link ROI}s
 *
 * @author Stuart Clark
 */
public class ROISynthesiser implements Iterator<ROI> {

  private Iterator<ROI> query;
  private long numRequired;
  private long counter;

  public ROISynthesiser(Query<ROI> query, long numRequired) {
    this.query = query.cloneQuery().iterator();
    this.numRequired = numRequired;
    this.counter = 0;
  }

  @Override
  public boolean hasNext() {
    return counter < numRequired;
  }

  @Override
  public ROI next() {
    if(hasNext()){
      counter++;
      return synthesise(query.next());
    }
    return null;
  }

  private ROI synthesise(ROI roi) {
    
  }

}
