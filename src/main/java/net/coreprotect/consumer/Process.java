package net.coreprotect.consumer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.coreprotect.Functions;
import net.coreprotect.database.Database;
import net.coreprotect.database.Logger;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class Process {
   private static Connection connection = null;
   private static int lastConnection = 0;

   private static void validateConnection() {
      validateConnection(false);
   }

   /**
    * `shutdown` marks the final flush from CoreProtect.safeShutdown, which runs
    * after Config.server_running has already been set to false. Without it this
    * method drops the connection and then declines to open another, so that
    * flush found connection == null and returned having written nothing -- every
    * row still buffered at restart was discarded, the queued co_world and
    * co_material_map rows among them.
    */
   private static void validateConnection(boolean shutdown) {
      try {
         if (connection != null) {
            int timeSinceLastConnection = (int)(System.currentTimeMillis() / 1000L) - lastConnection;
            if (timeSinceLastConnection > 900 || connection.isClosed() || (!Config.server_running && !shutdown) || Consumer.resetConnection) {
               connection.close();
               connection = null;
               Consumer.resetConnection = false;
            }
         }

         if (connection == null && (Config.server_running || shutdown)) {
            connection = Database.getConnection(false);
            lastConnection = (int)(System.currentTimeMillis() / 1000L);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   /**
    * Releases the connection the shutdown flushes were deliberately holding open
    * across their calls to validateConnection(true).
    */
   public static void closeConnection() {
      try {
         if (connection != null) {
            connection.close();
            connection = null;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   /**
    * Writes one consumer buffer out to the database. The buffer index is handed
    * over by Consumer.run() after it has flipped under QUEUE_LOCK, so nothing
    * else is writing to it by the time this runs.
    */
   public static void processConsumer(int process_id) {
      processConsumer(process_id, false);
   }

   /** As above; `shutdown` keeps the connection available past server_running. */
   public static void processConsumer(int process_id, boolean shutdown) {
      try {
         validateConnection(shutdown);
         if (connection == null) {
            return;
         }

         // This used to raise Consumer.is_paused, which is a purge/patch pause
         // request rather than a lock -- so a cycle starting while a purge held
         // it cleared it out from under them on the way out. Reader/writer
         // overlap is handled by the SQLite pragmas in Database.getConnection
         // now; `flushing` only reports progress to the schema patcher.
         Consumer.flushing = true;

         ArrayList<Object[]> consumer_data = Consumer.consumer.get(process_id);
         Map<Integer, String[]> users = Consumer.consumer_users.get(process_id);
         Map<Integer, Object> blocks = Consumer.consumer_object.get(process_id);

         Statement statement = connection.createStatement();

         try {
            resolveUserIds(statement, users);

            PreparedStatement[] batch = openBatchStatements();
            PreparedStatement signs = batch[0];
            PreparedStatement blockRows = batch[1];
            PreparedStatement skulls = batch[2];
            PreparedStatement containers = batch[3];
            PreparedStatement worlds = batch[4];
            PreparedStatement chat = batch[5];
            PreparedStatement command = batch[6];
            PreparedStatement session = batch[7];
            PreparedStatement entities = batch[8];
            PreparedStatement materials = batch[9];
            PreparedStatement art = batch[10];
            PreparedStatement entityMap = batch[11];

            try {
               Database.beginTransaction(statement);

               // The drain is wrapped so the buffer is always emptied, even if a
               // malformed row throws outside the per-row handler below. Leaving
               // it behind made the batch grow on every following cycle, and its
               // user/object entries have already been consumed either way.
               try {
                  for (Object[] data : consumer_data) {
                     if (data == null) {
                        continue;
                     }

                     int id = (Integer) data[0];
                     int action = (Integer) data[1];
                     Material block_type = (Material) data[2];
                     int block_data = (Integer) data[3];
                     Material replace_type = (Material) data[4];
                     int replace_data = (Integer) data[5];
                     int force_data = (Integer) data[6];

                     String[] user_data = users.get(id);
                     Object object = blocks.get(id);
                     if (user_data == null || object == null) {
                        continue;
                     }

                     String user = user_data[0];

                     try {
                        switch (action) {
                           case 0:
                              processBlockBreak(blockRows, skulls, process_id, id, block_type, block_data, replace_type, force_data, user, object);
                              break;
                           case 1:
                              processBlockPlace(blockRows, skulls, block_type, block_data, replace_type, replace_data, force_data, user, object);
                              break;
                           case 2:
                              processSignText(signs, process_id, id, force_data, user, object);
                              break;
                           case 3:
                              processContainerBreak(containers, process_id, id, user, object);
                              break;
                           case 4:
                              processPlayerInteraction(blockRows, user, object);
                              break;
                           case 5:
                              processContainerTransaction(containers, process_id, id, force_data, user, object);
                              break;
                           case 6:
                              processStructureGrowth(statement, blockRows, process_id, id, user, object);
                              break;
                           case 7:
                              processRollbackUpdate(statement, process_id, id, force_data, 0);
                              break;
                           case 8:
                              processRollbackUpdate(statement, process_id, id, force_data, 1);
                              break;
                           case 9:
                              processWorldInsert(worlds, user, force_data);
                              break;
                           case 10:
                              processSignUpdate(statement, object, block_data, force_data);
                              break;
                           case 11:
                              processSkullUpdate(statement, object, force_data);
                              break;
                           case 12:
                              processPlayerChat(chat, process_id, id, force_data, user);
                              break;
                           case 13:
                              processPlayerCommand(command, process_id, id, force_data, user);
                              break;
                           case 14:
                              processPlayerLogin(connection, session, process_id, id, object, block_data, replace_data, force_data, user);
                              break;
                           case 15:
                              processPlayerLogout(session, object, force_data, user);
                              break;
                           case 16:
                              processEntityKill(blockRows, entities, process_id, id, object, user);
                              break;
                           case 17:
                              processEntitySpawn(statement, object, force_data);
                              break;
                           case 18:
                              processHangingRemove(object, force_data);
                              break;
                           case 19:
                              processHangingSpawn(object, block_type, block_data, force_data);
                              break;
                           case 20:
                              processNaturalBlockBreak(statement, blockRows, process_id, id, user, object, block_type, block_data);
                              break;
                           case 21:
                              processMaterialInsert(materials, user, force_data);
                              break;
                           case 22:
                              processMaterialInsert(art, user, force_data);
                              break;
                           case 23:
                              processMaterialInsert(entityMap, user, force_data);
                              break;
                           case 24:
                              processPlayerKill(blockRows, id, object, user);
                              break;
                           default:
                              break;
                        }
                     } catch (Exception e) {
                        e.printStackTrace();
                     }

                     users.remove(id);
                     blocks.remove(id);
                  }
               } finally {
                  Database.commitTransaction(statement);
                  consumer_data.clear();
               }
            } finally {
               closeQuietly(batch);
            }
         } finally {
            closeQuietly(statement);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         Consumer.flushing = false;
         validateConnection(shutdown);
      }
   }

   /**
    * Makes sure every user named in this batch has a row in co_user, so the
    * inserts below can resolve them from the cache without hitting the database.
    */
   private static void resolveUserIds(Statement statement, Map<Integer, String[]> users) {
      Database.beginTransaction(statement);

      for (String[] user_data : users.values()) {
         String user = user_data[0];
         if (Config.player_id_cache.get(user.toLowerCase()) == null) {
            Database.loadUserID(connection, user, user_data[1]);
         }
      }

      Database.commitTransaction(statement);
   }

   /** One prepared statement per table this batch can write to. */
   private static PreparedStatement[] openBatchStatements() {
      PreparedStatement[] batch = new PreparedStatement[12];

      for (int type = 0; type < batch.length; ++type) {
         // Skulls and entities are read back for their generated key.
         boolean keys = type == 2 || type == 8;
         batch[type] = Database.prepareStatement(connection, type, keys);
      }

      return batch;
   }

   /**
    * Closes every statement even if one of them throws. They used to be closed
    * in a straight line, so a failure part way through leaked the rest.
    */
   private static void closeQuietly(AutoCloseable... closeables) {
      for (AutoCloseable closeable : closeables) {
         if (closeable != null) {
            try {
               closeable.close();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   private static void processBlockBreak(PreparedStatement preparedStmt, PreparedStatement preparedStmt_skulls, int process_id, int id, Material block_type, int block_data, Material replace_type, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         List<Object> meta = Functions.processMeta(block);
         if (block instanceof Skull) {
            Logger.log_skull_break(preparedStmt, preparedStmt_skulls, user, block);
         } else {
            Logger.log_break(preparedStmt, user, block.getLocation(), Functions.block_id(block_type), block_data, meta);
            if (force_data == 5 && (block_type.equals(Material.WOODEN_DOOR) || block_type.equals(Material.SPRUCE_DOOR) || block_type.equals(Material.BIRCH_DOOR) || block_type.equals(Material.JUNGLE_DOOR) || block_type.equals(Material.ACACIA_DOOR) || block_type.equals(Material.DARK_OAK_DOOR) || block_type.equals(Material.IRON_DOOR_BLOCK)) && !replace_type.equals(Material.WOODEN_DOOR) && !replace_type.equals(Material.SPRUCE_DOOR) && !replace_type.equals(Material.BIRCH_DOOR) && !replace_type.equals(Material.JUNGLE_DOOR) && !replace_type.equals(Material.ACACIA_DOOR) && !replace_type.equals(Material.DARK_OAK_DOOR) && !replace_type.equals(Material.IRON_DOOR_BLOCK)) {
               int d = block_data;
               if (block_data < 9) {
                  d = block_data + 8;
               }

               Location location = block.getLocation();
               location.setY(location.getY() + 1.0D);
               Logger.log_break(preparedStmt, user, location, Functions.block_id(block_type), d, null);
            }
         }
      }

   }

   private static void processBlockPlace(PreparedStatement preparedStmt, PreparedStatement preparedStmt_skulls, Material block_type, int block_data, Material replace_type, int replace_data, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         List<Object> meta = Functions.processMeta(block);
         if (block_type.equals(Material.SKULL)) {
            Logger.log_skull_place(preparedStmt, preparedStmt_skulls, user, block, Functions.block_id(replace_type), replace_data);
         } else Logger.log_place(preparedStmt, user, block, Functions.block_id(replace_type), replace_data, block_type, block_data, force_data == 1, meta);
      }

   }

   private static void processContainerBreak(PreparedStatement preparedStmt, int process_id, int id, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, ItemStack[]> containers = Consumer.consumer_containers.get(process_id);
         if (containers.get(id) != null) {
            ItemStack[] container = (ItemStack[])containers.get(id);
            Logger.log_container_break(preparedStmt, user, block.getLocation(), block.getType(), container);
            containers.remove(id);
         }
      }

   }

   private static void processContainerTransaction(PreparedStatement preparedStmt, int process_id, int id, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, Object> inventories = Consumer.consumer_inventories.get(process_id);
         if (inventories.get(id) != null) {
            Object inventory = inventories.get(id);
            String logging_chest_id = user.toLowerCase() + "." + block.getX() + "." + block.getY() + "." + block.getZ();
            if (Config.logging_chest.get(logging_chest_id) != null) {
               int current_chest = Config.logging_chest.get(logging_chest_id);
               if (Config.old_container.get(logging_chest_id) == null) {
                  return;
               }

               int force_size = 0;
               if (Config.force_containers.get(logging_chest_id) != null) {
                  force_size = Config.force_containers.get(logging_chest_id).size();
               }

               if (current_chest == force_data || force_size > 0) {
                  Logger.log_container(preparedStmt, user, block.getType(), inventory, block.getLocation());
                  List<ItemStack[]> old = Config.old_container.get(logging_chest_id);
                  if (old.isEmpty()) {
                     Config.old_container.remove(logging_chest_id);
                     Config.logging_chest.remove(logging_chest_id);
                  }
               }
            }

            inventories.remove(id);
         }
      }

   }

   private static void processEntityKill(PreparedStatement preparedStmt, PreparedStatement preparedStmt_entities, int process_id, int id, Object object, String user) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         EntityType type = (EntityType)((Object[])object)[1];
         Map<Integer, List<Object>> object_lists = Consumer.consumer_object_list.get(process_id);
         if (object_lists.get(id) != null) {
            List<Object> object_list = object_lists.get(id);
            int entityId = Functions.getEntityId(type);
            Logger.log_entity_kill(preparedStmt, preparedStmt_entities, user, block, object_list, entityId);
            object_lists.remove(id);
         }
      }

   }

   private static void processEntitySpawn(Statement statement, Object object, int row_id) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         EntityType type = (EntityType)((Object[])object)[1];
         String query = "SELECT data FROM " + Config.prefix + "entity WHERE rowid='" + row_id + "' LIMIT 0, 1";
         List<Object> data = Database.getEntityData(statement, block, query);
         Functions.spawnEntity(block, type, data);
      }

   }

   private static void processHangingRemove(Object object, int delay) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Functions.removeHanging(block, delay);
      }

   }

   private static void processHangingSpawn(Object object, Material type, int data, int delay) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Functions.spawnHanging(block, type, data, delay);
      }

   }

   private static void processMaterialInsert(PreparedStatement preparedStmt, String name, int material_id) {
      Database.insertMaterial(preparedStmt, material_id, name);
   }

   private static void processNaturalBlockBreak(Statement statement, PreparedStatement preparedStmt, int process_id, int id, String user, Object object, Material block_type, int block_data) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, List<BlockState>> block_lists = Consumer.consumer_block_list.get(process_id);
         if (block_lists.get(id) != null) {
            for (BlockState list_block : block_lists.get(id)) {
               String removed = Lookup.who_removed_cache(list_block);
               if (!removed.isEmpty()) {
                  user = removed;
               }
            }

            block_lists.remove(id);
            Logger.log_break(preparedStmt, user, block.getLocation(), Functions.block_id(block_type), block_data, null);
         }
      }

   }

   private static void processPlayerChat(PreparedStatement preparedStmt, int process_id, int id, int time, String user) {
      Map<Integer, String> strings = Consumer.consumer_strings.get(process_id);
      if (strings.get(id) != null) {
         String message = (String)strings.get(id);
         Logger.log_chat(preparedStmt, time, user, message);
         strings.remove(id);
      }

   }

   private static void processPlayerCommand(PreparedStatement preparedStmt, int process_id, int id, int time, String user) {
      Map<Integer, String> strings = Consumer.consumer_strings.get(process_id);
      if (strings.get(id) != null) {
         String message = (String)strings.get(id);
         Logger.log_command(preparedStmt, time, user, message);
         strings.remove(id);
      }

   }

   private static void processPlayerInteraction(PreparedStatement preparedStmt, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Logger.log_interact(preparedStmt, user, block);
      }

   }

   private static void processPlayerKill(PreparedStatement preparedStmt, int id, Object object, String user) {
      if (object instanceof Object[]) {
         BlockState block = (BlockState)((Object[])object)[0];
         String player = (String)((Object[])object)[1];
         Logger.log_player_kill(preparedStmt, user, block, player);
      }

   }

   private static void processPlayerLogin(Connection connection, PreparedStatement preparedStmt, int process_id, int id, Object object, int configSessions, int configUsernames, int time, String user) {
      if (object instanceof BlockState) {
         Map<Integer, String> strings = Consumer.consumer_strings.get(process_id);
         if (strings.get(id) != null) {
            String uuid = (String)strings.get(id);
            BlockState block = (BlockState)object;
            Logger.log_username(connection, user, uuid, configUsernames, time);
            if (configSessions == 1) {
               Logger.log_session(preparedStmt, user, block, time, 1);
            }

            strings.remove(id);
         }
      }

   }

   private static void processPlayerLogout(PreparedStatement preparedStmt, Object object, int time, String user) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Logger.log_session(preparedStmt, user, block, time, 0);
      }

   }

   private static void processRollbackUpdate(Statement statement, int process_id, int id, int action, int table) {
      Map<Integer, List<Object[]>> update_lists = Consumer.consumer_object_array_list.get(process_id);
      if (update_lists.get(id) != null) {
         for (Object[] list_row : update_lists.get(id)) {
            int rowid = (Integer) list_row[0];
            int rolled_back = (Integer) list_row[9];
            if (rolled_back == action) {
               Database.performUpdate(statement, rowid, action, table);
            }
         }

         update_lists.remove(id);
      }

   }

   private static void processSignText(PreparedStatement preparedStmt, int process_id, int id, int force_data, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, String[]> signs = Consumer.consumer_signs.get(process_id);
         if (signs.get(id) != null) {
            String[] sign_text = (String[])signs.get(id);
            Logger.sign_text(preparedStmt, user, block, sign_text[0], sign_text[1], sign_text[2], sign_text[3], force_data);
            signs.remove(id);
         }
      }

   }

   private static void processSignUpdate(Statement statement, Object object, int action, int time) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         int x = block.getX();
         int y = block.getY();
         int z = block.getZ();
         int wid = Functions.getWorldId(block.getWorld().getName());
         String query = "";
         if (action == 0) {
            query = "SELECT line_1, line_2, line_3, line_4 FROM " + Config.prefix + "sign WHERE wid='" + wid + "' AND x='" + x + "' AND z='" + z + "' AND y='" + y + "' AND time < '" + time + "' ORDER BY rowid DESC LIMIT 0, 1";
         } else {
            query = "SELECT line_1, line_2, line_3, line_4 FROM " + Config.prefix + "sign WHERE wid='" + wid + "' AND x='" + x + "' AND z='" + z + "' AND y='" + y + "' AND time >= '" + time + "' ORDER BY rowid ASC LIMIT 0, 1";
         }

         Database.getSignData(statement, block, query);
         Functions.updateBlock(block);
      }

   }

   private static void processSkullUpdate(Statement statement, Object object, int row_id) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         String query = "SELECT type,data,rotation,owner FROM " + Config.prefix + "skull WHERE rowid='" + row_id + "' LIMIT 0, 1";
         Database.getSkullData(statement, block, query);
         Functions.updateBlock(block);
      }

   }

   private static void processStructureGrowth(Statement statement, PreparedStatement preparedStmt, int process_id, int id, String user, Object object) {
      if (object instanceof BlockState) {
         BlockState block = (BlockState)object;
         Map<Integer, List<BlockState>> block_lists = Consumer.consumer_block_list.get(process_id);
         if (block_lists.get(id) != null) {
            List<BlockState> block_list = block_lists.get(id);
            String result_data = Lookup.who_placed(statement, block);
            if (!result_data.isEmpty()) {
               user = result_data;
            }

            for (BlockState list_block : block_list) {
               if (list_block.getY() >= block.getY()) {
                  Logger.log_place(preparedStmt, user, list_block, 0, 0, null, -1, false, null);
               }
            }

            block_lists.remove(id);
         }
      }

   }

   private static void processWorldInsert(PreparedStatement preparedStmt, String world, int world_id) {
      Database.insertWorld(preparedStmt, world_id, world);
   }
}
