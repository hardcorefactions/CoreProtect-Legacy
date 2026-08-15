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
   protected static void queueAdvancedBreak(String user, BlockState block, Material type, int data, Material break_type, int block_number) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 0, type, data, break_type, 0, block_number});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueArtInsert(int id, String name) {
      Location location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 22, null, 0, null, 0, id});
      queueStandardData(consumer_id, new String[]{name, null}, location);
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

      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 0, type, data, break_type, 0, block_number});
      queueStandardData(consumer_id, new String[]{user, null}, block);
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

      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 1, type, data, replace_type, replace_data, force_data});
      queueStandardData(consumer_id, new String[]{user, null}, block_location);
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
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 3, null, 0, null, 0, 0});
      ((Map)Consumer.consumer_containers.get(Consumer.current_consumer)).put(consumer_id, old_inventory);
      block.setType(type);
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueContainerRollbackUpdate(String user, Location location, List<Object[]> list, int action) {
      if (location == null) {
         location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      }

      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 8, null, 0, null, 0, action});
      ((Map)Consumer.consumer_object_array_list.get(Consumer.current_consumer)).put(consumer_id, list);
      queueStandardData(consumer_id, new String[]{user, null}, location);
   }

   protected static void queueContainerTransaction(String user, BlockState block, Material type, Object inventory, int chest_id) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 5, null, 0, null, 0, chest_id});
      ((Map)Consumer.consumer_inventories.get(Consumer.current_consumer)).put(consumer_id, inventory);
      block.setType(type);
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueEntityInsert(int id, String name) {
      Location location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 23, null, 0, null, 0, id});
      queueStandardData(consumer_id, new String[]{name, null}, location);
   }

   protected static void queueEntityKill(String user, Location location, List<Object> data, EntityType type) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 16, null, 0, null, 0, 0});
      ((Map)Consumer.consumer_object_list.get(Consumer.current_consumer)).put(consumer_id, data);
      queueStandardData(consumer_id, new String[]{user, null}, new Object[]{location.getBlock().getState(), type});
   }

   protected static void queueEntitySpawn(String user, BlockState block, EntityType type, int data) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 17, null, 0, null, 0, data});
      queueStandardData(consumer_id, new String[]{user, null}, new Object[]{block, type});
   }

   protected static void queueHangingRemove(String user, BlockState block, int delay) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 18, null, 0, null, 0, delay});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueHangingSpawn(String user, BlockState block, Material type, int data, int delay) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 19, type, data, null, 0, delay});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueMaterialInsert(int id, String name) {
      Location location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 21, null, 0, null, 0, id});
      queueStandardData(consumer_id, new String[]{name, null}, location);
   }

   protected static void queueNaturalBlockBreak(String user, BlockState block, Block relative, Material type, int data) {
      List<BlockState> relative_list = new ArrayList<>();
      if (relative != null) {
         relative_list.add(relative.getState());
      }

      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 20, type, data, null, 0, 0});
      ((Map)Consumer.consumer_block_list.get(Consumer.current_consumer)).put(consumer_id, relative_list);
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queuePlayerChat(Player player, String message, int time) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 12, null, 0, null, 0, time});
      ((Map)Consumer.consumer_strings.get(Consumer.current_consumer)).put(consumer_id, message);
      queueStandardData(consumer_id, new String[]{player.getName(), null}, player.getLocation());
   }

   protected static void queuePlayerCommand(Player player, String message, int time) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 13, null, 0, null, 0, time});
      ((Map)Consumer.consumer_strings.get(Consumer.current_consumer)).put(consumer_id, message);
      queueStandardData(consumer_id, new String[]{player.getName(), null}, player.getLocation().getBlock().getState());
   }

   protected static void queuePlayerInteraction(String user, BlockState block) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 4, null, 0, null, 0, 0});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queuePlayerKill(String user, Location location, String player) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 24, null, 0, null, 0, 0});
      queueStandardData(consumer_id, new String[]{user, null}, new Object[]{location.getBlock().getState(), player});
   }

   protected static void queuePlayerLogin(Player player, int time, int configSessions, int configUsernames) {
      int consumer_id = Consumer.getConsumerId();
      String uuid = player.getUniqueId().toString();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 14, null, configSessions, null, configUsernames, time});
      ((Map)Consumer.consumer_strings.get(Consumer.current_consumer)).put(consumer_id, uuid);
      queueStandardData(consumer_id, new String[]{player.getName(), uuid}, player.getLocation().getBlock().getState());
   }

   protected static void queuePlayerQuit(Player player, int time) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 15, null, 0, null, 0, time});
      queueStandardData(consumer_id, new String[]{player.getName(), null}, player.getLocation().getBlock().getState());
   }

   protected static void queueRollbackUpdate(String user, Location location, List<Object[]> list, int action) {
      if (location == null) {
         location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      }

      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 7, null, 0, null, 0, action});
      ((Map)Consumer.consumer_object_array_list.get(Consumer.current_consumer)).put(consumer_id, list);
      queueStandardData(consumer_id, new String[]{user, null}, location);
   }

   protected static void queueSignText(String user, BlockState block, String line1, String line2, String line3, String line4, int offset) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 2, null, 0, null, 0, offset});
      ((Map)Consumer.consumer_signs.get(Consumer.current_consumer)).put(consumer_id, new String[]{line1, line2, line3, line4});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueSignUpdate(String user, BlockState block, int action, int time) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 10, null, action, null, 0, time});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueSkullUpdate(String user, BlockState block, int row_id) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 11, null, 0, null, 0, row_id});
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   private static void queueStandardData(int consumer_id, String[] user, Object object) {
      ((Map)Consumer.consumer_users.get(Consumer.current_consumer)).put(consumer_id, user);
      ((Map)Consumer.consumer_object.get(Consumer.current_consumer)).put(consumer_id, object);
      Consumer.consumer_id.put(Consumer.current_consumer, consumer_id + 1);
   }

   protected static void queueStructureGrow(String user, BlockState block, List<BlockState> block_list) {
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 6, null, 0, null, 0, 0});
      ((Map)Consumer.consumer_block_list.get(Consumer.current_consumer)).put(consumer_id, block_list);
      queueStandardData(consumer_id, new String[]{user, null}, block);
   }

   protected static void queueWorldInsert(int id, String world) {
      Location location = new Location((World)CoreProtect.getInstance().getServer().getWorlds().get(0), (double)0.0F, (double)0.0F, (double)0.0F);
      int consumer_id = Consumer.getConsumerId();
      ((ArrayList)Consumer.consumer.get(Consumer.current_consumer)).add(new Object[]{consumer_id, 9, null, 0, null, 0, id});
      queueStandardData(consumer_id, new String[]{world, null}, location);
   }
}
