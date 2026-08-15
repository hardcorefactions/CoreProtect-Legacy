package net.coreprotect.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import net.coreprotect.consumer.Consumer;
import net.coreprotect.model.Config;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opens a real, pre-existing CoreProtect database through the actual
 * Database.getConnection path and checks that the new pragmas neither reject it
 * nor change its contents.
 *
 * Skipped unless -Dcompat.db=/path/to/a/copy is supplied, because it needs a
 * populated database and it converts that file to WAL. Point it at a copy.
 */
@EnabledIfSystemProperty(named = "compat.db", matches = ".+")
class UpgradeCompatibilityTest {

   private static String path;

   @BeforeAll
   static void setUp() {
      // Config.sqlite is a static final String constant, so javac inlines it
      // into Database.getConnection and it cannot be redirected at runtime. The
      // database under test has to sit at the real path; the harness puts it
      // there before invoking this.
      path = Config.sqlite;
      Config.config.put("use-mysql", 0);
      Config.config.put("sqlite-wal", 1);
      Config.config.put("sqlite-busy-timeout", 5000);
      Config.server_running = true;
      Config.purge_running = false;
      Config.converter_running = false;
      Consumer.is_paused = false;
   }

   private static String pragma(Connection connection, String name) throws Exception {
      Statement statement = connection.createStatement();
      ResultSet rs = statement.executeQuery("PRAGMA " + name);
      String value = rs.next() ? rs.getString(1) : null;
      rs.close();
      statement.close();
      return value;
   }

   private static long scalar(Connection connection, String query) throws Exception {
      Statement statement = connection.createStatement();
      ResultSet rs = statement.executeQuery(query);
      long value = rs.next() ? rs.getLong(1) : -1L;
      rs.close();
      statement.close();
      return value;
   }

   @Test
   @DisplayName("an existing database opens, converts to WAL, and keeps every row")
   void existingDatabaseStillWorks() throws Exception {
      File file = new File(path);
      assertTrue(file.isFile(), "compat.db must point at a real file");

      Connection probe = java.sql.DriverManager.getConnection("jdbc:sqlite:" + path);
      long blocksBefore = scalar(probe, "SELECT COUNT(*) FROM co_block");
      long containersBefore = scalar(probe, "SELECT COUNT(*) FROM co_container");
      long emptyBefore = scalar(probe, "SELECT COUNT(*) FROM co_container WHERE LENGTH(metadata) = 58");
      String pageSizeBefore = pragma(probe, "page_size");
      String journalBefore = pragma(probe, "journal_mode");
      probe.close();

      System.out.println("[compat] before : journal=" + journalBefore + " page_size=" + pageSizeBefore + " blocks=" + blocksBefore + " containers=" + containersBefore + " empty-blobs=" + emptyBefore);

      // The real path the plugin uses on startup.
      Connection connection = Database.getConnection(true);
      assertNotNull(connection, "Database.getConnection returned null for an existing database");

      String journalAfter = pragma(connection, "journal_mode");
      String pageSizeAfter = pragma(connection, "page_size");
      String busy = pragma(connection, "busy_timeout");
      System.out.println("[compat] after  : journal=" + journalAfter + " page_size=" + pageSizeAfter + " busy_timeout=" + busy);

      assertEquals("wal", journalAfter.toLowerCase(), "journal_mode should have converted to WAL");
      assertEquals("5000", busy, "busy_timeout should be applied");
      // The point of this assertion: page_size CANNOT change on a populated
      // database, so an existing file keeps its 1 KB pages until someone
      // vacuums it. Upgrading alone does not shrink anything.
      assertEquals(pageSizeBefore, pageSizeAfter, "page_size must be unchanged on an existing database");

      assertEquals(blocksBefore, scalar(connection, "SELECT COUNT(*) FROM co_block"), "block rows changed");
      assertEquals(containersBefore, scalar(connection, "SELECT COUNT(*) FROM co_container"), "container rows changed");
      assertEquals(emptyBefore, scalar(connection, "SELECT COUNT(*) FROM co_container WHERE LENGTH(metadata) = 58"), "old metadata blobs were altered");

      Statement check = connection.createStatement();
      ResultSet rs = check.executeQuery("PRAGMA quick_check(1)");
      rs.next();
      String integrity = rs.getString(1);
      rs.close();
      check.close();
      System.out.println("[compat] quick_check = " + integrity);
      assertEquals("ok", integrity, "database failed its integrity check after conversion");

      connection.close();
   }

