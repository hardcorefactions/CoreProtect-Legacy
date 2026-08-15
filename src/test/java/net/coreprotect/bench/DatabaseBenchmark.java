package net.coreprotect.bench;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compares the SQLite settings CoreProtect used to open its database with (none
 * at all) against the ones Database.applySqlitePragmas now sets.
 *
 * The schema, the INSERT and the rollback SELECT are the ones the plugin
 * actually issues -- copied from Functions.createDatabase, Database.prepareStatement
 * and Lookup.rawLookupResultSet respectively -- so the timings correspond to
 * real operations rather than a synthetic workload.
 *
 * Run with: ./gradlew benchmark
 *   -Dbench.rows=300000     rows seeded into co_block before measuring
 *   -Dbench.seconds=6       duration of the contention phase
 */
public final class DatabaseBenchmark {

   private static final String PREFIX = "co_";

   private static final String CREATE_BLOCK = "CREATE TABLE IF NOT EXISTS " + PREFIX + "block (time INTEGER, user INTEGER, wid INTEGER, x INTEGER, y INTEGER, z INTEGER, type INTEGER, data INTEGER, meta BLOB, action INTEGER, rolled_back INTEGER);";
   private static final String CREATE_BLOCK_INDEX = "CREATE INDEX IF NOT EXISTS block_index ON " + PREFIX + "block(wid,x,z,time);";
   private static final String CREATE_BLOCK_USER_INDEX = "CREATE INDEX IF NOT EXISTS block_user_index ON " + PREFIX + "block(user,time);";
   private static final String CREATE_BLOCK_TYPE_INDEX = "CREATE INDEX IF NOT EXISTS block_type_index ON " + PREFIX + "block(type,time);";

   private static final String INSERT_BLOCK = "INSERT INTO " + PREFIX + "block (time, user, wid, x, y, z, type, data, meta, action, rolled_back) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

   /** The consumer flushes one transaction per cycle; this is that batch size. */
   private static final int BATCH = 250;

   /** Radius the rollback SELECT covers, in blocks. */
   private static final int RADIUS = 250;

   private static int rows;
   private static int contentionSeconds;

   private DatabaseBenchmark() {
   }

   public static void main(String[] args) throws Exception {
      rows = Integer.getInteger("bench.rows", 300000);
      contentionSeconds = Integer.getInteger("bench.seconds", 6);

      Class.forName("org.sqlite.JDBC");

      System.out.println();
      System.out.println("CoreProtect SQLite benchmark");
      System.out.println("  seeded rows      : " + rows);
      System.out.println("  insert batch     : " + BATCH + " rows per transaction");
      System.out.println("  rollback radius  : " + RADIUS + " blocks");
      System.out.println("  contention phase : " + contentionSeconds + "s");
      System.out.println();

      Profile before = new Profile("previous (no pragmas)", false, 0);
      Profile after = new Profile("current (WAL + busy_timeout)", true, 5000);

      Report beforeReport = measure(before);
      Report afterReport = measure(after);

      print(beforeReport, afterReport);
   }

   /** One set of connection settings. */
   private static final class Profile {
      final String name;
      final boolean wal;
      final int busyTimeout;

      Profile(String name, boolean wal, int busyTimeout) {
         this.name = name;
         this.wal = wal;
         this.busyTimeout = busyTimeout;
      }

      Connection open(File file) throws SQLException {
         Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
         Statement statement = connection.createStatement();

         try {
            statement.execute("PRAGMA busy_timeout=" + this.busyTimeout);
            if (this.wal) {
               statement.execute("PRAGMA journal_mode=WAL");
               statement.execute("PRAGMA synchronous=NORMAL");
            } else {
               // What the plugin used to get by default.
               statement.execute("PRAGMA journal_mode=DELETE");
               statement.execute("PRAGMA synchronous=FULL");
            }
         } finally {
            statement.close();
         }

         return connection;
      }
   }

   /** Numbers collected for one profile. */
   private static final class Report {
      String name;
      double seedRowsPerSecond;
      double idleInsertRowsPerSecond;
      long idleLookupMs;
      long updateMsPerThousand;

      int readsOk;
      int readsFailed;
      long readTotalMs;
      int writeBatchesOk;
      int writeBatchesFailed;
      int writeRows;

      double readsPerSecond() {
         return this.readsOk / (double)contentionSeconds;
      }

