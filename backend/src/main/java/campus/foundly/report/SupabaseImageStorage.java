package campus.foundly.report;

import campus.foundly.common.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/** Stores private PNG objects in Supabase Storage; the service-role key stays on the backend. */
@Component
@Profile("supabase")
public class SupabaseImageStorage implements ImageStorage {
  private final RestClient client;
  private final String bucket;

  public SupabaseImageStorage(
      RestClient.Builder builder,
      @Value("${SUPABASE_URL}") String supabaseUrl,
      @Value("${SUPABASE_SERVICE_ROLE_KEY}") String serviceRoleKey,
      @Value("${SUPABASE_STORAGE_BUCKET:foundly-images}") String bucket) {
    if (!supabaseUrl.startsWith("https://") || serviceRoleKey.isBlank()
        || !bucket.matches("[a-z0-9][a-z0-9_-]{1,61}[a-z0-9]"))
      throw new IllegalArgumentException("Invalid Supabase Storage configuration.");
    this.bucket = bucket;
    this.client = builder
        .baseUrl(supabaseUrl.replaceAll("/+$", "") + "/storage/v1")
        .defaultHeader("apikey", serviceRoleKey)
        .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
        .build();
  }

  @Override
  public void put(String key, byte[] content) throws IOException {
    try {
      client.post()
          .uri("/object/{bucket}/{key}", bucket, safeKey(key))
          .contentType(MediaType.IMAGE_PNG)
          .header("x-upsert", "false")
          .body(content)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      throw new IOException("Could not store report image in Supabase Storage.", e);
    }
  }

  @Override
  public byte[] get(String key) throws IOException {
    try {
      byte[] result = client.get()
          .uri("/object/{bucket}/{key}", bucket, safeKey(key))
          .retrieve()
          .body(byte[].class);
      if (result == null) throw new ApiException(404, "Image not found.");
      return result;
    } catch (HttpClientErrorException e) {
      String message = e.getResponseBodyAsString();
      if (e.getStatusCode().value() == 404
          || (e.getStatusCode().value() == 400 && message.toLowerCase().contains("not found")))
        throw new ApiException(404, "Image not found.");
      throw new IOException("Could not read report image from Supabase Storage.", e);
    } catch (RestClientException e) {
      throw new IOException("Could not read report image from Supabase Storage.", e);
    }
  }

  @Override
  public void delete(String key) throws IOException {
    try {
      client.method(org.springframework.http.HttpMethod.DELETE)
          .uri("/object/{bucket}", bucket)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("prefixes", List.of(safeKey(key))))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      throw new IOException("Could not delete report image from Supabase Storage.", e);
    }
  }

  private String safeKey(String key) {
    if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.png"))
      throw new ApiException(404, "Image not found.");
    return key;
  }
}
