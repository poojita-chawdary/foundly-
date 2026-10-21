package campus.foundly.common;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** The Supabase connection secret is split before reaching connection-pool logs. */
@Configuration
@Profile("supabase")
public class SupabaseDatabaseConfig {
  @Bean
  DataSource supabaseDataSource(
      @Value("${SUPABASE_DATABASE_URL}") String connection,
      @Value("${SUPABASE_SSL_ROOT_CERT:}") String rootCertificate,
      Environment environment) {
    var parsed = parse(connection, environment.acceptsProfiles(Profiles.of("prod")));
    var pool = new HikariDataSource();
    pool.setJdbcUrl(parsed.jdbcUrl());
    pool.setUsername(parsed.username());
    pool.setPassword(parsed.password());
    pool.setDriverClassName("org.postgresql.Driver");
    pool.setSchema("foundly");
    pool.setMaximumPoolSize(5);
    pool.setMinimumIdle(1);
    pool.setConnectionTimeout(30000);
    pool.setPoolName("FoundlySupabase");
    parsed.options().forEach(pool::addDataSourceProperty);
    if (!"disable".equals(parsed.options().get("sslmode"))) {
      if (rootCertificate.isBlank()) {
        // Session poolers with a publicly trusted certificate use the JVM CA store.
        pool.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
      } else {
        pool.addDataSourceProperty("sslrootcert", rootCertificate);
      }
    }
    return pool;
  }

  // Deliberately no record: its generated toString would expose the password.
  static final class Connection {
    private final String jdbcUrl, username, password;
    private final Map<String, String> options;

    Connection(String jdbcUrl, String username, String password, Map<String, String> options) {
      this.jdbcUrl = jdbcUrl;
      this.username = username;
      this.password = password;
      this.options = Map.copyOf(options);
    }

    String jdbcUrl() { return jdbcUrl; }
    String username() { return username; }
    String password() { return password; }
    Map<String, String> options() { return options; }
  }

  static Connection parse(String value, boolean production) {
    try {
      var uri = URI.create(value);
      if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme()))
          || uri.getHost() == null || uri.getRawUserInfo() == null || uri.getFragment() != null)
        throw new IllegalArgumentException();
      var credentials = uri.getRawUserInfo().split(":", 2);
      if (credentials.length != 2 || credentials[0].isEmpty() || credentials[1].isEmpty()
          || uri.getPath() == null || !uri.getPath().matches("/[a-zA-Z0-9_-]+"))
        throw new IllegalArgumentException();
      int port = uri.getPort() == -1 ? 5432 : uri.getPort();
      if (port == 6543 || port < 1 || port > 65535)
        throw new IllegalArgumentException(); // Transaction pooling is not used for Flyway/JPA.
      var options = new LinkedHashMap<String, String>();
      options.put("sslmode", "verify-full");
      if (uri.getRawQuery() != null) {
        for (String pair : uri.getRawQuery().split("&")) {
          var parts = pair.split("=", 2);
          if (parts.length != 2 || !parts[0].equals("sslmode"))
            throw new IllegalArgumentException();
          options.put("sslmode", decode(parts[1]));
        }
      }
      if (!options.get("sslmode").equals("verify-full")
          && !(!production && options.get("sslmode").equals("disable")))
        throw new IllegalArgumentException();
      return new Connection("jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath(),
          decode(credentials[0]), decode(credentials[1]), options);
    } catch (RuntimeException ignored) {
      // URI exception messages can contain the full secret; never chain them.
      throw new IllegalArgumentException(
          "Invalid SUPABASE_DATABASE_URL. Use a percent-encoded Postgres session connection "
              + "on port 5432 with sslmode=verify-full (disable is allowed only outside prod).");
    }
  }

  private static String decode(String value) {
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }
}