   @Test
   @DisplayName("a rollback-shaped lookup still returns rows from the old data")
   void rollbackLookupStillReturnsRows() throws Exception {
      Connection connection = Database.getConnection(true);
      assertNotNull(connection);

      Statement statement = connection.createStatement();
      ResultSet bounds = statement.executeQuery("SELECT wid, x, z, time FROM co_block ORDER BY rowid DESC LIMIT 1");
      bounds.next();
      int wid = bounds.getInt("wid");
      int x = bounds.getInt("x");
      int z = bounds.getInt("z");
      int time = bounds.getInt("time");
      bounds.close();

      // The exact query shape Lookup.rawLookupResultSet builds for a radius
      // rollback, index hint included.
      String query = "SELECT rowid as id,time,user,wid,x,y,z,action,type,data,meta,rolled_back FROM co_block INDEXED BY block_index WHERE wid=" + wid + " AND x >= '" + (x - 200) + "' AND x <= '" + (x + 200) + "' AND z >= '" + (z - 200) + "' AND z <= '" + (z + 200) + "' AND time > '" + (time - 86400) + "' ORDER BY rowid DESC";

      long start = System.nanoTime();
      ResultSet rs = statement.executeQuery(query);
      int rows = 0;

      while(rs.next()) {
         rs.getInt("id");
         rs.getBytes("meta");
         ++rows;
      }

      rs.close();
      statement.close();
      long ms = (System.nanoTime() - start) / 1000000L;
      System.out.println("[compat] rollback-shaped lookup returned " + rows + " row(s) in " + ms + "ms");
      assertTrue(rows > 0, "the rollback query returned nothing from existing data");

      connection.close();
   }

   @Test
   @DisplayName("old blob rows and new NULL rows both read back")
   void oldAndNewContainerRowsCoexist() throws Exception {
      Connection connection = Database.getConnection(true);
      assertNotNull(connection);

      // Write a row the way the fixed insertContainer now does: metadata NULL.
      PreparedStatement insert = connection.prepareStatement("INSERT INTO co_container (time, user, wid, x, y, z, type, data, amount, metadata, action, rolled_back) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      insert.setInt(1, (int)(System.currentTimeMillis() / 1000L));
      insert.setInt(2, 1);
      insert.setInt(3, 0);
      insert.setInt(4, 1234567);
      insert.setInt(5, 64);
      insert.setInt(6, 1234567);
      insert.setInt(7, 1);
      insert.setInt(8, 0);
      insert.setInt(9, 1);
      insert.setObject(10, null);
      insert.setInt(11, 0);
      insert.setInt(12, 0);
      insert.executeUpdate();
      insert.close();

      Statement statement = connection.createStatement();
      ResultSet rs = statement.executeQuery("SELECT metadata FROM co_container WHERE x = 1234567 LIMIT 1");
      rs.next();
      byte[] fresh = rs.getBytes("metadata");
      rs.close();

      ResultSet old = statement.executeQuery("SELECT metadata FROM co_container WHERE LENGTH(metadata) = 58 LIMIT 1");
      old.next();
      byte[] legacy = old.getBytes("metadata");
      old.close();

      statement.executeUpdate("DELETE FROM co_container WHERE x = 1234567");
      statement.close();

      System.out.println("[compat] new row metadata = " + (fresh == null ? "NULL" : fresh.length + " bytes") + ", legacy row metadata = " + (legacy == null ? "NULL" : legacy.length + " bytes"));
      org.junit.jupiter.api.Assertions.assertNull(fresh, "the fixed insert should store NULL");
      assertNotNull(legacy, "an existing 58-byte blob should still be readable");
      assertEquals(58, legacy.length);

      connection.close();
   }
}
