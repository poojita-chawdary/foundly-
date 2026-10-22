package campus.foundly.report;

import campus.foundly.common.*;
import campus.foundly.domain.*;
import campus.foundly.security.Access;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ImageService {
  private final Store store;
  private final Access access;
  private final ReportService reports;
  private final ImageStorage images;

  public ImageService(Store store, Access access, ReportService reports, ImageStorage images) {
    this.store = store;
    this.access = access;
    this.reports = reports;
    this.images = images;
  }

  public void upload(long id, MultipartFile file) throws IOException {
    var r = store.lock(ItemReport.class, id);
    access.owner(access.user(), r);
    if (!r.status.equals("ACTIVE")) throw ApiException.bad("Only active reports accept images.");
    if (file.isEmpty()
        || file.getSize() > 5 * 1024 * 1024
        || !Set.of("image/png", "image/jpeg").contains(Objects.toString(file.getContentType(), "")))
      throw ApiException.bad("Choose a PNG or JPEG image up to 5 MB.");
    java.awt.image.BufferedImage image;
    try (var in = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(in);
      if (!readers.hasNext()) throw ApiException.bad("Invalid image.");
      var reader = readers.next();
      try {
        reader.setInput(in);
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG"))
          throw ApiException.bad("Only PNG and JPEG images are supported.");
        if ((long) reader.getWidth(0) * reader.getHeight(0) > 12000000)
          throw ApiException.bad("Image dimensions are too large (12 megapixels maximum).");
        image = reader.read(0);
      } finally {
        reader.dispose();
      }
    } catch (javax.imageio.IIOException e) {
      throw ApiException.bad("The image could not be decoded.");
    }
    // Re-encoding removes EXIF metadata and ignores the supplied filename entirely.
    String key = UUID.randomUUID() + ".png";
    var content = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "png", content)) throw new IOException("PNG encoder is unavailable.");
    images.put(key, content.toByteArray());
    String old = r.imageKey;
    r.imageKey = key;
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) deleteQuietly(old);
            else deleteQuietly(key);
          }
        });
  }

  public byte[] read(long id) throws IOException {
    var r = reports.readable(id);
    if (r.imageKey == null) throw new ApiException(404, "No image.");
    return images.get(r.imageKey);
  }

  private void deleteQuietly(String key) {
    if (key == null) return;
    try {
      images.delete(key);
    } catch (IOException ignored) {
      // The database remains authoritative; a cleanup job can remove orphaned objects.
    }
  }
}
