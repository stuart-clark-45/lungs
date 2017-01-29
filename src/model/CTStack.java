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

  private List<MedicalImage> slices;

  public CTStack() {
    slices = new ArrayList<>();
  }

  public ObjectId getId() {
    return id;
  }

  public List<MedicalImage> getSlices() {
    return slices;
  }

  public void setSlices(List<MedicalImage> slices) {
    this.slices = slices;
  }

  public void addSlice(MedicalImage mi) {
    slices.add(mi);
  }

  public List<String> getPaths() {
    return slices.stream().map(MedicalImage::getFilePath).collect(Collectors.toList());
  }

}
