package global;

/**
 * Structure of standard keys and values used for system configuration. All constructors are hidden.
 *
 * @author Stuart Clark
 */
public class Config {

  public static class Mode {
    private Mode() {}

    public static final String KEY = "mode";
    public static final String PROD = "PROD";
    public static final String TEST = "test";
  }

  private Config() {}

}
