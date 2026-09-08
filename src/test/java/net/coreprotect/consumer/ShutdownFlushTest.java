package net.coreprotect.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import net.coreprotect.Functions;
import net.coreprotect.model.Config;
import org.bukkit.block.BlockState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CoreProtect.safeShutdown sets Config.server_running = false and then asks
 * Process to write out whatever is still buffered. validateConnection closes the
 * connection and declines to open another one as soon as that flag is down, so
 * the flush found connection == null, returned immediately, and the buffers went
 * away with the JVM. Every row queued in the final moments of the session was
 * lost on each restart -- including the co_world and co_material_map rows that
 * block rows reference by id, which is what makes older rows stop resolving.
 *
 * queuePlayerInteraction is used to fill the buffer because it is the one queue
 * method that touches nothing on the object it is handed, so it works against a
 * stub BlockState with no running server. The row itself cannot be written
 * without Bukkit, but resolveUserIds gives the flush a real, observable write:
 * every user named in a batch gets a co_user row. That row is the evidence the
 * flush reached the database.
 */
class ShutdownFlushTest {

   private static final String USER = "shutdown-flush-tester";

   @BeforeAll
   static void setUp() {
      // Config.sqlite is a compile-time constant, so the database has to sit at
      // the real relative path rather than in a temp directory.
      new File("plugins/CoreProtect").mkdirs();
      deleteDatabase();

      Config.config.put("use-mysql", 0);
      Config.config.put("sqlite-wal", 1);
      Config.config.put("sqlite-busy-timeout", 5000);
      Config.server_running = true;
      Config.purge_running = false;
      Config.converter_running = false;
      Consumer.is_paused = false;

      Functions.createDatabaseTables(Config.prefix, false);
   }

   @AfterAll
   static void tearDown() {
      Config.server_running = false;
      // Process holds the connection open past the flush by design, and Windows
      // will not delete a file that is still open.
      Process.closeConnection();
      deleteDatabase();
   }

   private static void deleteDatabase() {
      for (String suffix : new String[]{"", "-wal", "-shm"}) {
         new File(Config.sqlite + suffix).delete();
      }
   }

   @BeforeEach
   void freshBuffers() {
      Consumer.initialize();
      Consumer.current_consumer = 0;
      Config.player_id_cache.remove(USER.toLowerCase());
   }

   private static BlockState stubBlockState() {
      return (BlockState)Proxy.newProxyInstance(ShutdownFlushTest.class.getClassLoader(), new Class<?>[]{BlockState.class}, (proxy, method, args) -> {
         switch (method.getName()) {
            case "hashCode":
               return System.identityHashCode(proxy);
            case "equals":
               return proxy == args[0];
            case "toString":
               return "stub-block-state";
            default:
               return null;
         }
      });
   }

   private static int userRows() throws Exception {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Config.sqlite);
      Statement statement = connection.createStatement();
      ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + Config.prefix + "user WHERE user = '" + USER + "'");
      int count = rs.next() ? rs.getInt(1) : -1;
      rs.close();
      statement.close();
      connection.close();
      return count;
   }

   private static void fillBuffer(int records) {
      BlockState block = stubBlockState();

      for (int i = 0; i < records; ++i) {
         Queue.queuePlayerInteraction(USER, block);
      }

      assertEquals(records, Consumer.consumer.get(0).size(), "buffer 0 should hold the queued records");
   }

   @Test
   @DisplayName("a shutdown flush writes the buffer out after server_running is down")
   void shutdownFlushReachesTheDatabase() throws Exception {
      fillBuffer(5);
      Config.server_running = false;

      Process.processConsumer(0, true);

      assertEquals(0, Consumer.consumer.get(0).size(), "the shutdown flush must drain the buffer");
      assertEquals(1, userRows(), "the shutdown flush must reach the database");
   }

   @Test
   @DisplayName("without the shutdown flag the same flush is a no-op, which is the bug")
   void ordinaryFlushIsRefusedAfterShutdown() throws Exception {
      fillBuffer(5);
      Config.server_running = false;

      Process.processConsumer(0);

      assertEquals(5, Consumer.consumer.get(0).size(), "no connection is available, so nothing can be drained");
      assertEquals(0, userRows(), "nothing should have reached the database");

      // And the shutdown flush still recovers those same rows afterwards.
      Process.processConsumer(0, true);
      assertEquals(0, Consumer.consumer.get(0).size());
      assertTrue(userRows() > 0, "the rows the refused flush left behind must still be recoverable");
   }
}
