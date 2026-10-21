package campus.foundly.common;

public class ApiException extends RuntimeException {
  public final int status;

  public ApiException(int status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException bad(String message) {
    return new ApiException(400, message);
  }

  public static ApiException forbidden() {
    return new ApiException(403, "You do not have permission for this action.");
  }
}
