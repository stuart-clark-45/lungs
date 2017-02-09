package optimise;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.jenetics.Gene;
import org.jenetics.Genotype;
import org.jenetics.Population;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.util.Factory;
import org.jenetics.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Should be extended by all optimisers.
 *
 * @param <G> the type of gene being used i.e {@link org.jenetics.IntegerGene}.
 * @param <E> the type used as the result for the evaluation function i.e. {@link Double}.
 * @author Stuart Clark
 */
public abstract class Optimiser<G extends Gene<?, G>, E extends Comparable<? super E>> implements
    Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Optimiser.class);
  private final Engine<G, E> engine;

  /**
   * The current population.
   */
  private Population<G, E> population;

  /**
   * The maximum number of generations that should be used.
   */
  private final int generations;

  /**
   * The maximum number of times deltaFitness can be 0 before the GA is stopped.
   */
  private int stagnationLimit;

  /**
   * @param popSize The initial population size.
   * @param generations The maximum number of generations that should be used.
   *        stopped.
   */
  public Optimiser(int popSize, int generations) {
    this.generations = generations;
    this.stagnationLimit = Integer.MAX_VALUE;

    // Create the execution environment
    this.engine = Engine.builder(this::eval, factory()).populationSize(popSize).build();
    LOGGER.info("Population size of: " + this.engine.getPopulationSize());
  }

  public void setStagnationLimit(int stagnationLimit) {
    this.stagnationLimit = stagnationLimit;
  }

  public void savePopulation() {
    try {
      IO.jaxb.write(population, new File(populationFile()));
      LOGGER.info("Saved population to " + populationFile());
    } catch (IOException e) {
      LOGGER.error("Failed to save population to " + populationFile());
    }
  }

  @SuppressWarnings("unchecked")
  public void loadPopulation() throws IOException {
    final File file = new File(populationFile());
    if (file.exists()) {
      this.population = (Population<G, E>) (IO.jaxb.read(file));
      LOGGER.info("Loaded population from " + populationFile());
    } else {
      LOGGER.info("There is no population to load will generate a new one");
    }
  }

  /**
   * Run the GA and print the results
   */
  @SuppressWarnings("ConstantConditions")
  public void run() {
    LOGGER.info("Running " + name() + "...");

    // Save population even if optimiser interrupted
    Runtime.getRuntime().addShutdownHook(new Thread(this::savePopulation));

    // Load population or create new one
    Iterator<EvolutionResult<G, E>> iterator;
    if (population != null) {
      iterator = engine.iterator(population);
    } else {
      iterator = engine.iterator();
    }

    // Hold the fitness of the previous generation (only used if {@code fitness} is numerical)
    double lastFitness = 0.0;
    // Used to count the number of generations that there has been no change in fitness
    int stagnation = 0;
    // Used to count the number of generations
    int counter = 0;

    // Run the GA
    while (stagnation < stagnationLimit && ++counter <= generations) {
      // Process generation
      EvolutionResult<G, E> result = iterator.next();
      population = result.getPopulation();

      // Check if stagnating
      E fitness = result.getBestFitness();
      if (fitness.equals(lastFitness)) {
        stagnation++;
      } else {
        stagnation = 0;
      }

      // Logging
      StringBuilder sb = new StringBuilder();
      sb.append("Generation ").append(counter).append("/").append(generations)
          .append(" complete with best fitness of: ").append(String.valueOf(fitness));
      if (fitness instanceof Number) {
        double fit = ((Number) fitness).doubleValue();
        sb.append(" and delta fitness of: ").append(fit - lastFitness);
        lastFitness = fit;
      }
      LOGGER.info(sb.toString());
      Genotype<G> gt = result.getBestPhenotype().getGenotype();
      LOGGER.info("\n" + gtToString(gt));
    }

    LOGGER.info(name() + " Finished");
    Toolkit.getDefaultToolkit().beep();
  }

  /**
   * @return the name of the optimiser.
   */
  protected abstract String name();

  /**
   * @return the name of the file that should be used to save and load the population.
   */
  protected abstract String populationFile();

  /**
   * @return the genotype factory that should be used.
   */
  protected abstract Factory<Genotype<G>> factory();

  /**
   * @param gt
   * @return the fitness for {@code gt} and return it.
   */
  protected abstract E eval(Genotype<G> gt);

  /**
   * @param gt
   * @return a string representation of {@code gt}. It may be a good idea to do this in such a way
   *         that it can be pasted straight into application.conf.
   */
  protected abstract String gtToString(Genotype<G> gt);
}
