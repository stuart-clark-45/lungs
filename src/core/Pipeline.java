package core;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import data.DataPipeline;
import ml.MLPipeline;

/**
 * Used to run the {@link data.DataPipeline} followed by the {@link ml.MLPipeline}.
 *
 * @author Stuart Clark
 */
public class Pipeline {

  public static void main(String[] args) throws ExecutionException, InterruptedException,
      IOException {

    // Run the pipelines
    DataPipeline.main(new String[0]);
    MLPipeline.main(new String[0]);

  }

}
