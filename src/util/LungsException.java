package util;

/**
 * @author Stuart Clark
 */
public class LungsException extends Exception {

  public LungsException() {}

  public LungsException(String message) {
    super(message);
  }

  public LungsException(String message, Throwable cause) {
    super(message, cause);
  }

  public LungsException(Throwable cause) {
    super(cause);
  }

  public LungsException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
  
}
