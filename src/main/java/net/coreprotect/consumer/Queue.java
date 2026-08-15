package net.coreprotect.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.coreprotect.CoreProtect;
import net.coreprotect.Functions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Queue {
   /**
    * Appends one record to the active consumer buffer.
    *
    * Every queue method used to read Consumer.current_consumer three separate
    * times -- once for the id, once for the row, once for the user/object maps --
    * while appending to a bare ArrayList. Two problems came out of that:
    *
    * - The consumer thread flips current_consumer roughly twice a second. A flip
    *   landing between the row append and the user/object puts left the row in
    *   one buffer and its data in the other, and Process silently discards a row
    *   whose user or object is missing.
    * - The main thread, the async chat thread (AsyncPlayerChatEvent) and rollback
    *   threads all append concurrently. Unsynchronised ArrayList.add loses
    *   elements outright, and can leave null holes -- which is why Process still
    *   carries a `data != null` guard.
    *
    * Both are closed by reading the buffer index exactly once and doing the whole
    * append under a lock the buffer flip in Consumer.run() also takes.
    *
    * The lock is held only for the map writes; callers do their Bukkit work
    * before calling in.
    */
   @SuppressWarnings("unchecked")
   private static void enqueue(Object[] row, String[] user, Object object, Map<Integer, ? extends Map<Integer, ?>> payload_map, Object payload) {
      synchronized(Consumer.QUEUE_LOCK) {
         int buffer = Consumer.current_consumer;
         int consumer_id = Consumer.consumer_id.get(buffer);
         row[0] = consumer_id;
         Consumer.consumer.get(buffer).add(row);
         if (payload_map != null) {
            ((Map<Integer, Object>)payload_map.get(buffer)).put(consumer_id, payload);
         }

         Consumer.consumer_users.get(buffer).put(consumer_id, user);
         Consumer.consumer_object.get(buffer).put(consumer_id, object);
         Consumer.consumer_id.put(buffer, consumer_id + 1);
      }
   }

   private static void enqueue(Object[] row, String[] user, Object object) {
      enqueue(row, user, object, null, null);
   }

   protected static void queueAdvancedBreak(String user, BlockState block, Material type, int data, Material break_type, int block_number) {
      enqueue(new Object[]{0, 0, type, data, break_type, 0, block_number}, new String[]{user, null}, block);
   }

   protected static void queueArtInsert(int id, String name) {
      Location location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      enqueue(new Object[]{0, 22, null, 0, null, 0, id}, new String[]{name, null}, location);
   }

   public static void queueBlockBreak(String user, BlockState block, Material type, int data) {
      queueBlockBreak(user, block, type, data, (Material)null, 0);
   }

   protected static void queueBlockBreak(String user, BlockState block, Material type, int data, Material break_type, int block_number) {
      if (type.equals(Material.MOB_SPAWNER)) {
         CreatureSpawner mobSpawner = (CreatureSpawner)block;
         data = Functions.getSpawnerType(mobSpawner.getSpawnedType());
      } else if (type.equals(Material.DOUBLE_PLANT) && data >= 8 && !user.startsWith("#")) {
         if (block_number == 5) {
            return;
         }

         block = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ()).getState();
         data = Functions.getData(block);
      }

      enqueue(new Object[]{0, 0, type, data, break_type, 0, block_number}, new String[]{user, null}, block);
   }

   protected static void queueBlockPlace(Player player, BlockState final_placed, Block placed, BlockState replaced, Material force_t, int force_d) {
      queueBlockPlace(player.getName(), final_placed, placed, replaced, force_t, force_d, 0);
   }

   protected static void queueBlockPlace(String player, Block placed) {
      queueBlockPlace(player, placed.getState(), placed, (BlockState)null, (Material)null, -1, 0);
   }

   protected static void queueBlockPlace(String player, Block placed, BlockState replaced, int force) {
      queueBlockPlace(player, placed.getState(), placed, replaced, (Material)null, -1, force);
   }

   protected static void queueBlockPlace(String user, Block placed, BlockState replaced, Material type, int data) {
      queueBlockPlace(user, placed.getState(), placed, replaced, type, data, 1);
   }

   public static void queueBlockPlace(String user, BlockState block_location, Block block_type, BlockState block_replaced, Material force_t, int force_d, int force_data) {
      Material type = block_type.getType();
      int data = Functions.getData(block_type);
      Material replace_type = null;
      int replace_data = 0;
      if (type.equals(Material.MOB_SPAWNER)) {
         CreatureSpawner mobSpawner = (CreatureSpawner)block_location;
         data = Functions.getSpawnerType(mobSpawner.getSpawnedType());
         force_data = 1;
      }

      if (block_replaced != null) {
         replace_type = block_replaced.getType();
         replace_data = Functions.getData(block_replaced);
         if (replace_type.equals(Material.DOUBLE_PLANT) && replace_data >= 8) {
            BlockState block_below = block_replaced.getWorld().getBlockAt(block_replaced.getX(), block_replaced.getY() - 1, block_replaced.getZ()).getState();
            Material below_type = block_below.getType();
            int below_data = Functions.getData(block_below);
            queueBlockBreak(user, block_below, below_type, below_data);
         }
      }

      if (force_t != null) {
         type = force_t;
         force_data = 1;
      }

      if (force_d != -1) {
         data = force_d;
         force_data = 1;
      }

      enqueue(new Object[]{0, 1, type, data, replace_type, replace_data, force_data}, new String[]{user, null}, block_location);
   }

   protected static void queueBlockPlace(String user, BlockState placed, BlockState replaced, Material force_type) {
      queueBlockPlace(user, placed, placed.getBlock(), replaced, force_type, -1, 0);
   }

   protected static void queueBlockPlace(String player, BlockState placed, BlockState replaced, Material type, int data) {
      queueBlockPlace(player, placed, placed.getBlock(), replaced, type, data, 1);
   }

   protected static void queueBlockPlace(String player, BlockState placed, Material type, int data) {
      queueBlockPlace(player, placed, placed.getBlock(), (BlockState)null, type, data, 1);
   }

   protected static void queueBlockPlaceDelayed(final String user, final Block placed, final BlockState replaced, int ticks) {
      CoreProtect.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(CoreProtect.getInstance(), () -> {
         try {
            Queue.queueBlockPlace(user, placed.getState(), placed, replaced, (Material)null, -1, 0);
         } catch (Exception e) {
            e.printStackTrace();
         }

      }, (long)ticks);
   }

   protected static void queueContainerBreak(String user, BlockState block, Material type, ItemStack[] old_inventory) {
      // BlockState.setType only edits this snapshot, so it stays outside the lock.
      block.setType(type);
      enqueue(new Object[]{0, 3, null, 0, null, 0, 0}, new String[]{user, null}, block, Consumer.consumer_containers, old_inventory);
   }

   protected static void queueContainerRollbackUpdate(String user, Location location, List<Object[]> list, int action) {
      if (location == null) {
         location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      }

      enqueue(new Object[]{0, 8, null, 0, null, 0, action}, new String[]{user, null}, location, Consumer.consumer_object_array_list, list);
   }

   protected static void queueContainerTransaction(String user, BlockState block, Material type, Object inventory, int chest_id) {
      block.setType(type);
      enqueue(new Object[]{0, 5, null, 0, null, 0, chest_id}, new String[]{user, null}, block, Consumer.consumer_inventories, inventory);
   }

   protected static void queueEntityInsert(int id, String name) {
      Location location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      enqueue(new Object[]{0, 23, null, 0, null, 0, id}, new String[]{name, null}, location);
   }

   protected static void queueEntityKill(String user, Location location, List<Object> data, EntityType type) {
      enqueue(new Object[]{0, 16, null, 0, null, 0, 0}, new String[]{user, null}, new Object[]{location.getBlock().getState(), type}, Consumer.consumer_object_list, data);
   }

   protected static void queueEntitySpawn(String user, BlockState block, EntityType type, int data) {
      enqueue(new Object[]{0, 17, null, 0, null, 0, data}, new String[]{user, null}, new Object[]{block, type});
   }

   protected static void queueHangingRemove(String user, BlockState block, int delay) {
      enqueue(new Object[]{0, 18, null, 0, null, 0, delay}, new String[]{user, null}, block);
   }

   protected static void queueHangingSpawn(String user, BlockState block, Material type, int data, int delay) {
      enqueue(new Object[]{0, 19, type, data, null, 0, delay}, new String[]{user, null}, block);
   }

   protected static void queueMaterialInsert(int id, String name) {
      Location location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      enqueue(new Object[]{0, 21, null, 0, null, 0, id}, new String[]{name, null}, location);
   }

   protected static void queueNaturalBlockBreak(String user, BlockState block, Block relative, Material type, int data) {
      List<BlockState> relative_list = new ArrayList<>();
      if (relative != null) {
         relative_list.add(relative.getState());
      }

      enqueue(new Object[]{0, 20, type, data, null, 0, 0}, new String[]{user, null}, block, Consumer.consumer_block_list, relative_list);
   }

   protected static void queuePlayerChat(Player player, String message, int time) {
      // Called from the async chat thread; the Bukkit reads happen before the lock.
      String name = player.getName();
      Location location = player.getLocation();
      enqueue(new Object[]{0, 12, null, 0, null, 0, time}, new String[]{name, null}, location, Consumer.consumer_strings, message);
   }

   protected static void queuePlayerCommand(Player player, String message, int time) {
      String name = player.getName();
      BlockState block = player.getLocation().getBlock().getState();
      enqueue(new Object[]{0, 13, null, 0, null, 0, time}, new String[]{name, null}, block, Consumer.consumer_strings, message);
   }

   protected static void queuePlayerInteraction(String user, BlockState block) {
      enqueue(new Object[]{0, 4, null, 0, null, 0, 0}, new String[]{user, null}, block);
   }

   protected static void queuePlayerKill(String user, Location location, String player) {
      enqueue(new Object[]{0, 24, null, 0, null, 0, 0}, new String[]{user, null}, new Object[]{location.getBlock().getState(), player});
   }

   protected static void queuePlayerLogin(Player player, int time, int configSessions, int configUsernames) {
      String uuid = player.getUniqueId().toString();
      BlockState block = player.getLocation().getBlock().getState();
      enqueue(new Object[]{0, 14, null, configSessions, null, configUsernames, time}, new String[]{player.getName(), uuid}, block, Consumer.consumer_strings, uuid);
   }

   protected static void queuePlayerQuit(Player player, int time) {
      BlockState block = player.getLocation().getBlock().getState();
      enqueue(new Object[]{0, 15, null, 0, null, 0, time}, new String[]{player.getName(), null}, block);
   }

   protected static void queueRollbackUpdate(String user, Location location, List<Object[]> list, int action) {
      if (location == null) {
         location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      }

      enqueue(new Object[]{0, 7, null, 0, null, 0, action}, new String[]{user, null}, location, Consumer.consumer_object_array_list, list);
   }

   protected static void queueSignText(String user, BlockState block, String line1, String line2, String line3, String line4, int offset) {
      enqueue(new Object[]{0, 2, null, 0, null, 0, offset}, new String[]{user, null}, block, Consumer.consumer_signs, new String[]{line1, line2, line3, line4});
   }

   protected static void queueSignUpdate(String user, BlockState block, int action, int time) {
      enqueue(new Object[]{0, 10, null, action, null, 0, time}, new String[]{user, null}, block);
   }

   protected static void queueSkullUpdate(String user, BlockState block, int row_id) {
      enqueue(new Object[]{0, 11, null, 0, null, 0, row_id}, new String[]{user, null}, block);
   }

   protected static void queueStructureGrow(String user, BlockState block, List<BlockState> block_list) {
      enqueue(new Object[]{0, 6, null, 0, null, 0, 0}, new String[]{user, null}, block, Consumer.consumer_block_list, block_list);
   }

   protected static void queueWorldInsert(int id, String world) {
      Location location = new Location(CoreProtect.getInstance().getServer().getWorlds().get(0), 0.0D, 0.0D, 0.0D);
      enqueue(new Object[]{0, 9, null, 0, null, 0, id}, new String[]{world, null}, location);
   }
}
