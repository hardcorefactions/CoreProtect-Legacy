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
   public static boolean resetConnection = false;
   public static int current_consumer = 0;
   public static boolean is_paused = false;
   private static boolean running = false;
   protected static boolean pause_success = false;
   static Map<Integer, ArrayList<Object[]>> consumer = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Integer> consumer_id = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, String[]>> consumer_users = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, String>> consumer_strings = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, Object>> consumer_object = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, String[]>> consumer_signs = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, ItemStack[]>> consumer_containers = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, Object>> consumer_inventories = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, List<BlockState>>> consumer_block_list = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, List<Object[]>>> consumer_object_array_list = Collections.synchronizedMap(new HashMap());
   static Map<Integer, Map<Integer, List<Object>>> consumer_object_list = Collections.synchronizedMap(new HashMap());

   private static void errorDelay() {
      try {
         Thread.sleep(30000L);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public static int getConsumerId() {
      return (Integer)consumer_id.get(current_consumer);
   }

   public static void initialize() {
      consumer.put(0, new ArrayList());
      consumer.put(1, new ArrayList());
      consumer_users.put(0, new HashMap());
      consumer_users.put(1, new HashMap());
      consumer_strings.put(0, new HashMap());
      consumer_strings.put(1, new HashMap());
      consumer_object.put(0, new HashMap());
      consumer_object.put(1, new HashMap());
      consumer_signs.put(0, new HashMap());
      consumer_signs.put(1, new HashMap());
      consumer_inventories.put(0, new HashMap());
      consumer_inventories.put(1, new HashMap());
      consumer_block_list.put(0, new HashMap());
      consumer_block_list.put(1, new HashMap());
      consumer_object_array_list.put(0, new HashMap());
      consumer_object_array_list.put(1, new HashMap());
      consumer_object_list.put(0, new HashMap());
      consumer_object_list.put(1, new HashMap());
      consumer_containers.put(0, new HashMap());
      consumer_containers.put(1, new HashMap());
      consumer_id.put(0, 0);
      consumer_id.put(1, 0);
   }

   public static boolean isRunning() {
      return running;
   }

   private static void pauseConsumer() {
      try {
         while(Config.server_running && (is_paused || Config.purge_running)) {
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

      while(Config.server_running || Config.converter_running) {
         try {
            int process_id = 0;
            if (current_consumer == 0) {
               current_consumer = 1;
               consumer_id.put(current_consumer, 0);
            } else {
               process_id = 1;
               current_consumer = 0;
               consumer_id.put(current_consumer, 0);
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
      ((ArrayList)consumer.get(current_consumer == 1 ? 0 : 1)).clear();
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
