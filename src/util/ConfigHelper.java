package util;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.PropertiesConfiguration;

import config.Mode;

/**
 * Helper class used to make a single isntance of the configuration globally accessible.
 *
 * @author Stuart Clark
 */
public class ConfigHelper {

  private static final String FILE_NAME = "application.conf";

  /**
   * Used {@link ConfigHelper#getProps()} never this field directly.
   */
  private static ConfigurationMap props;

  /**
   * @return a map of keys to values originally loaded from {@code FILE_NAME}.
   */
  public static ConfigurationMap getProps() {
    if (props == null) {
      try {
        File file = new File(ClassLoader.getSystemResource(FILE_NAME).toURI());
        Configuration config = new PropertiesConfiguration(file);
        props = new ConfigurationMap(config);
      } catch (URISyntaxException | ConfigurationException e) {
        throw new IllegalStateException("Failed to load configuration");
      }
    }
    return props;
  }

  /**
   * @param key a key with a boolean value.
   * @return the value for the key.
   * @throws IllegalStateException if key not present or value not a boolean
   */
  public static boolean getBoolean(String key) {
    // Check the key
    assertKeySet(key);

    String value = (String) getProps().get(key);

    // Check the value
    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
      throw new IllegalStateException("The value for " + key + " must be either true or false");
    }

    return Boolean.parseBoolean(value);
  }

  /**
   * @param key a key with a string value.
   * @return the value for the key.
   * @throws IllegalStateException if key not present.
   */
  public static String getString(String key) {
    // Check the key
    assertKeySet(key);

    return (String) getProps().get(key);
  }

  /**
   * @return the current system mode.
   */
  public static Mode.VALUE getMode() {
    String s = (String) getProps().get(Mode.KEY);
    return Mode.VALUE.valueOf(s);
  }

  /**
   * Check if the key is set, throw {@link IllegalStateException} if it is not.
   *
   * @param key
   */
  private static void assertKeySet(String key) {
    if (!getProps().containsKey(key)) {
      throw new IllegalStateException(key + " must be set in " + FILE_NAME);
    }
  }

}