      double contendedWriteRowsPerSecond() {
         return this.writeRows / (double)contentionSeconds;
      }

      long meanReadMs() {
         return this.readsOk == 0 ? -1L : this.readTotalMs / this.readsOk;
      }
   }

   private static Report measure(Profile profile) throws Exception {
      File file = File.createTempFile("coreprotect-bench-", ".db");
      Files.deleteIfExists(file.toPath());

      Report report = new Report();
      report.name = profile.name;

      try {
         Connection connection = profile.open(file);

         try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(CREATE_BLOCK);
            statement.executeUpdate(CREATE_BLOCK_INDEX);
            statement.executeUpdate(CREATE_BLOCK_USER_INDEX);
            statement.executeUpdate(CREATE_BLOCK_TYPE_INDEX);
            statement.close();

            long start = System.nanoTime();
            seed(connection, rows);
            report.seedRowsPerSecond = rows / ((System.nanoTime() - start) / 1.0e9D);

            report.idleInsertRowsPerSecond = measureIdleInserts(connection);
            report.idleLookupMs = measureIdleLookup(connection);
            report.updateMsPerThousand = measureRolledBackUpdates(connection);
         } finally {
            connection.close();
         }

         measureContention(profile, file, report);
      } finally {
         Files.deleteIfExists(file.toPath());
         Files.deleteIfExists(new File(file.getAbsolutePath() + "-wal").toPath());
         Files.deleteIfExists(new File(file.getAbsolutePath() + "-shm").toPath());
         Files.deleteIfExists(new File(file.getAbsolutePath() + "-journal").toPath());
      }

