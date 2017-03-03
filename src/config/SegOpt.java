package config;

/**
 * @author Stuart Clark
 */
public class SegOpt {

  private SegOpt() {}

  /**
   * The size of the GA should use.
   */
  public static final String POPULATION = "segopt.population";

  /**
   * The number of generations the GA should run for.
   */
  public static final String GENERATIONS = "segopt.generations";

  /**
   * The number of CT stacks that GA should use for evaluation
   */
  public static final String STACKS = "segopt.stacks";

  /**
   * True if the population should be loaded from a file, false otherwise.
   */
  public static final String LOAD_POPULATION = "segopt.loadPopulation";

}
