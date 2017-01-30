package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * A stack of images for a single CT scan.
 *
 * @author Stuart Clark
 */
@Entity
public class CTStack {

  @Id
  private ObjectId id;

  private List<CTSlice> slices;

  public CTStack() {
    slices = new ArrayList<>();
  }

  public ObjectId getId() {
    return id;
  }

  public List<CTSlice> getSlices() {
    return slices;
  }

  public void setSlices(List<CTSlice> slices) {
    this.slices = slices;
  }

  public void addSlice(CTSlice mi) {
    slices.add(mi);
  }

  public List<String> getPaths() {
    return slices.stream().map(CTSlice::getFilePath).collect(Collectors.toList());
  }

}
