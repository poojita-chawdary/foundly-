package campus.foundly.report;

import java.io.IOException;

/** Private image object storage used by report images. */
public interface ImageStorage {
  void put(String key, byte[] content) throws IOException;

  byte[] get(String key) throws IOException;

  void delete(String key) throws IOException;
}