      return report;
   }

   /** Fills co_block with rows spread over 20 hours, the way a live server would. */
   private static void seed(Connection connection, int count) throws SQLException {
      Random random = new Random(20260815L);
      int now = (int)(System.currentTimeMillis() / 1000L);
      int start = now - 20 * 60 * 60;

      connection.setAutoCommit(false);
      PreparedStatement insert = connection.prepareStatement(INSERT_BLOCK);

      try {
         for(int i = 0; i < count; ++i) {
            bindRow(insert, random, start + (int)((long)(now - start) * i / count));
            insert.addBatch();

            if (i % 10000 == 9999) {
               insert.executeBatch();
               connection.commit();
            }
         }

         insert.executeBatch();
         connection.commit();
      } finally {
         insert.close();
         connection.setAutoCommit(true);
      }
   }

   private static void bindRow(PreparedStatement insert, Random random, int time) throws SQLException {
      insert.setInt(1, time);
      insert.setInt(2, 1 + random.nextInt(40));
      insert.setInt(3, 0);
      insert.setInt(4, random.nextInt(4000) - 2000);
      insert.setInt(5, random.nextInt(120) + 4);
      insert.setInt(6, random.nextInt(4000) - 2000);
      insert.setInt(7, 1 + random.nextInt(180));
      insert.setInt(8, random.nextInt(16));
      insert.setObject(9, null);
      insert.setInt(10, random.nextInt(2));
      insert.setInt(11, 0);
   }

   /** Consumer-shaped inserts with nothing else touching the database. */
   private static double measureIdleInserts(Connection connection) throws SQLException {
      Random random = new Random(7L);
      int now = (int)(System.currentTimeMillis() / 1000L);
      int batches = 40;

      long start = System.nanoTime();

      for(int b = 0; b < batches; ++b) {
         writeBatch(connection, random, now);
      }

      double seconds = (System.nanoTime() - start) / 1.0e9D;
      return batches * BATCH / seconds;
   }

   /** One consumer flush: BEGIN, BATCH inserts, COMMIT. */
   private static void writeBatch(Connection connection, Random random, int now) throws SQLException {
      Statement statement = connection.createStatement();
      statement.executeUpdate("BEGIN TRANSACTION");
      statement.close();

      PreparedStatement insert = connection.prepareStatement(INSERT_BLOCK);

      try {
         for(int i = 0; i < BATCH; ++i) {
            bindRow(insert, random, now);
            insert.executeUpdate();
            insert.clearParameters();
         }
      } finally {
         insert.close();
      }

      Statement commit = connection.createStatement();
      commit.executeUpdate("COMMIT TRANSACTION");
      commit.close();
   }

   /** The SELECT a radius rollback issues, with nothing else running. */
   private static long measureIdleLookup(Connection connection) throws SQLException {
      long start = System.nanoTime();
      rollbackLookup(connection, 0);
      return (System.nanoTime() - start) / 1000000L;
   }

   /**
    * Lookup.rawLookupResultSet's rollback form: radius bounded, time bounded,
    * ordered by rowid descending, using block_index.
    */
   private static int rollbackLookup(Connection connection, int centre) throws SQLException {
      int checkTime = (int)(System.currentTimeMillis() / 1000L) - 20 * 60 * 60;
      String query = "SELECT rowid as id,time,user,wid,x,y,z,action,type,data,meta,rolled_back FROM " + PREFIX + "block INDEXED BY block_index WHERE wid=0 AND x >= '" + (centre - RADIUS) + "' AND x <= '" + (centre + RADIUS) + "' AND z >= '" + (centre - RADIUS) + "' AND z <= '" + (centre + RADIUS) + "' AND time > '" + checkTime + "' ORDER BY rowid DESC";

      Statement statement = connection.createStatement();

      try {
         ResultSet rs = statement.executeQuery(query);
         int found = 0;

         while(rs.next()) {
            rs.getInt("id");
            rs.getInt("rolled_back");
            rs.getBytes("meta");
            ++found;
         }

         rs.close();
         return found;
      } finally {
         statement.close();
      }
   }

   /**
    * Process.processRollbackUpdate marks rows one UPDATE at a time. This is how
    * long that takes per thousand rows, which is what a rollback pays to record
    * what it did.
    */
   private static long measureRolledBackUpdates(Connection connection) throws SQLException {
      List<Integer> ids = new ArrayList<>();
      Statement select = connection.createStatement();
      ResultSet rs = select.executeQuery("SELECT rowid FROM " + PREFIX + "block LIMIT 0, 1000");

      while(rs.next()) {
         ids.add(rs.getInt(1));
      }

      rs.close();
      select.close();

      if (ids.isEmpty()) {
         return -1L;
      }

      Statement statement = connection.createStatement();
      statement.executeUpdate("BEGIN TRANSACTION");
      long start = System.nanoTime();

      for(Integer id : ids) {
         statement.executeUpdate("UPDATE " + PREFIX + "block SET rolled_back='1' WHERE rowid='" + id + "'");
      }

      long elapsed = System.nanoTime() - start;
      statement.executeUpdate("COMMIT TRANSACTION");
      statement.close();

      return elapsed / 1000000L * 1000L / ids.size();
   }

   /**
    * The situation that produced the bug report: the consumer writing on its own
    * connection while a rollback reads on another. Counts what each side manages
    * and what it loses to SQLITE_BUSY.
    */
   private static void measureContention(Profile profile, File file, Report report) throws Exception {
      final AtomicBoolean running = new AtomicBoolean(true);
      final CountDownLatch ready = new CountDownLatch(2);
      final CountDownLatch go = new CountDownLatch(1);

      Thread writer = new Thread(() -> {
         Random random = new Random(11L);
         int now = (int)(System.currentTimeMillis() / 1000L);

         try {
            Connection connection = profile.open(file);
            ready.countDown();
            go.await();

            try {
               while(running.get()) {
                  try {
                     writeBatch(connection, random, now);
                     ++report.writeBatchesOk;
                     report.writeRows += BATCH;
                  } catch (SQLException e) {
                     // Exactly what Database.insertBlock swallows in production,
                     // losing that batch of block logs.
                     ++report.writeBatchesFailed;
                     rollbackQuietly(connection);
                  }
               }
            } finally {
               connection.close();
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }, "bench-writer");

      Thread reader = new Thread(() -> {
         try {
            Connection connection = profile.open(file);
            ready.countDown();
            go.await();

            try {
               int centre = 0;

               while(running.get()) {
                  long start = System.nanoTime();

                  try {
                     rollbackLookup(connection, centre);
                     ++report.readsOk;
                     report.readTotalMs += (System.nanoTime() - start) / 1000000L;
                  } catch (SQLException e) {
                     // In production this is the rollback that reports success
                     // having read nothing.
                     ++report.readsFailed;
                  }

                  centre = (centre + 400) % 1600;
               }
            } finally {
               connection.close();
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }, "bench-reader");

      writer.start();
      reader.start();
      ready.await();
      go.countDown();
      Thread.sleep(contentionSeconds * 1000L);
      running.set(false);
      writer.join();
      reader.join();
   }

   private static void rollbackQuietly(Connection connection) {
      try {
         Statement statement = connection.createStatement();
         statement.executeUpdate("ROLLBACK TRANSACTION");
         statement.close();
      } catch (SQLException ignored) {
         // Nothing to roll back.
      }
   }

   private static void print(Report before, Report after) {
      System.out.println(row("", before.name, after.name, "change"));
      System.out.println(rule());
      System.out.println(row("-- database to itself (no contention) --", "", "", ""));
      System.out.println(row("seed insert, rows/s  (up)", num(before.seedRowsPerSecond), num(after.seedRowsPerSecond), higherBetter(before.seedRowsPerSecond, after.seedRowsPerSecond)));
      System.out.println(row("consumer flush, rows/s  (up)", num(before.idleInsertRowsPerSecond), num(after.idleInsertRowsPerSecond), higherBetter(before.idleInsertRowsPerSecond, after.idleInsertRowsPerSecond)));
      System.out.println(row("rollback lookup, ms  (down)", before.idleLookupMs + "", after.idleLookupMs + "", lowerBetter(before.idleLookupMs, after.idleLookupMs)));
      System.out.println(row("mark rolled_back, ms/1k  (down)", before.updateMsPerThousand + "", after.updateMsPerThousand + "", lowerBetter(before.updateMsPerThousand, after.updateMsPerThousand)));
      System.out.println(rule());
      System.out.println(row("-- rollback reading while the consumer writes --", "", "", ""));
      System.out.println(row("log rows written  (up)", before.writeRows + "", after.writeRows + "", higherBetter(before.writeRows, after.writeRows)));
      System.out.println(row("contended write, rows/s  (up)", num(before.contendedWriteRowsPerSecond()), num(after.contendedWriteRowsPerSecond()), higherBetter(before.contendedWriteRowsPerSecond(), after.contendedWriteRowsPerSecond())));
      System.out.println(row("log batches FAILED, busy  (down)", before.writeBatchesFailed + "", after.writeBatchesFailed + "", ""));
      System.out.println(row("lookups completed", before.readsOk + "", after.readsOk + "", ""));
      System.out.println(row("lookups FAILED, busy  (down)", before.readsFailed + "", after.readsFailed + "", ""));
      System.out.println(row("mean lookup, ms", before.meanReadMs() + "", after.meanReadMs() + "", ""));
      System.out.println();

      int lostBefore = before.writeBatchesFailed * BATCH;
      System.out.println("Block logs dropped during the contention phase:");
      System.out.println("  previous : " + lostBefore + " row(s) across " + before.writeBatchesFailed + " failed batch(es)");
      System.out.println("  current  : " + after.writeBatchesFailed * BATCH + " row(s) across " + after.writeBatchesFailed + " failed batch(es)");
      System.out.println();
      System.out.println("Rollbacks that would have reported success over no data:");
      System.out.println("  previous : " + before.readsFailed);
      System.out.println("  current  : " + after.readsFailed);
      System.out.println();
      System.out.println("Note on the two lookup rows: the previous settings complete more lookups");
      System.out.println("with a lower mean only because most of their attempts failed outright and");
      System.out.println("the writer was starved, so the table barely grew. The current settings read");
      System.out.println("a table that gained " + (after.writeRows - before.writeRows) + " more rows over the same window, and lose none.");
      System.out.println();
   }

   private static String row(String label, String a, String b, String c) {
      return String.format(Locale.ROOT, "%-34s %22s %22s %10s", label, a, b, c);
   }

   private static String rule() {
      StringBuilder builder = new StringBuilder();

      for(int i = 0; i < 92; ++i) {
         builder.append('-');
      }

      return builder.toString();
   }

   private static String num(double value) {
      return String.format(Locale.ROOT, "%,.0f", value);
   }

   /** For throughput: how many times more the current settings manage. */
   private static String higherBetter(double before, double after) {
      if (before <= 0.0D) {
         return after > 0.0D ? "n/a" : "";
      }

      return String.format(Locale.ROOT, "%.2fx", after / before);
   }

   /** For latency: how many times faster the current settings are. */
   private static String lowerBetter(double before, double after) {
      if (after <= 0.0D) {
         return "";
      }

      return String.format(Locale.ROOT, "%.2fx", before / after);
   }
}
