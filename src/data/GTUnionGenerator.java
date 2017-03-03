package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.GTUnion;
import model.GroundTruth;
import util.FutureMonitor;
import util.LungsException;
import util.PointUtils;
import vision.Matcher;

/**
 * Used to combine {@link GroundTruth}s in order to create {@link GTUnion}s
 *
 * @author Stuart Clark
 */
public class GTUnionGenerator extends Importer<GTUnion> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GTUnionGenerator.class);
  private static final int LOG_INTERVAL = 1000;
  private static final String IMAGE_SOP_UID = "imageSopUID";

  private final ExecutorService es;

  public GTUnionGenerator(ExecutorService es) {
    super(GTUnion.class);
    this.es = es;
  }

  @Override
  protected String testPath() {
    return null;
  }

  @Override
  protected String normalPath() {
    return null;
  }

  @Override
  protected void importModels() throws LungsException {
    LOGGER.info("GTUnionGenerator running...");

    // Get all of the slice UIDs
    List sopUIDs = ds.getCollection(GroundTruth.class).distinct(IMAGE_SOP_UID);

    // Create all the future required to combine the GroundTruths
    List<Future> futures = new ArrayList<>();
    int numSlice = sopUIDs.size();
    for (int i = 0; i < numSlice; i++) {

      // Logging
      if (i % LOG_INTERVAL == 0) {
        LOGGER.info(i + "/" + numSlice + " futures created");
      }

      // Generate all the GTUnions for the slice
      String sopUID = (String) sopUIDs.get(i);
      futures.add(es.submit(() -> {
        // TODO create test that checks that none of the groups share GroundTruths
          Set<Set<GroundTruth>> groups = getGroups(sopUID);

          // Create a GTUnion for each of the groups and save it to the database
          groups.forEach(group -> createGTUnion(group).ifPresent(ds::save));
        }));

    }

    // Monitor the futures
    FutureMonitor monitor = new FutureMonitor(futures);
    monitor.setLogString("slices processed");
    monitor.monitor();

    LOGGER.info("GTUnionGenerator finished running...");
  }

  /**
   * @param sopUID the imageSopUID for the slice the groups will be returned for.
   * @return as set of sets of {@link GroundTruth}s that are all for the same nodule.
   */
  private Set<Set<GroundTruth>> getGroups(String sopUID) {
    Set<Set<GroundTruth>> groups = new HashSet<>();
    List<GroundTruth> gts =
        ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
            .field(IMAGE_SOP_UID).equal(sopUID).asList();

    // For each of the gts
    for (int i = 0; i < gts.size(); i++) {
      GroundTruth gtI = gts.get(i);

      // Create a set of all the GroundTruths that match gts.get(i) (including gts.get(i))
      Set<GroundTruth> matched = new HashSet<>();
      matched.add(gtI);
      for (int j = 0; j < gts.size(); j++) {

        // Don't want to match gt to it's self
        if (i == j) {
          continue;
        }

        // Add gtJ to matched if it relates to the same nodule nodule as gtI
        GroundTruth gtJ = gts.get(j);
        if (Matcher.match(gtI, gtJ) > 0.1) {
          matched.add(gtJ);
        }

      }

      // Add matched to set of groups. As groups is a set there should be no duplicate groups
      groups.add(matched);
    }

    checkGroups(groups);

    return groups;
  }

  public void checkGroups(Set<Set<GroundTruth>> groups) {
    List<Set<GroundTruth>> groupList = new ArrayList<>(groups);
    for (int i = 0; i < groupList.size(); i++) {

      Set<GroundTruth> group = groupList.get(i);

      for (int j = 0; j < groupList.size(); j++) {
        // Don't want to check against self
        if (i == j) {
          continue;
        }

        for (GroundTruth groundTruth : groupList.get(j)) {
          if (group.contains(groundTruth)) {
            throw new IllegalStateException(
                "GroundTruth appears in multiple groups see slice with imageSopUID: "
                    + groundTruth.getImageSopUID());
          }
        }

      }
    }
  }

  /**
   * @param gts a set of {@link GroundTruth}s that all correspond to the same nodule and slice.
   * @return an {@code Optional.of()} a newly created {@link GTUnion} for {@code gts} provided. Or
   *         {@link Optional#empty()} if one could not be created.o
   */
  private Optional<GTUnion> createGTUnion(Set<GroundTruth> gts) {
    GTUnion gtUnion = new GTUnion();
    GroundTruth first = gts.iterator().next();

    // Set the region
    Set<Point> regionUnion = new HashSet<>();
    gts.forEach(gt -> regionUnion.addAll(gt.getRegion()));
    List<Point> regionList = new ArrayList<>(regionUnion);
    // Due to bad data some very small GroundTruths have exclusive edgePoints that contain no region
    // within them. These GroundTruths are simply ignored.
    if (regionList.isEmpty()) {
      return Optional.empty();
    }
    gtUnion.setRegion(regionList);

    // Set the edgePoints
    List<Point> edgePoints = PointUtils.region2Contour(regionList);
    gtUnion.setEdgePoints(edgePoints);

    // Set the centroid
    gtUnion.setCentroid(PointUtils.centroid(edgePoints));

    // Set other fields
    gtUnion.setImageSopUID(first.getImageSopUID());
    gtUnion.setSeriesInstanceUID(first.getSeriesInstanceUID());

    return Optional.of(gtUnion);
  }

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    new GTUnionGenerator(es).run();
  }

}
