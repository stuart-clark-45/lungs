package config;

/**
 * The key and values for the system mode configuration variable
 *
 * @author Stuart Clark
 */
public class Mode {

  public static final String KEY = "mode";

  public enum Value {
    PROD, DEV, TEST
  }

}
