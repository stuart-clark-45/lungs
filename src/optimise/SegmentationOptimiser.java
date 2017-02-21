//package optimise;
//
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.jenetics.Genotype;
//import org.jenetics.IntegerChromosome;
//import org.jenetics.IntegerGene;
//import org.jenetics.util.Factory;
//import org.mongodb.morphia.Datastore;
//import org.mongodb.morphia.query.FindOptions;
//import org.mongodb.morphia.query.Query;
//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.core.Point;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import core.Lungs;
//import model.CTSlice;
//import model.CTStack;
//import model.GroundTruth;
//import model.ROI;
//import util.DataFilter;
//import util.LungsException;
//import util.MatUtils;
//import util.MongoHelper;
//import vision.Matcher;
//
///**
// * Used to optimise the parameters that are used to segment
// *
// * @author Stuart Clark
// */
//public class SegmentationOptimiser extends Optimiser<IntegerGene, Double> {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(SegmentationOptimiser.class);
//
//  /*
//   * Indexes in genotype for parameters to be optimised
//   */
//  private static final int SIGMA_COLOUR = 0;
//  private static final int SIGMA_SPACE = 1;
//  private static final int KERNEL_SIZE = 2;
//  private static final int THRESHOLD = 3;
//  private static final int OPENING_WIDTH = 4;
//  private static final int OPENING_HEIGHT = 5;
//  private static final int OPENING_KERNEL = 6;
//
//  /**
//   * The {@link Mat}s to segment.
//   */
//  private List<Mat> mats;
//
//  /**
//   * The points list of {@link Point}s that must be included in the segmentation.
//   */
//  private List<List<GroundTruth>> groundTruths;
//
//  /**
//   * The total number of {@link GroundTruth}s used in the fitness function.
//   */
//  private int totalGTs;
//
//  /**
//   * @param generations the maximum number of generations that should be used.
//   * @param numStacks the number of stacks to use to obtain images for segmentation evaluation.
//   * @param readingNumber The reading number that should be used when selecting ground truths. See
//   *        documentation at {@link GroundTruth#readingNumber}.
//   */
//  public SegmentationOptimiser(int popSize, int generations, int numStacks, int readingNumber) {
//    super(popSize, generations);
//    this.mats = new ArrayList<>();
//    this.groundTruths = new ArrayList<>();
//
//    // Load some stacks
//    Datastore ds = MongoHelper.getDataStore();
//    Query<CTStack> query = DataFilter.get().all(ds.createQuery(CTStack.class));
//    List<CTStack> stacks = query.asList(new FindOptions().limit(numStacks));
//
//    // For each slice in all the stacks
//    for (CTStack stack : stacks) {
//      for (CTSlice slice : stack.getSlices()) {
//
//        // Find all the first readings for the slice that contain a nodule. Only the first reading
//        // is used as we need to know exactingly how many nodules there are in the set of Mats we
//        // will use
//        List<GroundTruth> gtList =
//            ds.createQuery(GroundTruth.class).field("type").equal(GroundTruth.Type.BIG_NODULE)
//                .field("imageSopUID").equal(slice.getImageSopUID()).field("readingNumber")
//                .equal(readingNumber).asList();
//
//        // If there are nodules in the slice
//        if (!gtList.isEmpty()) {
//
//          // Add Mat for slice into list that will be used in eval(..)
//          mats.add(MatUtils.getSliceMat(slice));
//
//          // Add gtLists to list that will be used in eval(..)
//          groundTruths.add(gtList);
//          totalGTs += gtList.size();
//        }
//
//      }
//    }
//
//    LOGGER.info(mats.size() + " Mats will be used in eval(..)");
//  }
//
//
//  /**
//   * Determine the fitness of {@code gt} by calculating how accurately is segments the
//   *
//   * @param gt
//   * @return the fitness of {@code gt}.
//   */
//  @Override
//  protected Double eval(Genotype<IntegerGene> gt) {
//    // Segment the Mats
//    Lungs lungs =
//        new Lungs(getInt(gt, SIGMA_COLOUR), getInt(gt, SIGMA_SPACE), getInt(gt, KERNEL_SIZE),
//            getInt(gt, THRESHOLD), getInt(gt, OPENING_WIDTH), getInt(gt, OPENING_HEIGHT), getInt(
//                gt, OPENING_KERNEL));
//    List<Mat> segmented = lungs.segment(mats);
//
//    // Extract the ROIs for the mats. Each sublist contains all the ROIs for the corresponding Mat
//    // in segmented
//    List<List<ROI>> allROIs = new ArrayList<>();
//    int numROIs = 0;
//    for (Mat mat : segmented) {
//      List<ROI> rois;
//      try {
//        rois = lungs.extractRois(mat);
//        allROIs.add(rois);
//        numROIs += rois.size();
//      } catch (LungsException e) {
//        throw new IllegalStateException("Failed to extract ROIs", e);
//      }
//    }
//
//    double inclusion = noduleInclusion(allROIs);
//
//    double fitness = inclusion;
//    if (inclusion > 0.7) {
//      fitness += Double.MAX_VALUE / 2;
//      fitness -= numROIs;
//    }
//
//    return fitness;
//  }
//
//  /**
//   * @param allROIs a list of lists of {@link ROI}s each sublist should be all of the {@link ROI}s
//   *        for the corresponding Mat in {@code this.mats}.
//   * @return a {@code double} between 0 and 1 inclusive that indicated how well nodules were
//   *         included in the segmented Mat. 1 meaning perfect inclusion 0 meaning no inclusion at
//   *         all.
//   */
//  private double noduleInclusion(List<List<ROI>> allROIs) {
//    double noduleInclusion = 0.0;
//    for (int i = 0; i < allROIs.size(); i++) {
//      // Get ground truths for segmented mat
//      List<GroundTruth> segGts = groundTruths.get(i);
//      // Get ROIs for segmented mat
//      List<ROI> rois = allROIs.get(i);
//
//      // Find the bestScore for each of the GroundTruths using Matcher
//      for (GroundTruth segGt : segGts) {
//
//        double bestScore = 0.0;
//        for (ROI roi : rois) {
//          double score = Matcher.match(roi, segGt);
//          if (score > bestScore) {
//            bestScore = score;
//          }
//        }
//
//        // Add to noduleInclusion
//        noduleInclusion += bestScore;
//      }
//
//    }
//
//    // Normalise to value between 0 and 1 inclusive and return
//    return noduleInclusion / totalGTs;
//  }
//
//  @Override
//  protected String gtToString(Genotype<IntegerGene> gt) {
//    return "# Size of the kernel used by the bilateral filter\n"
//        + "segmentation.filter.kernelsize = "
//        + getInt(gt, KERNEL_SIZE)
//        + "\n"
//        + "# Sigma for colour used by the bilateral filter\n"
//        + "segmentation.filter.sigmacolor = "
//        + getInt(gt, SIGMA_COLOUR)
//        + "\n"
//        + "# Sigma for space used by the bilateral filter\n"
//        + "segmentation.filter.sigmaspace = "
//        + getInt(gt, SIGMA_SPACE)
//        + "\n"
//        + "# The threshold used\n"
//        + "segmentation.threshold = "
//        + getInt(gt, THRESHOLD)
//        + "\n"
//        + "# The type of kernel to use MORPH_RECT = 0, MORPH_CROSS = 1, MORPH_ELLIPSE = 2\n"
//        + "segmentation.opening.kernel = "
//        + getInt(gt, OPENING_KERNEL)
//        + "\n"
//        + "# The width of the kernel to use\n"
//        + "segmentation.opening.width = "
//        + getInt(gt, OPENING_WIDTH)
//        + "\n"
//        + "# The height of the kernel to use\n"
//        + "segmentation.opening.height = " + getInt(gt, OPENING_HEIGHT);
//  }
//
//  /**
//   * @param gt
//   * @param index
//   * @return the integer value for the chromosome at {@code index}.
//   */
//  private int getInt(Genotype<IntegerGene> gt, int index) {
//    return gt.getChromosome(index).getGene().intValue();
//  }
//
//  @Override
//  protected String name() {
//    return "Segmentation Optimiser";
//  }
//
//  @Override
//  protected String populationFile() {
//    return "seg-opt-population.xml";
//  }
//
//  @Override
//  protected Factory<Genotype<IntegerGene>> factory() {
//    return Genotype.of(
//    // Sigma Colour
//        IntegerChromosome.of(1, 10),
//        // Sigma Space
//        IntegerChromosome.of(1, 10),
//        // Kernel Size
//        IntegerChromosome.of(3, 5),
//        // Threshold
//        IntegerChromosome.of(0, 255),
//        // Opening Width
//        IntegerChromosome.of(1, 10),
//        // Opening Height
//        IntegerChromosome.of(1, 10),
//        // Opening Kernel
//        IntegerChromosome.of(1, 2));
//  }
//
//  /**
//   * @param args optionally can provide an integer that is used as the reading number
//   * @throws IOException
//   */
//  public static void main(String[] args) throws IOException {
//    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//    // Create optimiser
//    int popSize = 50;
//    int generations = 20000;
//    int numStacks = 10;
//    int readingNumber = args.length == 1 ? Integer.parseInt(args[0]) : 0;
//    SegmentationOptimiser optimiser =
//        new SegmentationOptimiser(popSize, generations, numStacks, readingNumber);
//
//    // Load the persisted population if there is one
//    // optimiser.loadPopulation();
//
//    // Run the optimiser
//    optimiser.run();
//  }
//
//}
