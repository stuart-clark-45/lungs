package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.ConfigurationMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import config.Mode;

/**
 * @author Stuart Clark
 */
@RunWith(Testing.class)
public class ConfigHelperTest {

  private static final String TESTING123 = "testing123";
  private static final String TESTING456 = "testing456";

  private ConfigurationMap props;

  @Before
  public void setUp() throws Exception {
    props = ConfigHelper.getProps();
  }

  @After
  public void tearDown() throws Exception {
    props.remove(TESTING123);
    props.remove(TESTING456);
  }

  @Test
  public void testGetBoolean() throws Exception {
    props.put(TESTING123, "true");
    props.put(TESTING456, "false");

    assertTrue(ConfigHelper.getBoolean(TESTING123));
    assertFalse(ConfigHelper.getBoolean(TESTING456));
  }

  @Test
  public void testGetDouble() throws Exception {
    double value = 0.5;
    props.put(TESTING123, String.valueOf(value));
    assertEquals(Double.valueOf(value), Double.valueOf(ConfigHelper.getDouble(TESTING123)));
  }

  @Test
  public void testGetInt() throws Exception {
    int value = 1;
    props.put(TESTING123,  String.valueOf(value));
    assertEquals(Integer.valueOf(value), Integer.valueOf(ConfigHelper.getInt(TESTING123)));
  }

  @Test(expected = IllegalStateException.class)
  public void testBadGetBoolean() throws Exception {
    props.put(TESTING123, "not true or false");
    ConfigHelper.getBoolean(TESTING123);
  }

  @Test
  public void testGetString() throws Exception {
    String value = "value";
    props.put(TESTING123, value);
    assertEquals(value, ConfigHelper.getString(TESTING123));
  }

  @Test
  public void testGetMode() throws Exception {
    assertEquals(Mode.VALUE.TEST, ConfigHelper.getMode());
  }

  @Test(expected = IllegalStateException.class)
  public void testNoKey() throws Exception {
    ConfigHelper.getBoolean("not added");
  }

}
