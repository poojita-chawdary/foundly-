package campus.foundly.report;

import campus.foundly.common.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Local-only adapter for development; production uses Supabase Storage. */
@Component
@Profile("!supabase")
public class LocalImageStorage implements ImageStorage {
  private final Path root;

  public LocalImageStorage(@Value("${foundly.uploads:./data/images}") String root)
      throws IOException {
    this.root = Path.of(root).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  @Override
  public void put(String key, byte[] content) throws IOException {
    Files.write(resolve(key), content);
  }

  @Override
  public byte[] get(String key) throws IOException {
    Path path = resolve(key);
    if (!Files.exists(path)) throw new ApiException(404, "Image not found.");
    return Files.readAllBytes(path);
  }

  @Override
  public void delete(String key) throws IOException {
    Files.deleteIfExists(resolve(key));
  }

  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root) || !Path.of(key).getFileName().toString().equals(key))
      throw new ApiException(404, "Image not found.");
    return path;
  }
}
