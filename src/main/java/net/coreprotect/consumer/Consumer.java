package net.coreprotect.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.coreprotect.model.Config;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public class Consumer implements Runnable, Thread.UncaughtExceptionHandler {
   // These flags are written by one thread and spun on by another (the consumer
   // thread, the async rollback thread and the main thread all participate).
   // Without volatile the spinning read may be hoisted out of the loop and never
   // observe the write -- Thread.sleep() is not a synchronisation point.
   public static volatile boolean resetConnection = false;
   public static volatile int current_consumer = 0;
   /**
    * A request to hold the consumer still, set by /co purge and by the schema
    * patcher. It is NOT a mutex: Process no longer sets or clears it, because a
    * consumer cycle that started while a purge held the flag used to clear it on
    * the way out, releasing a caller that thought it still had exclusive access.
    */
   public static volatile boolean is_paused = false;
   private static volatile boolean running = false;
   protected static volatile boolean pause_success = false;
   /**
    * True while Process is writing a batch out. The schema patcher waits on this
    * to know the backlog it built up has actually reached the database before it
    * announces the upgrade finished; that used to be read off is_paused, which
    * conflated "a flush is in progress" with "someone asked me to stop".
    */
   public static volatile boolean flushing = false;
   /**
    * Guards the active buffer index together with the contents of every map
    * below. Held by Queue.enqueue for one append and by the buffer flip in
    * run(); once the flip has happened under this lock no other thread can still
    * be writing to the buffer being handed to Process.
    */
   public static final Object QUEUE_LOCK = new Object();
   static final Map<Integer, ArrayList<Object[]>> consumer = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Integer> consumer_id = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, String[]>> consumer_users = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, String>> consumer_strings = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, Object>> consumer_object = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, String[]>> consumer_signs = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, ItemStack[]>> consumer_containers = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, Object>> consumer_inventories = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, List<BlockState>>> consumer_block_list = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, List<Object[]>>> consumer_object_array_list = Collections.synchronizedMap(new HashMap<>());
   static final Map<Integer, Map<Integer, List<Object>>> consumer_object_list = Collections.synchronizedMap(new HashMap<>());

   private static void errorDelay() {
      try {
         Thread.sleep(30000L);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static void initialize() {
      consumer.put(0, new ArrayList<>());
      consumer.put(1, new ArrayList<>());
      consumer_users.put(0, new HashMap<>());
      consumer_users.put(1, new HashMap<>());
      consumer_strings.put(0, new HashMap<>());
      consumer_strings.put(1, new HashMap<>());
      consumer_object.put(0, new HashMap<>());
      consumer_object.put(1, new HashMap<>());
      consumer_signs.put(0, new HashMap<>());
      consumer_signs.put(1, new HashMap<>());
      consumer_inventories.put(0, new HashMap<>());
      consumer_inventories.put(1, new HashMap<>());
      consumer_block_list.put(0, new HashMap<>());
      consumer_block_list.put(1, new HashMap<>());
      consumer_object_array_list.put(0, new HashMap<>());
      consumer_object_array_list.put(1, new HashMap<>());
      consumer_object_list.put(0, new HashMap<>());
      consumer_object_list.put(1, new HashMap<>());
      consumer_containers.put(0, new HashMap<>());
      consumer_containers.put(1, new HashMap<>());
      consumer_id.put(0, 0);
      consumer_id.put(1, 0);
   }

   public static boolean isRunning() {
      return running;
   }

   private static void pauseConsumer() {
      try {
         while (Config.server_running && (is_paused || Config.purge_running)) {
            pause_success = true;
            resetConnection = true;
            Thread.sleep(100L);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      pause_success = false;
   }

   public void run() {
      running = true;
      is_paused = false;

      while (Config.server_running || Config.converter_running) {
         try {
            int process_id;
            // The flip takes the same lock as Queue.enqueue, so once it returns
            // every append that read the old index has already finished and the
            // buffer handed to Process is nobody else's to touch.
            synchronized (QUEUE_LOCK) {
               process_id = current_consumer;
               current_consumer = current_consumer == 0 ? 1 : 0;
               // Only restart ids from zero when the buffer really was drained.
               // A cycle that failed to clear leaves rows behind, and resetting
               // over them makes two live rows share an id.
               if (consumer.get(current_consumer).isEmpty()) {
                  consumer_id.put(current_consumer, 0);
               }
            }

            Thread.sleep(500L);
            pauseConsumer();
            Process.processConsumer(process_id);
         } catch (Exception e) {
            e.printStackTrace();
            errorDelay();
         }
      }

      running = false;
   }

   public void uncaughtException(Thread thread, Throwable e) {
      running = false;
      // The buffer used to be cleared here, which threw away up to a full cycle
      // of records that had never been written. Leave it: the restarted consumer
      // picks it up on its next pass.
      e.printStackTrace();
      startConsumer();
   }

   public static void startConsumer() {
      if (!running) {
         Thread consumerThread = new Thread(new Consumer());
         consumerThread.setUncaughtExceptionHandler(new Consumer());
         consumerThread.start();
      }

   }
}
