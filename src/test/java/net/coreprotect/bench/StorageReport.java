package net.coreprotect.bench;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reports where a CoreProtect SQLite database's bytes have gone.
 *
 * Run with: ./gradlew dbreport -Pdb=/path/to/database.db
 *
 * Uses SQLite's dbstat virtual table for exact per-table and per-index page
 * usage, so the numbers add up to the file rather than being estimated from row
 * counts. Opens the database query-only, but it still reads every page: point it
 * at a copy rather than the file a running server has open.
 */
public final class StorageReport {

   /** Serialised bytes of an empty java.util.ArrayList -- the wasted-blob signature. */
   private static final byte[] EMPTY_LIST_BYTES = emptyListBlob();
   private static final int EMPTY_LIST_BLOB = EMPTY_LIST_BYTES.length;
   /** The same bytes as a SQLite blob literal, so the cleanup matches exactly. */
   private static final String EMPTY_LIST_HEX = hex(EMPTY_LIST_BYTES);

   private StorageReport() {
   }

   public static void main(String[] args) throws Exception {
      String path = System.getProperty("db.path", args.length > 0 ? args[0] : "");
      if (path.isEmpty()) {
         System.out.println("Usage: ./gradlew dbreport -Pdb=/path/to/database.db");
         return;
      }

      File file = new File(path);
      if (!file.isFile()) {
         System.out.println("No such file: " + file.getAbsolutePath());
         return;
      }

      Class.forName("org.sqlite.JDBC");
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

      try {
         Statement statement = connection.createStatement();
         statement.execute("PRAGMA query_only=1");
         statement.close();

         header(file);
         overview(connection, file);
         objects(connection);
         tables(connection);
         blobs(connection);
         advice(connection);
      } finally {
         connection.close();
      }
   }

   private static void header(File file) {
      System.out.println();
      System.out.println("CoreProtect storage report");
      System.out.println("  " + file.getAbsolutePath());
      System.out.println();
   }

   private static void overview(Connection connection, File file) throws SQLException {
      long pageSize = scalar(connection, "PRAGMA page_size");
      long pageCount = scalar(connection, "PRAGMA page_count");
      long freelist = scalar(connection, "PRAGMA freelist_count");

      System.out.println(String.format(Locale.ROOT, "%-26s %14s", "file size", bytes(file.length())));
      System.out.println(String.format(Locale.ROOT, "%-26s %14s", "page size", pageSize + " B"));
      System.out.println(String.format(Locale.ROOT, "%-26s %14s", "pages", String.format(Locale.ROOT, "%,d", pageCount)));
      System.out.println(String.format(Locale.ROOT, "%-26s %14s   %s", "free pages", String.format(Locale.ROOT, "%,d", freelist), "(" + bytes(freelist * pageSize) + " reclaimable by VACUUM)"));
      System.out.println();
   }

   /** Exact page usage per table and per index. */
   private static void objects(Connection connection) throws SQLException {
      Map<String, Long> sizes = new LinkedHashMap<>();
      long total = 0L;

      try {
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT name, SUM(pgsize) AS bytes FROM dbstat GROUP BY name ORDER BY bytes DESC");

         while(rs.next()) {
            long size = rs.getLong("bytes");
            sizes.put(rs.getString("name"), size);
            total += size;
         }

         rs.close();
         statement.close();
      } catch (SQLException e) {
         System.out.println("dbstat is not available in this SQLite build; skipping the exact breakdown.");
         System.out.println();
         return;
      }

      // Which of these names are indexes rather than tables.
      List<String> indexes = new ArrayList<>();
      Statement statement = connection.createStatement();
      ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='index'");

      while(rs.next()) {
         indexes.add(rs.getString("name"));
      }

      rs.close();
      statement.close();

      System.out.println("Where the bytes are");
      System.out.println(rule());
      System.out.println(String.format(Locale.ROOT, "%-34s %6s %14s %8s", "object", "kind", "size", "share"));
      System.out.println(rule());

      long tableBytes = 0L;
      long indexBytes = 0L;

      for(Map.Entry<String, Long> entry : sizes.entrySet()) {
         boolean isIndex = indexes.contains(entry.getKey());
         if (isIndex) {
            indexBytes += entry.getValue();
         } else {
            tableBytes += entry.getValue();
         }

         if (entry.getValue() * 200L >= total) {
            System.out.println(String.format(Locale.ROOT, "%-34s %6s %14s %7.1f%%", entry.getKey(), isIndex ? "index" : "table", bytes(entry.getValue()), 100.0D * entry.getValue() / total));
         }
      }

      System.out.println(rule());
      System.out.println(String.format(Locale.ROOT, "%-34s %6s %14s %7.1f%%", "all tables", "", bytes(tableBytes), 100.0D * tableBytes / total));
      System.out.println(String.format(Locale.ROOT, "%-34s %6s %14s %7.1f%%", "all indexes", "", bytes(indexBytes), 100.0D * indexBytes / total));
      System.out.println();
      System.out.println("Objects under 0.5% are omitted.");
      System.out.println();
   }

