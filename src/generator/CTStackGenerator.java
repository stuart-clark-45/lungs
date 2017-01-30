package generator;

import java.util.Comparator;
import java.util.List;

import org.mongodb.morphia.Datastore;

import com.mongodb.DBCollection;

import model.CTStack;
import model.CTSlice;
import util.MongoHelper;

/**
 * Generate {@link CTStack}s for each of the series of images that are present in the
 * {@link CTSlice} collection.
 *
 * @author Stuart Clark
 */
public class CTStackGenerator implements Runnable {

  private Datastore ds;

  public CTStackGenerator() {
    ds = MongoHelper.getDataStore();
  }

  @Override
  public void run() {
    // Drop collection and indexes
    DBCollection collection = ds.getCollection(CTStack.class);
    collection.drop();
    collection.dropIndexes();

    // Get each series instance UID
    List uids = ds.getCollection(CTSlice.class).distinct("seriesInstanceUID");
    for (Object o : uids) {
      String uid = (String) o;

      // Get all the slices for the stack
      List<CTSlice> images =
          ds.createQuery(CTSlice.class).field("seriesInstanceUID").equal(uid).asList();

      // Sort them and create the stack
      CTStack stack = new CTStack();
      images.stream().sorted(Comparator.comparingInt(CTSlice::getImageNumber))
          .forEach(stack::addSlice);

      ds.save(stack);
    }

    // index collection
    ds.ensureIndexes(CTStack.class);
  }

  public static void main(String[] args) throws Exception {
    new CTStackGenerator().run();
  }

}
