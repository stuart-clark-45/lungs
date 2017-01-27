package util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.ConfigurationMap;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Stuart Clark
 */
public class ConfigHelperTest {

  private ConfigurationMap props;

  @Before
  public void setUp() throws Exception {
    props = ConfigHelper.getProps();
  }

  @Test
  public void testGetBoolean() throws Exception {
    String testing123 = "testing123";
    String testing456 = "testing456";

    props.put(testing123, "true");
    props.put(testing456, "false");

    assertTrue(ConfigHelper.getBoolean(testing123));
    assertFalse(ConfigHelper.getBoolean(testing456));
  }

  @Test(expected = IllegalStateException.class)
  public void testBadValue() throws Exception {
    String testing123 = "testing123";

    props.put(testing123, "not true or false");

    ConfigHelper.getBoolean(testing123);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoKey() throws Exception {
    ConfigHelper.getBoolean("not added");
  }

}