   /** Row counts and the age span of each log table. */
   private static void tables(Connection connection) throws SQLException {
      String[][] targets = {
         {"co_block", "time"},
         {"co_container", "time"},
         {"co_chat", "time"},
         {"co_command", "time"},
         {"co_session", "time"},
         {"co_sign", "time"},
         {"co_skull", "time"},
         {"co_entity", "time"},
         {"co_user", "time"},
         {"co_username_log", "time"}
      };

      System.out.println("Rows and age");
      System.out.println(rule());
      System.out.println(String.format(Locale.ROOT, "%-20s %14s %14s %16s", "table", "rows", "oldest", "newest"));
      System.out.println(rule());

      for(String[] target : targets) {
         try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) c, MIN(" + target[1] + ") lo, MAX(" + target[1] + ") hi FROM " + target[0]);

            if (rs.next() && rs.getLong("c") > 0L) {
               long count = rs.getLong("c");
               long lo = rs.getLong("lo");
               long hi = rs.getLong("hi");
               System.out.println(String.format(Locale.ROOT, "%-20s %14s %14s %16s", target[0], String.format(Locale.ROOT, "%,d", count), age(lo), age(hi)));
            }

            rs.close();
            statement.close();
         } catch (SQLException ignored) {
            // Table absent on this install.
         }
      }

      System.out.println();
   }

   /** How much of the file is metadata blobs, and how much of that is boilerplate. */
   private static void blobs(Connection connection) throws SQLException {
      System.out.println("Metadata blobs");
      System.out.println(rule());

      blobColumn(connection, "co_block", "meta");
      blobColumn(connection, "co_container", "metadata");

      System.out.println();
   }

   private static void blobColumn(Connection connection, String table, String column) {
      try {
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT COUNT(*) rows_total, SUM(CASE WHEN " + column + " IS NULL THEN 1 ELSE 0 END) nulls, SUM(COALESCE(LENGTH(" + column + "), 0)) total_bytes, SUM(CASE WHEN LENGTH(" + column + ") = " + EMPTY_LIST_BLOB + " THEN 1 ELSE 0 END) empties FROM " + table);

         if (rs.next()) {
            long rowsTotal = rs.getLong("rows_total");
            long nulls = rs.getLong("nulls");
            long totalBytes = rs.getLong("total_bytes");
            long empties = rs.getLong("empties");

            if (rowsTotal > 0L) {
               System.out.println(String.format(Locale.ROOT, "%s.%s", table, column));
               System.out.println(String.format(Locale.ROOT, "   %-32s %14s", "rows", String.format(Locale.ROOT, "%,d", rowsTotal)));
               System.out.println(String.format(Locale.ROOT, "   %-32s %14s", "blob bytes stored", bytes(totalBytes)));
               System.out.println(String.format(Locale.ROOT, "   %-32s %14s", "rows with no blob (NULL)", String.format(Locale.ROOT, "%,d", nulls)));
               System.out.println(String.format(Locale.ROOT, "   %-32s %14s   %s", "rows storing an empty list", String.format(Locale.ROOT, "%,d", empties), empties > 0L ? "<-- " + bytes(empties * EMPTY_LIST_BLOB) + " of pure overhead" : ""));
            }
         }

         rs.close();
         statement.close();
      } catch (SQLException ignored) {
         // Table absent on this install.
      }
   }

   private static void advice(Connection connection) throws SQLException {
      long pageSize = scalar(connection, "PRAGMA page_size");
      long freelist = scalar(connection, "PRAGMA freelist_count");

      System.out.println("What to do about it");
      System.out.println(rule());

      if (freelist * pageSize > 50L * 1024L * 1024L) {
         System.out.println(" * VACUUM would return " + bytes(freelist * pageSize) + " of already-free space to the filesystem.");
      }

      long empties = 0L;

      try {
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT SUM(CASE WHEN LENGTH(metadata) = " + EMPTY_LIST_BLOB + " THEN 1 ELSE 0 END) e FROM co_container");

         if (rs.next()) {
            empties = rs.getLong("e");
         }

         rs.close();
         statement.close();
      } catch (SQLException ignored) {
         // No container table.
      }

      if (empties > 0L) {
         System.out.println(" * " + String.format(Locale.ROOT, "%,d", empties) + " container rows store a serialised empty list. Setting them to NULL");
         System.out.println("   frees " + bytes(empties * EMPTY_LIST_BLOB) + ", which VACUUM then returns to the filesystem.");
         System.out.println("   Stop the server, back the file up, then run against a copy:");
         System.out.println();
         System.out.println("     UPDATE co_container SET metadata = NULL WHERE metadata = x'" + EMPTY_LIST_HEX + "';");
         System.out.println("     VACUUM;");
         System.out.println();
         System.out.println("   The literal is the exact byte sequence of a serialised empty ArrayList,");
         System.out.println("   so it cannot match an item that really carries metadata.");
         System.out.println();
      }

      System.out.println(" * Trim old data with /co purge t:<age>, then VACUUM. Purge rewrites the");
      System.out.println("   tables it keeps, so it reclaims space on its own.");
      System.out.println();
   }

   private static byte[] emptyListBlob() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.writeObject(new ArrayList<>());
         oos.flush();
         oos.close();
         return bos.toByteArray();
      } catch (Exception e) {
         return new byte[0];
      }
   }

   private static String hex(byte[] data) {
      StringBuilder builder = new StringBuilder();

      for(byte b : data) {
         builder.append(String.format(Locale.ROOT, "%02X", b));
      }

      return builder.toString();
   }

   private static long scalar(Connection connection, String query) throws SQLException {
      Statement statement = connection.createStatement();

      try {
         ResultSet rs = statement.executeQuery(query);
         long value = rs.next() ? rs.getLong(1) : 0L;
         rs.close();
         return value;
      } finally {
         statement.close();
      }
   }

   private static String age(long unix) {
      if (unix <= 0L) {
         return "-";
      }

      double days = (System.currentTimeMillis() / 1000.0D - unix) / 86400.0D;
      if (days < 1.0D) {
         return String.format(Locale.ROOT, "%.1f h ago", days * 24.0D);
      }

      return String.format(Locale.ROOT, "%.1f d ago", days);
   }

   private static String bytes(long value) {
      if (value >= 1024L * 1024L * 1024L) {
         return String.format(Locale.ROOT, "%.2f GB", value / (1024.0D * 1024.0D * 1024.0D));
      }

      if (value >= 1024L * 1024L) {
         return String.format(Locale.ROOT, "%.1f MB", value / (1024.0D * 1024.0D));
      }

      if (value >= 1024L) {
         return String.format(Locale.ROOT, "%.1f KB", value / 1024.0D);
      }

      return value + " B";
   }

   private static String rule() {
      StringBuilder builder = new StringBuilder();

      for(int i = 0; i < 66; ++i) {
         builder.append('-');
      }

      return builder.toString();
   }
}
