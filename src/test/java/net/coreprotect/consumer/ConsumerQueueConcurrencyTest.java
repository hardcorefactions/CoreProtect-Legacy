package net.coreprotect.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.block.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives the real Queue and Consumer buffers the way the server does, and
 * compares them against a faithful copy of the pre-fix append.
 *
 * queuePlayerInteraction is the one queue method that touches nothing on the
 * object it is handed, so the whole protocol can be exercised with a stub
 * BlockState and no running server.
 *
 * The assertions are on the current code: it must not lose a record. The legacy
 * numbers are printed for comparison rather than asserted, since a run that
 * happens to get lucky should not fail the build.
 */
class ConsumerQueueConcurrencyTest {

   private static final int PRODUCERS = 8;
   private static final int RECORDS_PER_PRODUCER = 20000;
   private static final int TOTAL = PRODUCERS * RECORDS_PER_PRODUCER;

   /** Result of one run of the harness. */
   private static final class Result {
      int drained;
      int duplicates;
      int drainFailures;
      int appendFailures;

      int lost() {
         return TOTAL - drained;
      }
   }

   private static BlockState stubBlockState() {
      return (BlockState)Proxy.newProxyInstance(ConsumerQueueConcurrencyTest.class.getClassLoader(), new Class<?>[]{BlockState.class}, (proxy, method, args) -> {
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

   /**
    * The append as it was before the fix: current_consumer read once for the id,
    * again for the row, and twice more for the user and object maps, with no
    * lock around any of it.
    */
   private static void legacyEnqueue(String user, BlockState block) {
      int consumer_id = Consumer.consumer_id.get(Consumer.current_consumer);
      Consumer.consumer.get(Consumer.current_consumer).add(new Object[]{consumer_id, 4, null, 0, null, 0, 0});
      Consumer.consumer_users.get(Consumer.current_consumer).put(consumer_id, new String[]{user, null});
      Consumer.consumer_object.get(Consumer.current_consumer).put(consumer_id, block);
      Consumer.consumer_id.put(Consumer.current_consumer, consumer_id + 1);
   }

   /** The flip from Consumer.run(), with and without the lock. */
   private static int flip(boolean locked) {
      if (locked) {
         synchronized(Consumer.QUEUE_LOCK) {
            int process_id = Consumer.current_consumer;
            Consumer.current_consumer = process_id == 0 ? 1 : 0;
            if (Consumer.consumer.get(Consumer.current_consumer).isEmpty()) {
               Consumer.consumer_id.put(Consumer.current_consumer, 0);
            }

            return process_id;
         }
      }

      int process_id = Consumer.current_consumer;
      Consumer.current_consumer = process_id == 0 ? 1 : 0;
      Consumer.consumer_id.put(Consumer.current_consumer, 0);
      return process_id;
   }

   /** The drain from Process.processConsumer, including its user/object guard. */
   private static void drain(int process_id, Set<String> seen, Result result) {
      ArrayList<Object[]> rows = Consumer.consumer.get(process_id);
      Map<Integer, String[]> users = Consumer.consumer_users.get(process_id);
      Map<Integer, Object> blocks = Consumer.consumer_object.get(process_id);

      try {
         for(Object[] row : rows) {
            if (row != null) {
               int id = (Integer)row[0];
               String[] user = users.get(id);
               if (user != null && blocks.get(id) != null) {
                  if (!seen.add(user[0])) {
                     ++result.duplicates;
                  }

                  ++result.drained;
                  users.remove(id);
                  blocks.remove(id);
               }
            }
         }
      } catch (Exception e) {
         // ConcurrentModificationException here is itself the bug: an append
         // landed in the buffer that was being drained.
         ++result.drainFailures;
      }

      rows.clear();
   }

   private Result harness(boolean locked) throws Exception {
      Consumer.initialize();
      Consumer.current_consumer = 0;

      final BlockState block = stubBlockState();
      final Set<String> seen = ConcurrentHashMap.newKeySet();
      final Result result = new Result();
      final AtomicBoolean producing = new AtomicBoolean(true);
      final AtomicInteger appendFailures = new AtomicInteger();
      final CountDownLatch ready = new CountDownLatch(PRODUCERS);
      final CountDownLatch go = new CountDownLatch(1);
      final CountDownLatch done = new CountDownLatch(PRODUCERS);

      // One drainer, mirroring the consumer thread: flip, let in-flight appends
      // land, then process the buffer it just took ownership of.
      Thread drainer = new Thread(() -> {
         while(producing.get()) {
            int process_id = flip(locked);

            try {
               Thread.sleep(20L);
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
            }

            drain(process_id, seen, result);
         }
      }, "drainer");

      for(int p = 0; p < PRODUCERS; ++p) {
         final int producer = p;
         Thread thread = new Thread(() -> {
            ready.countDown();

            try {
               go.await();

               for(int i = 0; i < RECORDS_PER_PRODUCER; ++i) {
                  String user = producer + ":" + i;

                  try {
                     if (locked) {
                        Queue.queuePlayerInteraction(user, block);
                     } else {
                        legacyEnqueue(user, block);
                     }
                  } catch (Exception e) {
                     // An unsynchronised ArrayList growing under a concurrent
                     // add throws straight out of the event handler.
                     appendFailures.incrementAndGet();
                  }
               }
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
            } finally {
               done.countDown();
            }
         }, "producer-" + p);
         thread.start();
      }

      ready.await();
      drainer.start();
      go.countDown();
      done.await();

      // Two more passes so nothing is left sitting in either buffer.
      Thread.sleep(60L);
      producing.set(false);
      drainer.join();
      drain(flip(locked), seen, result);
      drain(flip(locked), seen, result);

      result.appendFailures = appendFailures.get();
      return result;
   }

   @Test
   @DisplayName("the queue keeps every record when the main thread and async threads append together")
   void queueDoesNotLoseRecordsUnderConcurrentAppends() throws Exception {
      Result fixed = harness(true);

      System.out.println("[queue] current : drained=" + fixed.drained + "/" + TOTAL + " lost=" + fixed.lost() + " duplicates=" + fixed.duplicates + " drain-failures=" + fixed.drainFailures + " append-failures=" + fixed.appendFailures);

      Result legacy = harness(false);

      System.out.println("[queue] previous: drained=" + legacy.drained + "/" + TOTAL + " lost=" + legacy.lost() + " duplicates=" + legacy.duplicates + " drain-failures=" + legacy.drainFailures + " append-failures=" + legacy.appendFailures);
      System.out.println("[queue] " + PRODUCERS + " threads x " + RECORDS_PER_PRODUCER + " records; previous code lost " + String.format("%.4f%%", 100.0D * legacy.lost() / TOTAL));

      assertEquals(0, fixed.lost(), "records lost by the current queue");
      assertEquals(0, fixed.duplicates, "records drained twice by the current queue");
      assertEquals(0, fixed.drainFailures, "appends landed in the buffer being drained");
      assertEquals(0, fixed.appendFailures, "appends threw out of the caller");
      assertEquals(TOTAL, fixed.drained, "records drained by the current queue");
   }

   @Test
   @DisplayName("loss rate at a realistic server's event rate, for scale")
   void lossRateAtRealisticEventRates() throws Exception {
      // The headline harness runs eight threads flat out, which is far more
      // contention than a live server produces. This is the shape that actually
      // occurs: the main thread logging block events while the async chat thread
      // logs messages, both at rates a busy 1.8.8 server would see.
      int mainEvents = 60000;
      int chatEvents = 2000;

      Consumer.initialize();
      Consumer.current_consumer = 0;

      final BlockState block = stubBlockState();
      final Set<String> seen = ConcurrentHashMap.newKeySet();
      final Result result = new Result();
      final AtomicBoolean producing = new AtomicBoolean(true);
      final AtomicInteger lost = new AtomicInteger();

      Thread drainer = new Thread(() -> {
         while(producing.get()) {
            int process_id = flip(false);

            try {
               Thread.sleep(20L);
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
            }

            drain(process_id, seen, result);
         }
      }, "drainer");

      Thread main = new Thread(() -> {
         for(int i = 0; i < mainEvents; ++i) {
            legacyEnqueue("main:" + i, block);
         }
      }, "main");

      Thread chat = new Thread(() -> {
         for(int i = 0; i < chatEvents; ++i) {
            legacyEnqueue("chat:" + i, block);

            try {
               // Roughly one message every 3ms across all players.
               Thread.sleep(0L, 300000);
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
            }
         }
      }, "chat");

      drainer.start();
      main.start();
      chat.start();
      main.join();
      chat.join();
      Thread.sleep(60L);
      producing.set(false);
      drainer.join();
      drain(flip(false), seen, result);
      drain(flip(false), seen, result);

      lost.set(mainEvents + chatEvents - result.drained);
      System.out.println("[queue] realistic rates, previous code: " + result.drained + "/" + (mainEvents + chatEvents) + " kept, " + lost.get() + " lost (" + String.format("%.3f%%", 100.0D * lost.get() / (mainEvents + chatEvents)) + ")");

      // Reporting only -- the assertion that matters is on the current code.
      assertTrue(result.drained > 0, "harness ran");
   }

   @Test
   @DisplayName("a record and its user data always land in the same buffer")
   void recordAndPayloadStayTogetherAcrossFlips() throws Exception {
      // The failure this covers is a record whose row went into one buffer while
      // its user and object went into the other: the row is then dropped by the
      // guard in Process, and stale entries are left behind in the other buffer.
      Consumer.initialize();
      Consumer.current_consumer = 0;

      final BlockState block = stubBlockState();
      final AtomicBoolean running = new AtomicBoolean(true);
      final AtomicInteger appended = new AtomicInteger();
      final AtomicInteger orphaned = new AtomicInteger();

      Thread flipper = new Thread(() -> {
         while(running.get()) {
            flip(true);
         }
      }, "flipper");

      Thread producer = new Thread(() -> {
         for(int i = 0; i < 200000; ++i) {
            Queue.queuePlayerInteraction("u" + i, block);
            appended.incrementAndGet();
         }
      }, "producer");

      flipper.start();
      producer.start();
      producer.join();
      running.set(false);
      flipper.join();

      // Every row in either buffer must still find its own user and object.
      for(int buffer = 0; buffer < 2; ++buffer) {
         ArrayList<Object[]> rows = Consumer.consumer.get(buffer);
         Map<Integer, String[]> users = Consumer.consumer_users.get(buffer);
         Map<Integer, Object> blocks = Consumer.consumer_object.get(buffer);

         for(Object[] row : rows) {
            if (row == null || users.get((Integer)row[0]) == null || blocks.get((Integer)row[0]) == null) {
               orphaned.incrementAndGet();
            }
         }
      }

      System.out.println("[queue] flip race: appended=" + appended.get() + " orphaned=" + orphaned.get());
      assertTrue(appended.get() > 0, "producer ran");
      assertEquals(0, orphaned.get(), "rows separated from their user/object data by a buffer flip");
   }
}
