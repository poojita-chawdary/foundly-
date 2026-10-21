package campus.foundly.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SupabaseDatabaseConfigTest {
  @Test
  void separatesCredentialsAndPreservesEncodedPassword() {
    var c = SupabaseDatabaseConfig.parse(
        "postgresql://postgres.project:p%40ss%3Aword+value@pooler.supabase.com:5432/postgres", true);
    assertEquals("postgres.project", c.username());
    assertEquals("p@ss:word+value", c.password());
    assertEquals("jdbc:postgresql://pooler.supabase.com:5432/postgres", c.jdbcUrl());
    assertEquals("verify-full", c.options().get("sslmode"));
    assertFalse(c.toString().contains("p@ss"));
  }

  @Test
  void productionRejectsInsecureTlsAndTransactionPooler() {
    assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseConfig.parse(
        "postgres://user:secret@host:5432/postgres?sslmode=disable", true));
    assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseConfig.parse(
        "postgres://user:secret@host:5432/postgres?sslmode=require", true));
    assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseConfig.parse(
        "postgres://user:secret@host:6543/postgres", true));
  }

  @Test
  void errorsDoNotExposeSecretsAndOptionsCannotOverrideCredentials() {
    var e = assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseConfig.parse(
        "postgresql://user:super secret@host/postgres", true));
    assertFalse(e.getMessage().contains("super secret"));
    assertNull(e.getCause());
    assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseConfig.parse(
        "postgresql://user:secret@host/postgres?user=other", true));
  }

  @Test
  void localPostgresCanRunWithoutTls() {
    var c = SupabaseDatabaseConfig.parse(
        "postgresql://user:secret@localhost:15432/foundly?sslmode=disable", false);
    assertEquals("disable", c.options().get("sslmode"));
  }
}
